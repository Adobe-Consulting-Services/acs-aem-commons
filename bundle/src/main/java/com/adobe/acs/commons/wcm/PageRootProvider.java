package com.adobe.acs.commons.wcm;

import aQute.bnd.annotation.ConsumerType;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;

/**
 * Service to fetch the site root page (i.e. home page) for a given resource.
 */
@ConsumerType
public interface PageRootProvider {
    /**
     * Returns the root page for the provided resource. The root page is selected
     * via the regex(es) provided in the PageRootProviderImpl's OSGi configuration.
     * @param resource The Resource for which to return the root page
     * @return Root page
     */
    Page getRootPage(Resource resource);

    /**
     * Returns the root path for the provided resource path. The root path is selected
     * via the regex(es) provided in the PageRootProviderImpl's OSGi configuration.
     * @param resourcePath The path for which to return the root path
     * @return Root path
     */
    String getRootPagePath(String resourcePath);
}
