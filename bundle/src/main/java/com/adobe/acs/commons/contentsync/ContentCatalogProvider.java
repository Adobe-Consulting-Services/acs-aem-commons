package com.adobe.acs.commons.contentsync;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface ContentCatalogProvider {
    List<CatalogItem> getItems(SlingHttpServletRequest request);

    boolean isModified(CatalogItem catalogItem, Resource targetResource);

    /**
     *
     * @return  message to print in the UI
     */
    String getMessage(CatalogItem catalogItem, Resource targetResource);
}
