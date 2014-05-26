package com.adobe.acs.commons.solr;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

public interface SolrMetadataBuilder {

    public enum ResolvingType {
	CQ_PAGE, DAM_ASSET
    }


    

	/**
	 * return the metadata as JSON object
	 * @param path the path 
	 * @return
	 */
    public void buildMetadata(Resource path, JSONObject json)
	    throws JSONException;
	
	    /**
     * Determines if this builder is able to handle this specific resourcetype;
     * depending on the type the meaning of resourcetype is different
     * 
     * CQ_PAGE: that's the sling:resourceType of the page DAM_ASSET: mimetype of
     * the asset
     * 
     * @param resourcetype
     * @return
     */
    public boolean canHandle(String resourcetype, ResolvingType type);
	
}
