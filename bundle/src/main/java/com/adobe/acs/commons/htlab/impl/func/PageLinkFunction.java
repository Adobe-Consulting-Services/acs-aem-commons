/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.htlab.impl.func;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.adobe.acs.commons.htlab.HTLabFunction;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.adobe.acs.commons.htlab.HTLabContext;
import com.adobe.acs.commons.htlab.HTLabMapResult;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs page links by establishing a page path, appending .html, and then mapping the page path using a
 * {@link org.apache.sling.api.resource.ResourceResolver}.
 */
@Component
@Service
@Properties({
        @Property(name = HTLabFunction.OSGI_FN_NAME, value = "pageLink", propertyPrivate = true)
})
public class PageLinkFunction implements HTLabFunction {
    private static final Logger LOG = LoggerFactory.getLogger(PageLinkFunction.class);

    @Nonnull
    @Override
    public HTLabMapResult apply(@Nonnull HTLabContext context, @Nonnull String key, @CheckForNull Object value) {
        LOG.debug("[pageLink] key={}, value={}", key, value);
        SlingHttpServletRequest request = context.getRequest();
        Resource reqResource = context.getResource();
        ResourceResolver resolver = request != null ? request.getResourceResolver() : context.getResolver();
        if (request == null || resolver == null || reqResource == null) {
            return HTLabMapResult.failure();
        }

        PageManager pageManager = resolver.adaptTo(PageManager.class);
        String path = null;
        if (value instanceof Page) {
            path = ((Page) value).getPath();
        } else if (value instanceof Resource) {
            Page page = ((Resource) value).adaptTo(Page.class);
            if (page == null) {
                if (pageManager != null) {
                    page = pageManager.getContainingPage((Resource) value);
                    path = page.getPath();
                }
            }
        } else if (value instanceof Adaptable) {
            Page page = ((Adaptable) value).adaptTo(Page.class);
            if (page != null) {
                path = page.getPath();
            }
        } else if (value instanceof String) {
            // try to assume value is a resource path.
            String resourcePath = (String) value;
            if (resourcePath.startsWith("/content/") && !resourcePath.startsWith("/content/dam/")) {
                Page page = null;
                if (pageManager != null) {
                    page = pageManager.getContainingPage(resourcePath);
                }
                if (page != null) {
                    path = page.getPath();
                } else {
                    path = resourcePath;
                }
            }
        }

        if (path != null) {
            String mapped = resolver.map(request, path + ".html");
            return HTLabMapResult.success(mapped);
        }

        return HTLabMapResult.forwardValue();
    }
}
