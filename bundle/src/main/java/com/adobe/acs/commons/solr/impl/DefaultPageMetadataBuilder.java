package com.adobe.acs.commons.solr.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.solr.SolrMetadataBuilder;
import com.day.cq.wcm.api.Page;

/**
 * This SolrMetadataBuilder is capable to handle all pages in a very unspecific
 * manner. Should be considered the fallback mechanism.
 * 
 * 
 */

@Service
@Component()
@Property(name = Constants.SERVICE_RANKING, intValue = Integer.MAX_VALUE)
public class DefaultPageMetadataBuilder implements SolrMetadataBuilder {

    private final static Logger log = LoggerFactory
	    .getLogger(DefaultPageMetadataBuilder.class);


    @Override
    public void buildMetadata(Resource path, JSONObject json)
	    throws JSONException {

	Page p = path.adaptTo(Page.class);
	if (p == null) {
	    // somethings really weird, as this was working before --> maybe
	    // resource removed?
	    log.warn("failed to adapt to Page (resourcepath={}", path.getPath());
	} else {

	    Resource jcrContent = p.getContentResource();

	    json.put("title", p.getPageTitle());
	}

    }

    @Override
    public boolean canHandle(String resourcetype, ResolvingType type) {
	return (type == ResolvingType.CQ_PAGE);
    }

}
