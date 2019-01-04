/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.redirectmaps.models;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.query.Query;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirectmaps.impl.FakeSlingHttpServletRequest;
import com.day.cq.commons.jcr.JcrConstants;

/**
 * A Sling Model for serializing a RedirectMap configuration to a consolidated
 * RedirectMap text file
 */
@Model(adaptables = Resource.class)
public class RedirectMapModel {

    private static final Logger log = LoggerFactory.getLogger(RedirectMapModel.class);

    private static final String NO_TARGET_MSG = "No target found in entry %s";
    private static final String SOURCE_WHITESPACE_MSG = "Source path %s for content %s contains whitespace";
    private static final String WHITESPACE_MSG = "Extra whitespace found in entry %s";

    public static final String MAP_FILE_NODE = "redirectMap.txt";

    @Inject
    @Optional
    @Named(MAP_FILE_NODE)
    private Resource redirectMap;

    @Inject
    @Optional
    private List<RedirectConfigModel> redirects;

    @Inject
    @Source("sling-object")
    private ResourceResolver resourceResolver;

    private List<MapEntry> addItems(RedirectConfigModel config, Iterator<Resource> items, String suffix) {
        List<MapEntry> entries = new ArrayList<MapEntry>();
        while (items.hasNext()) {
            Resource item = items.next();
            String path = item.getPath();
            ValueMap properties = item.getChild(JcrConstants.JCR_CONTENT).getValueMap();
            FakeSlingHttpServletRequest mockRequest = new FakeSlingHttpServletRequest(resourceResolver,
                    config.getProtocol(), config.getDomain(), (config.getProtocol().equals("https") ? 443 : 80));
            String pageUrl = config.getProtocol() + "://" + config.getDomain()
                    + resourceResolver.map(mockRequest, item.getPath() + suffix);

            String[] sources = properties.get(config.getProperty(), String[].class);
            for (String source : sources) {
                MapEntry entry = new MapEntry(source, pageUrl, item.getPath());
                if (source.matches(".*\\s+.*")) {
                    String msg = String.format(SOURCE_WHITESPACE_MSG, entry.getSource(), path);
                    log.warn(msg);
                    entry.setStatus(msg);
                    entry.setValid(false);
                }
                entries.add(entry);
            }
        }
        return entries;
    }

    private List<MapEntry> gatherEntries(RedirectConfigModel config) {
        log.trace("gatherEntries");

        log.debug("Getting all of the entries for {}", config.getResource());

        List<MapEntry> entries = new ArrayList<MapEntry>();

        String pageQuery = "SELECT * FROM [cq:Page] WHERE [jcr:content/" + config.getProperty()
                + "] IS NOT NULL AND (ISDESCENDANTNODE([" + config.getPath() + "]) OR [jcr:path]='" + config.getPath()
                + "')";
        log.debug("Finding pages with redirects with query: {}", pageQuery);
        entries.addAll(addItems(config, resourceResolver.findResources(pageQuery, Query.JCR_SQL2), ".html"));
        String assetQuery = "SELECT * FROM [dam:Asset] WHERE [jcr:content/" + config.getProperty()
                + "] IS NOT NULL AND (ISDESCENDANTNODE([" + config.getPath() + "]) OR [jcr:path]='" + config.getPath()
                + "')";
        log.debug("Finding assets with redirects with query: {}", assetQuery);
        entries.addAll(addItems(config, resourceResolver.findResources(assetQuery, Query.JCR_SQL2), ""));
        return entries;
    }

    public List<MapEntry> getEntries() throws IOException {
        log.trace("getEntries");

        List<MapEntry> entries = new ArrayList<MapEntry>();
        if (redirectMap != null) {
            InputStream is = redirectMap.adaptTo(InputStream.class);
            for (String line : IOUtils.readLines(is, "UTF-8")) {
                MapEntry entry = toEntry(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        if (redirects != null) {
            redirects.forEach(r -> entries.addAll(gatherEntries(r)));
        } else {
            log.debug("No redirect configurations specified");
        }

        Map<String, Integer> sources = new HashMap<String, Integer>();

        for (MapEntry entry : entries) {
            if (!sources.containsKey(entry.getSource())) {
                sources.put(entry.getSource(), 1);
            } else {
                log.trace("Found duplicate entry for {}", entry.getSource());
                sources.put(entry.getSource(), sources.get(entry.getSource()) + 1);
            }
        }
        sources.entrySet().removeIf(e -> e.getValue() <= 1);
        log.debug("Found {} duplicate entries", sources.keySet().size());

        entries.stream().filter(e -> sources.containsKey(e.getSource())).forEach(e -> {
            e.setValid(false);
            e.setStatus("Duplicate entry for " + e.getSource() + ", found redirect to " + e.getTarget());
        });

        return entries;
    }

    /**
     * Get all of the entries from the cq:Pages and dam:Assets which contain
     * whitespace in their vanity URL.
     *
     * @return
     * @throws IOException
     */
    public List<MapEntry> getInvalidEntries() throws IOException {
        log.trace("getInvalidEntries");
        List<MapEntry> invalidEntries = new ArrayList<MapEntry>();
        if (redirects != null) {
            List<MapEntry> entries = getEntries();

            invalidEntries.addAll(entries.stream().filter(e -> !e.isValid()).collect(Collectors.toList()));
            log.debug("Found {} invalid entries", invalidEntries.size());
        }
        return invalidEntries;
    }

    /**
     * Get the contents of the RedirectMap as a String
     *
     * @return
     * @throws IOException
     */
    public String getRedirectMap() throws IOException {
        log.debug("Retrieving redirect map from {}", redirectMap);

        StringBuilder sb = new StringBuilder();

        if (redirectMap != null) {
            log.debug("Loading RedirectMap file from {}", redirectMap);
            sb.append("# Redirect Map File\n");
            InputStream is = redirectMap.adaptTo(InputStream.class);
            sb.append(IOUtils.toString(is, "UTF-8"));
        } else {
            log.debug("No redirect map specified");
        }

        if (redirects != null) {
            for (RedirectConfigModel config : redirects) {
                writeEntries(config, sb);
            }
        } else {
            log.debug("No redirect configurations specified");
        }
        return sb.toString();
    }

    private MapEntry toEntry(String l) {
        String[] seg = l.split("\\s+");

        MapEntry entry = null;
        if (StringUtils.isBlank(l) || l.startsWith("#")) {
            // Skip as the line is empty or a comment
        } else if (seg.length == 2) {
            entry = new MapEntry(seg[0], seg[1], "File");
        } else if (seg.length > 2) {
            entry = new MapEntry(seg[0], seg[1], "File");
            entry.setValid(false);
            entry.setStatus(String.format(WHITESPACE_MSG, l));
        } else {
            entry = new MapEntry(seg[0], "", "File");
            entry.setValid(false);
            entry.setStatus(String.format(NO_TARGET_MSG, l));
        }
        return entry;
    }

    private void writeEntries(RedirectConfigModel config, StringBuilder sb) {
        log.trace("writeEntries");

        List<MapEntry> entries = this.gatherEntries(config);

        sb.append("\n# Dynamic entries for " + config.getResource().getPath() + "\n");
        for (MapEntry entry : entries) {
            if (entry.isValid()) {
                sb.append(entry.getSource() + " " + entry.getTarget() + "\n");
            }
        }
    }

}
