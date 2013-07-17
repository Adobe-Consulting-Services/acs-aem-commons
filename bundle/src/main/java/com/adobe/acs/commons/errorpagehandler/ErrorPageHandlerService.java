package com.adobe.acs.commons.errorpagehandler;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

public interface ErrorPageHandlerService {
    public static final String STATUS_CODE = "javax.servlet.error.status_code";
    public static final String SERVLET_NAME = "javax.servlet.error.servlet_name";
    public static final int DEFAULT_STATUS_CODE = 500;

    public boolean isEnabled();

    public String findErrorPage(SlingHttpServletRequest request, Resource errorResource);
    public int getStatusCode(SlingHttpServletRequest request);
    public String getErrorPageName(SlingHttpServletRequest request);

    public boolean isAuthorModeRequest(SlingHttpServletRequest request);
    public boolean isAuthorPreviewModeRequest(SlingHttpServletRequest request);

    public void doHandle404(SlingHttpServletRequest request, SlingHttpServletResponse response);

    public String getException(SlingHttpServletRequest request);
    public String getRequestProgress(SlingHttpServletRequest request);

    public void resetRequestAndResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, int statusCode);
}
