/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.wcm.comparisons.impl;

import com.adobe.acs.commons.wcm.comparisons.PageCompareData;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLine;
import com.adobe.acs.commons.wcm.comparisons.VersionSelection;
import com.day.cq.wcm.api.NameConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

class PageCompareDataImpl implements PageCompareData {

    private static final Logger log = LoggerFactory.getLogger(PageCompareDataImpl.class);

    private final Resource resource;
    private final String versionName;

    private Date versionDate;

    private final List<PageCompareDataLine> lines = new ArrayList<PageCompareDataLine>();

    private final List<VersionSelection> versionSelection = new ArrayList<VersionSelection>();

    PageCompareDataImpl(Resource resource, String versionName) throws RepositoryException {
        this.resource = resource.isResourceType(NameConstants.NT_PAGE) ? resource.getChild(NameConstants.NN_CONTENT) : resource;
        this.versionName = versionName;

        initialize();
    }

    private void initialize() throws RepositoryException {
        if (versionName.equals("latest")) {
            populate(resource, resource.getPath(), 0);
            versionDate = Properties.lastModified(resource);
        }

        VersionManager versionManager = resource.getResourceResolver().adaptTo(Session.class).getWorkspace().getVersionManager();
        try {
            VersionHistory history = versionManager.getVersionHistory(this.resource.getPath());
            VersionIterator versionIterator = history.getAllVersions();
            while (versionIterator.hasNext()) {
                Version next = versionIterator.nextVersion();
                versionSelection.add(new VersionSelectionImpl(next.getName(), next.getCreated().getTime()));
                if (next.getName().equalsIgnoreCase(versionName)) {
                    String versionPath = next.getFrozenNode().getPath();
                    Resource versionResource = resource.getResourceResolver().resolve(versionPath);
                    populate(versionResource, versionPath, 0);
                    versionDate = next.getCreated().getTime();
                }
            }
        } catch (javax.jcr.UnsupportedRepositoryOperationException e) {
            log.debug(String.format("node %s not versionable", this.resource.getPath()));
        }
        versionSelection.add(new VersionSelectionImpl("latest", Properties.lastModified(resource)));
    }


    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public String getVersion() {
        return versionName;
    }

    @Override
    public Date getVersionDate() {
        return Optional.ofNullable(versionDate)
                .map(date -> (Date) date.clone())
                .orElse(null);
    }

    @Override
    public String getPath() {
        return resource.getPath();
    }

    @Override
    public List<VersionSelection> getVersions() {
        return Collections.unmodifiableList(versionSelection);
    }

    @Override
    public List<PageCompareDataLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void populate(final Resource resource, final String basePath, final int depth) throws RepositoryException {
        final ValueMap map = resource.getValueMap();
        final List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        for (final String key : keys) {
            final Property property = resource.adaptTo(Node.class).getProperty(key);
            lines.add(new PageCompareDataLineImpl(property, basePath, depth + 1));
        }

        final Iterator<Resource> iter = resource.getChildren().iterator();
        while (iter.hasNext()) {
            final Resource child = iter.next();
            lines.add(new PageCompareDataLineImpl(child, basePath, depth + 1));
            populate(child, basePath, depth + 1);
        }
    }
}
