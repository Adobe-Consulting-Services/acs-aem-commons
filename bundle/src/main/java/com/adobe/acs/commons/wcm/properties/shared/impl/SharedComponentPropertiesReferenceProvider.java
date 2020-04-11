/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * SharedPropertiesReferenceProvider finds the components that have been shared across the site
 * using the current resource by going over the components that are present in the current resource
 * page and its template. After identifying the shared components under the current resource page,
 * this adds the root page to the list of references that needs to be published.
 */
@Component(service = ReferenceProvider.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public final class SharedComponentPropertiesReferenceProvider implements ReferenceProvider {

    @org.osgi.service.component.annotations.Reference
    private PageRootProvider pageRootProvider;

    @Override
    public List<Reference> findReferences(Resource resource) {
        List<Reference> references = new ArrayList<>();

        if (!resource.getPath().startsWith("/content")) {
            return references;
        }

        ResourceResolver resourceResolver = resource.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return references;
        }

        Page page = pageManager.getContainingPage(resource);
        if (page == null) {
            return references;
        } else {

            Set<String> componentResourceTypes = collectComponentResourceTypes(page.getContentResource());

            if (page.getTemplate() != null) {
                Resource templateResource = resourceResolver
                    .getResource(page.getTemplate().getPath());
                componentResourceTypes.addAll(collectComponentResourceTypes(templateResource));
            }

            if (componentResourceTypes.size() > 0) {
                references = findSharedComponents(resource, componentResourceTypes);
            }
        }

        return references;
    }


    /**
     * Collect the resourceType of current resource and its children recursively.
     * Avoided the usage of resource.getResourceType() as it returns the "jcr:primaryType"
     * when the resource doesn't have "sling:resourceType" property.
     *
     * @param resource {@link Resource}
     */
    private Set<String> collectComponentResourceTypes(Resource resource) {
        Set<String> componentResourceTypes = new HashSet<>();
        if (resource != null) {
            String componentResourceType = resource.getValueMap()
                .get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, String.class);
            if (StringUtils.isNotEmpty(componentResourceType)) {
                componentResourceTypes.add(componentResourceType);
            }

            if (resource.hasChildren()) {
                Iterator<Resource> children = resource.listChildren();
                while (children.hasNext()) {
                    componentResourceTypes.addAll(collectComponentResourceTypes(children.next()));
                }
            }
        }
        return componentResourceTypes;
    }


    /**
     * Iterates through the given collection of resourceTypes and finds all the components that has
     * shared dialog, then adds the root page of the given resource to the list.
     *
     * @param resource {@link Resource}
     * @param componentResourceTypes {@link Set<String>}
     * @return references
     */
    private List<Reference> findSharedComponents(Resource resource,
        Set<String> componentResourceTypes) {
        List<Reference> references = new ArrayList<>();

        ResourceResolver resourceResolver = resource.getResourceResolver();
        for (String resourceType : componentResourceTypes) {

            Resource componentResource = getComponentResource(resourceType, resourceResolver);
            if (componentResource == null) {
                continue;
            }

            String jcrPrimaryType = componentResource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class);
            if (StringUtils.isEmpty(jcrPrimaryType) || !jcrPrimaryType.equalsIgnoreCase("cq:Component")) {
                continue;
            }

            Resource sharedDialog = componentResource.getResourceResolver()
                .getResource(componentResource.getPath() + "/dialogshared");
            Resource globalDialog = componentResource.getResourceResolver()
                .getResource(componentResource.getPath() + "/dialogglobal");
            if (sharedDialog == null && globalDialog == null) {
                continue;
            }

            Page rootPage = pageRootProvider.getRootPage(resource);
            if (rootPage != null) {
                references.add(new Reference("sharedProperties", getReferenceName(rootPage),
                    rootPage.getContentResource(),
                    rootPage.getLastModified() != null ? rootPage.getLastModified()
                        .getTimeInMillis() : -1L));

                return references;
            }
        }
        return references;
    }

    /**
     * Returns the Component Resource by searching the configured resolver search paths ["/libs", "/apps"]
     * @param resourceType {@link String}
     * @param resourceResolver {@link ResourceResolver}
     * @return Resource
     */
    private Resource getComponentResource(String resourceType, ResourceResolver resourceResolver) {
        String[] resolverSearchPaths = resourceResolver.getSearchPath();

        for (String searchPath : resolverSearchPaths) {
            Resource resource = resourceResolver.getResource(searchPath + resourceType);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    /**
     * Get the page title prop if it's null get the name
     *
     * @param page {@link Page}
     * @return page title | name
     */
    private String getReferenceName(@Nonnull Page page) {
        return StringUtils.defaultIfEmpty(page.getTitle(), page.getName());
    }

}

