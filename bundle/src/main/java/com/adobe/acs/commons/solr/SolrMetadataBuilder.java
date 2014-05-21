package com.adobe.acs.commons.solr;

import org.apache.sling.commons.json.JSONObject;

public interface SolrMetadataBuilder {

	/**
	 * return the metadata as JSON object
	 * @param path the path 
	 * @return
	 */
	public JSONObject[] getMetadata (String path);
	
	/**
	 * Determines if this builder is able to handle this specific resourcetype
	 * @param resourcetype
	 * @return
	 */
	public boolean canHandle (String resourcetype);
	
}
