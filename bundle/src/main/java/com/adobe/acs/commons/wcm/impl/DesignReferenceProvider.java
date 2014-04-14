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
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;

import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;

/**
 * 
 * ACS AEM commons Design Reference Provider.
 * Reference provider that searches for design pages for any given page resource.
 *
 */
@Component(
        label = "ACS AEM Commons - Design Reference Provider",
        description = "Reference provider that searches for design pages for any given page resource",
        metatype = false , policy = ConfigurationPolicy.REQUIRE)
@Service
public class DesignReferenceProvider implements ReferenceProvider {

    private static final String TYPE_DESIGN_PAGE = "designpage";
    private static final String DESIGN_PATH = "cq:designPath";

    @Override
    public final List<Reference> findReferences(Resource resource) {
        String designPath = getDesignPath(resource);
        if (null == designPath || "".equals(designPath)) {
            return Collections.emptyList();
        }
        Resource designResource =
                resource.getResourceResolver().getResource(designPath);
        Page designPage = designResource.adaptTo(Page.class);
        List<Reference> references = new ArrayList<Reference>(1);
        references.add(new Reference(TYPE_DESIGN_PAGE,
                designResource.getName(), designResource,
                getLastModifiedTimeOfResource(designPage)));
        return references;
    }

    private long getLastModifiedTimeOfResource(Page page) {
        final Calendar mod = page.getLastModified();
        long lastModified = mod != null ? mod.getTimeInMillis() : -1;
        return lastModified;
    }

    private String getDesignPath(Resource resource) {
        HierarchyNodeInheritanceValueMap hnvm =
                new HierarchyNodeInheritanceValueMap(resource);
        return hnvm.getInherited(DESIGN_PATH, "");
    }
}
