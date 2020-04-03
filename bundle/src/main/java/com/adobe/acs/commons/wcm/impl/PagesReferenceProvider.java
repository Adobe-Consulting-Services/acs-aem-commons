/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;

import com.day.cq.commons.PathInfo;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;

/**
 * ACS AEM Commons - Pages Reference Provider
 * Reference provider that searches for  pages referenced inside any given page resource
 */
@Component(policy = ConfigurationPolicy.REQUIRE, metatype = true, label = "ACS AEM Commons - Pages Reference Provider", description = "ACS AEM Commons - Pages Reference Provider")
@Service
public final class PagesReferenceProvider implements ReferenceProvider {

    private static final String TYPE_PAGE = "page";
    private static final String DEFAULT_PAGE_ROOT_PATH = "/content/";

    private String pageRootPath = DEFAULT_PAGE_ROOT_PATH;

    @Property(label = "page root path", description = "Page root path",
            value = DEFAULT_PAGE_ROOT_PATH)
    private static final String PAGE_ROOT_PATH = "page.root.path";

    // any text containing /content/
    private Pattern pattern = Pattern.compile("([\"']|^)("
            + Pattern.quote(pageRootPath) + ")(\\S|$)");

    @Activate
    protected void activate(Map<String, Object> props) {
        pageRootPath =
                PropertiesUtil.toString(props.get(PAGE_ROOT_PATH),
                        DEFAULT_PAGE_ROOT_PATH);
        pattern =
                Pattern.compile("([\"']|^)(" + Pattern.quote(pageRootPath)
                        + ")(\\S|$)");

    }

    @Override
    public List<Reference> findReferences(Resource resource) {
        List<Reference> references = new ArrayList<Reference>();

        ResourceResolver resolver = resource.getResourceResolver();
        PageManager pageManager = resolver.adaptTo(PageManager.class);

        Set<Page> pages = new HashSet<Page>();
        search(resource, pages, pageManager);

        for (Page page: pages) {
            Resource contentResource = page.getContentResource();
            if (contentResource != null && !contentResource.getPath().equals(resource.getPath())) {
                references.add(getReference(page));
            }
        }

        return references;
    }

    private void search(Resource resource, Set<Page> pages, PageManager pageManager) {
        findReferencesInResource(resource, pages, pageManager);
        for (Iterator<Resource> iter = resource.listChildren(); iter.hasNext();) {
            search(iter.next(), pages, pageManager);
        }
    }

    private void findReferencesInResource(Resource resource,
            Set<Page> pages, PageManager pageManager) {
        ValueMap map = resource.getValueMap();
        for (Object value : map.values()) {
            if (value instanceof String) {
                String strValue = (String) value;
                addPagesFromPropertyValue(strValue, pages, pageManager);
            } else if (value instanceof String[]) {
                for (String strValue : (String[]) value) {
                    addPagesFromPropertyValue(strValue, pages, pageManager);
                }
            }
        }
    }

    private void addPagesFromPropertyValue(String strValue, Set<Page> pages, PageManager pageManager) {
        if (pattern.matcher(strValue).find()) {
            for (String path : getAllPathsInAProperty(strValue)) {
                Page page = pageManager.getContainingPage(path);
                if (page != null) {
                    pages.add(page);
                }
            }
        }
    }

    private Reference getReference(Page page) {
        return new Reference(TYPE_PAGE,
                String.format("%s (Page)", page.getName()),
                page.getContentResource().getParent(),
                getLastModifiedTimeOfResource(page));
    }

    private long getLastModifiedTimeOfResource(Page page) {
        final Calendar mod = page.getLastModified();
        long lastModified = mod != null ? mod.getTimeInMillis() : -1;
        return lastModified;
    }

    private Set<String> getAllPathsInAProperty(String value) {

        if (isSinglePathInValue(value)) {
            return getSinglePath(value);
        } else {
            return getMultiplePaths(value);
        }
    }

    private boolean isSinglePathInValue(String value) {
        return value.startsWith("/");
    }

    private Set<String> getSinglePath(String value) {
        Set<String> paths = new HashSet<String>();
        paths.add(decode(value));
        return paths;
    }

    private Set<String> getMultiplePaths(String value) {
        Set<String> paths = new HashSet<String>();
        int startPos = value.indexOf(pageRootPath, 1);
        while (startPos != -1) {
            char charBeforeStartPos = value.charAt(startPos - 1);
            if (charBeforeStartPos == '\'' || charBeforeStartPos == '"') {
                int endPos = value.indexOf(charBeforeStartPos, startPos);
                if (endPos > startPos) {
                    String ref = value.substring(startPos, endPos);
                    paths.add(decode(ref));
                    startPos = endPos;
                }
            }
            startPos = value.indexOf(pageRootPath, startPos + 1);
        }
        return paths;
    }

    private String decode(String url) {
        return new PathInfo(url).getResourcePath();
    }
}
