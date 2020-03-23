package com.adobe.acs.commons.wcm.properties.shared.impl;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
public class SharedComponentPropertiesReferenceProvider implements ReferenceProvider {

    @org.osgi.service.component.annotations.Reference
    private PageRootProvider pageRootProvider;

    private Set<String> componentResourceTypes = new HashSet<>();

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

            collectComponentResourceTypes(page.getContentResource());

            if (page.getTemplate() != null) {
                Resource templateResource = resourceResolver
                    .getResource(page.getTemplate().getPath());
                collectComponentResourceTypes(templateResource);
            }

            if (componentResourceTypes.size() > 0) {
                return findSharedComponents(componentResourceTypes, resource);
            }
        }

        return references;
    }


    /**
     * Collect the resourceType of current resource and its children recursively.
     * Avoided the usage of resource.getResourceType() as it returns the "jcr:primaryType" when the resource doesn't have "sling:resourceType" property.
     * @param resource {@link Resource}
     */
    private void collectComponentResourceTypes(Resource resource) {
        if (resource != null) {
            String componentResourceType = resource.getValueMap().get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, String.class);
            if (StringUtils.isNotEmpty(componentResourceType)) {
                componentResourceTypes.add(componentResourceType);
            }
            if (resource.hasChildren()) {
                Iterator<Resource> children = resource.listChildren();
                while (children.hasNext()) {
                    collectComponentResourceTypes(children.next());
                }
            }
        }
    }


    /**
     * Iterates through the given collection of resourceTypes and finds all the components that has
     * shared dialog, then adds the root page of the given resource to the list.
     *
     * @param resourceTypes {@link Set}
     * @param resource {@link Resource}
     * @return references
     */
    private List<Reference> findSharedComponents(Set<String> resourceTypes, Resource resource) {
        List<Reference> references = new ArrayList<>();

        ResourceResolver resourceResolver = resource.getResourceResolver();
        for (String resourceType : resourceTypes) {

            if (StringUtils.isNotEmpty(resourceType)) {

                String[] resolverSearchPaths = resourceResolver.getSearchPath();

                for (String searchPath : resolverSearchPaths) {
                    String resourceType_1 = searchPath + resourceType;
                    if (resourceResolver.getResource(resourceType_1) != null) {
                        resourceType = searchPath + resourceType;
                        break;
                    }
                }

                Resource componentResource = resource.getResourceResolver()
                    .getResource(resourceType);
                if (componentResource == null) {
                    continue;
                }

                com.day.cq.wcm.api.components.Component component = componentResource
                    .adaptTo(com.day.cq.wcm.api.components.Component.class);
                if (component == null) {
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
                }
            }
        }
        return references;
    }

    /**
     * Get the page title prop if it's null get the name
     *
     * @param page {@link Page}
     * @return page title | name
     */
    private String getReferenceName(Page page) {
        String title = null;
        if (page != null) {
            title = page.getTitle() != null ? page.getTitle() : page.getName();
        }
        return title;
    }

}

