package com.adobe.acs.commons.sitemaps;

import org.apache.sling.api.resource.ResourceResolver;
import org.w3c.dom.Document;

public interface SiteMapGenerator {
	Document getSiteMap(ResourceResolver resolver);
}
