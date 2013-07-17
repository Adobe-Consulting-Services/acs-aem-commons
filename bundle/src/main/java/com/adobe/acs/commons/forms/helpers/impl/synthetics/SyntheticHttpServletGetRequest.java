package com.adobe.acs.commons.forms.helpers.impl.synthetics;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class SyntheticHttpServletGetRequest extends HttpServletRequestWrapper {
	private static final String METHOD_GET = "GET";

	public SyntheticHttpServletGetRequest(final HttpServletRequest request) {
		super(request);
	}

	public String getMethod() {
		return METHOD_GET;
	}
}
