package com.adobe.acs.commons.solr.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;

import com.adobe.acs.commons.solr.SolrMetadataBuilder;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = Integer.MAX_VALUE)
public class DefaultAssetMetadataBuilder implements SolrMetadataBuilder {

    @Override
    public void buildMetadata(Resource path, JSONObject json)
	    throws JSONException {
	// TODO Auto-generated method stub

    }

    @Override
    public boolean canHandle(String resourcetype, ResolvingType type) {
	return (type == ResolvingType.DAM_ASSET);
    }

}
