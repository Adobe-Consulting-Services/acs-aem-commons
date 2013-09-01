package com.adobe.acs.commons.forms.helpers.impl.synthetics;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

/**
 * Used to turn a POST request into a GET request for Internal Forwarding
 */
public class SyntheticSlingHttpServletGetRequest extends SlingHttpServletRequestWrapper {
	private static final String METHOD_GET = "GET";

	public SyntheticSlingHttpServletGetRequest(final SlingHttpServletRequest request) {
		super(request);
	}

	@Override
	public String getMethod() {
		return METHOD_GET;
	}
}
