package com.adobe.acs.commons.errorpagehandler;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

/**
 * Error Page Handling Service which facilitates the resolution of errors against authorable pages for discrete content trees.
 *
 * This service is used via the ACS-AEM-Commons error page handler implementation to create author-able error pages.
 */
public interface ErrorPageHandlerService {
    public static final int DEFAULT_STATUS_CODE = SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    /**
     * Determines if this Service is "enabled". If it has been configured to be "Disabled" the Service still exists however it should not be used.
     * This OSGi Property toggle allows error page handler to be toggled on an off without via OSGi means without throwing Null pointers, etc.
     *
     * @return true is the Service should be considered enabled
     */
    public boolean isEnabled();

    /**
     * Find the JCR full path to the most appropriate Error Page
     *
     * @param request
     * @param errorResource
     * @return
     */
    public String findErrorPage(SlingHttpServletRequest request, Resource errorResource);

    /**
     * Get Error Status Code from Request or Default (500) if no status code can be found
     *
     * @param request
     * @return
     */
    public int getStatusCode(SlingHttpServletRequest request);

    /**
     * Get the Error Page's name (all lowercase) that should be used to render the page for this error.
     *
     * This looks at the Servlet Sling has already resolved to handle this request (making Sling do all the hard work)!
     *
     * @param request
     * @return
     */
    public String getErrorPageName(SlingHttpServletRequest request);


    /**
     * Determine is the request is a 404 and if so handles the request appropriately base on some CQ idiosyncrasies .
     *
     * Mainly forces an authentication request in Authoring modes (!WCMMode.DISABLED)
     *
     * @param request
     * @param response
     */
    public void doHandle404(SlingHttpServletRequest request, SlingHttpServletResponse response);

    /**
     * Returns the Exception Message (Stacktrace) from the Request
     *
     * @param request
     * @return
     */
    public String getException(SlingHttpServletRequest request);

    /**
     * Returns a String representation of the RequestProgress trace
     *
     * @param request
     * @return
     */
    public String getRequestProgress(SlingHttpServletRequest request);

    /**
     * Reset response attributes to support printing out a new page (rather than one that potentially errored out).
     * This includes clearing clientlib inclusion state, and resetting the response.
     *
     * If the response is committed, and it hasnt been closed by code, check the response AND jsp buffer sizes and ensure they are large enough to NOT force a buffer flush.
     * @param request
     * @param response
     * @param statusCode
     */
    public void resetRequestAndResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, int statusCode);
}
