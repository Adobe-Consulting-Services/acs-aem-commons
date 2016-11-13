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
     * Returns the root page for the provided resource. The root page is selected via the regex provided in the PageRootProviderImpl's OSGi configuration.
     * @param resource
     * @return
     */
    Page getRootPage(Resource resource);
}
