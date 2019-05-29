package com.adobe.acs.commons.properties;

import java.util.Map;

import com.day.cq.wcm.api.Page;

import org.apache.sling.api.resource.Resource;

public interface PropertyAggregatorService {

    /**
     * Iterates up the content tree to aggregate all the current page properties and inherited
     * page properties. Assigns the appropriate namespace to the properties as well.
     *
     * @param resource The content resource of a page
     * @return The map of properties
     */
    Map<String, Object> getProperties(Resource resource);

    /**
     * Overloaded method from above. Passes the content resource of the page.
     *
     * @param page The current page
     * @return The map of properties
     */
    Map<String, Object> getProperties(Page page);
}
