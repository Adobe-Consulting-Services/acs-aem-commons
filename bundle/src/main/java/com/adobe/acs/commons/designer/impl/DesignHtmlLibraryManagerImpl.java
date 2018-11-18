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
package com.adobe.acs.commons.designer.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.designer.DesignHtmlLibraryManager;
import com.adobe.acs.commons.designer.PageRegion;
import com.day.cq.wcm.api.designer.Design;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;

/**
 * ACS Commons - Design HTML Library Manager
 * Helper service used to expose configured Design-specific client libraries in JSPs.
 *
 */
@Component
public final class DesignHtmlLibraryManagerImpl implements DesignHtmlLibraryManager {
    private static final Logger log = LoggerFactory.getLogger(DesignHtmlLibraryManagerImpl.class);

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    @Override
    public void writeCssInclude(final SlingHttpServletRequest request, final Design design,
            final PageRegion pageRegion, final Writer writer) throws IOException {
        htmlLibraryManager.writeCssInclude(request, writer, this.getCssLibraries(design, pageRegion));
    }

    @Override
    public void writeJsInclude(final SlingHttpServletRequest request, final Design design,
            final PageRegion pageRegion, final Writer writer) throws IOException {
        htmlLibraryManager.writeJsInclude(request, writer, this.getJsLibraries(design, pageRegion));
    }

    @Override
    public void writeIncludes(final SlingHttpServletRequest request, final Design design,
            final PageRegion pageRegion, final Writer writer) throws IOException {
        writeCssInclude(request, design, pageRegion, writer);
        writeJsInclude(request, design, pageRegion, writer);
    }

    @Override
    public String[] getCssLibraries(final Design design, final PageRegion pageRegion) {
        final ValueMap cssProps = this.getPageRegionProperties(design, pageRegion);
        return cssProps.get(PROPERTY_CSS, new String[] {});
    }

    @Override
    public String[] getJsLibraries(final Design design, final PageRegion pageRegion) {
        final ValueMap jsProps = this.getPageRegionProperties(design, pageRegion);
        return jsProps.get(PROPERTY_JS, new String[] {});
    }

    @Override
    public String[] getLibraries(final Design design, final PageRegion pageRegion) {
        final String[] cssLibs = this.getCssLibraries(design, pageRegion);
        final String[] jsLibs = this.getJsLibraries(design, pageRegion);

        final LinkedHashSet<String> libs = new LinkedHashSet<String>();
        libs.addAll(Arrays.asList(cssLibs));
        libs.addAll(Arrays.asList(jsLibs));

        return libs.toArray(new String[libs.size()]);
    }

    /**
     * Gets the ValueMap that contains the client library lists for the specified design and PageRegion.
     *
     * @param design
     * @param pageRegion
     * @return the ValueMap associated with the PageRegion;
     *         CSS and JS libraries can be looked up via PROPERTY_CSS and PROPERTY_JS
     */
    private ValueMap getPageRegionProperties(final Design design, final PageRegion pageRegion) {
        final String relPath = RESOURCE_NAME + "/" + pageRegion;

        final ValueMap empty = new ValueMapDecorator(new HashMap<String, Object>());

        if (design == null) {
            log.warn("Cannot find properties for `null` Design");
            return empty;
        } else if (design.getContentResource() == null) {
            log.warn("Cannot find properties for `null` Design content resource");
            return empty;
        } else if (design.getContentResource().getChild(relPath) == null) {
            log.warn("Could not find resource: {}", design.getContentResource().getPath() + "/" + relPath);
            return empty;
        }

        return design.getContentResource().getChild(relPath).adaptTo(ValueMap.class);
    }
}
