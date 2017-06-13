package com.adobe.acs.commons.wcm.vanity;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

public interface VanityURLService {
	
	/**
	 * @param vanityPath Vanity path that needs to be validated.
	 * @param request SlingHttpServletRequest object used for performing query/lookup
	 * @return return true if valid else false
	 */
	boolean isValidVanityURL(String vanityPath, SlingHttpServletRequest request);

	
	/**
	 * This method checks if a given request URI (after performing the Resource Resolver Mapping) is a valid vanity URL, 
	 * if true it will perform the FORWARD using Request Dispatcher.  
	 * 
	 * 
	 * @param request
	 * @param response
	 * @return true if this request is dispatched cause it's a valid Vanity path, else false.
	 */
	boolean dispatch(SlingHttpServletRequest request, SlingHttpServletResponse response);

}
