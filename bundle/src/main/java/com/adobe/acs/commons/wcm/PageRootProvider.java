package com.adobe.acs.commons.wcm;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;

/**
 * Service to fetch the site root page (i.e. home page) for a given resource.
 */
public interface PageRootProvider {
    Page getRootPage(Resource resource);
}
