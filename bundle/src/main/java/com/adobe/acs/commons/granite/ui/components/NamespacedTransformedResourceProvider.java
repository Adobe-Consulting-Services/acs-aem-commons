package com.adobe.acs.commons.granite.ui.components;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

/**
 * NamespacedTransformedResourceProvider
 * <p>
 * Transforms a resource underlying children with a namespace.
 * </p>
 *
 * @author raaijmak@adobe.com
 * @since 2020-03-09
 */
public interface NamespacedTransformedResourceProvider {
    
    /**
     * Transforms a resource underlying children with a namespace.
     * Children under the resource will have various properties (the ones configured under the service) prefixed with the namespace
     * @param request
     * @param targetResource
     * @return Wrapped resource
     */
    Resource transformResourceWithNameSpacing(SlingHttpServletRequest request, Resource targetResource);
    
}
