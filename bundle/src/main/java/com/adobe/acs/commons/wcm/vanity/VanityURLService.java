package com.adobe.acs.commons.wcm.vanity;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;

@ProviderType
public interface VanityURLService {

	/**
	 * This method checks if a given request URI (after performing the Resource Resolver Mapping) is a valid vanity URL, 
	 * if true it will perform the FORWARD using Request Dispatcher.  
	 *
	 * @param request the request object
	 * @param response the response object
	 * @return true if this request is dispatched because it's a valid Vanity path, else false.
	 */
	boolean dispatch(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException, RepositoryException;
}