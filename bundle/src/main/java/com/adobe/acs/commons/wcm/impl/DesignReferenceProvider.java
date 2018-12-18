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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;

/**
 * 
 * ACS AEM commons Design Reference Provider.
 * Reference provider that searches for design pages for any given page resource.
 */
@Component(policy = ConfigurationPolicy.REQUIRE)
@Service
public final class DesignReferenceProvider implements ReferenceProvider {

    private static final String TYPE_DESIGN_PAGE = "designpage";

    @Override
    public List<Reference> findReferences(Resource resource) {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Designer designer = resourceResolver.adaptTo(Designer.class);

        Page page = pageManager.getContainingPage(resource);
        if (page == null) {
            return Collections.emptyList();
        }

        Design design = designer.getDesign(page);
        if (design == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new Reference(TYPE_DESIGN_PAGE,
                String.format("%s (Design)", design.getId()),
                design.getContentResource(),
                getLastModified(design)));
    }

    private long getLastModified(Design design) {
        final Calendar mod = design.getLastModified();
        long lastModified = mod != null ? mod.getTimeInMillis() : -1;
        return lastModified;
    }
}
