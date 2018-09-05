/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.errorpagehandler;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import aQute.bnd.annotation.ProviderType;

/**
 * Error Page Handling Service which facilitates the resolution of errors against authorable pages for discrete content trees.
 *
 * This service is used via the ACS-AEM-Commons error page handler implementation to create author-able error pages.
 */
@ProviderType

@SuppressWarnings("squid:S1214")
public interface ErrorPageHandlerService {
    int DEFAULT_STATUS_CODE = SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    /**
     * Determines if this Service is "enabled". If it has been configured to be "Disabled" the Service still exists however it should not be used.
     * This OSGi Property toggle allows error page handler to be toggled on an off without via OSGi means without throwing Null pointers, etc.
     *
     * @return true is the Service should be considered enabled
     */
    boolean isEnabled();

    /**
     * Find the JCR full path to the most appropriate Error Page
     *
     * @param request
     * @param errorResource
     * @return
     */
    String findErrorPage(SlingHttpServletRequest request, Resource errorResource);

    /**
     * Get Error Status Code from Request or Default (500) if no status code can be found
     *
     * @param request
     * @return
     */
    int getStatusCode(SlingHttpServletRequest request);

    /**
     * Get the Error Page's name (all lowercase) that should be used to render the page for this error.
     *
     * This looks at the Servlet Sling has already resolved to handle this request (making Sling do all the hard work)!
     *
     * @param request
     * @return
     */
    String getErrorPageName(SlingHttpServletRequest request);

    /**
     * Determine is the request is a 404 and if so handles the request appropriately base on some CQ idiosyncrasies .
     *
     * Invokes the AEM Login Selector Autheticator on 404'ing requests made by anonymous users.
     *
     * @param request
     * @param response
     *
     * @return true if the error page handler should process the 404; false is the AEM authenticator has been invoked
     * and responsible for processing the request further
     */
    boolean doHandle404(SlingHttpServletRequest request, SlingHttpServletResponse response);

    /**
     * Returns the Exception Message (Stacktrace) from the Request
     *
     * @param request
     * @return
     */
    String getException(SlingHttpServletRequest request);

    /**
     * Returns a String representation of the RequestProgress trace
     *
     * @param request
     * @return
     */
    String getRequestProgress(SlingHttpServletRequest request);

    /**
     * Reset response attributes to support printing out a new page (rather than one that potentially errored out).
     * This includes clearing clientlib inclusion state, and resetting the response.
     *
     * If the response is committed, and it hasnt been closed by code, check the response AND jsp buffer sizes and ensure they are large enough to NOT force a buffer flush.
     * @param request
     * @param response
     * @param statusCode
     */
    void resetRequestAndResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, int statusCode);

    /**
     * Include the path, forcing the request method to be GET. This method will silently
     * swallow exceptions.
     * 
     * @param request the request
     * @param response the response
     * @param path the path
     */
    @SuppressWarnings("checkstyle:abbreviationaswordinname")
    void includeUsingGET(SlingHttpServletRequest request, SlingHttpServletResponse response, String path);
    
    
    /**
     * Determines if Vanity-URL dispatch check is enabled. When enabled and current request URI is a valid vanity (after performing resource resolver mapping),
     * request will be forwarded.
     * 
     * @return true if check is enabled else false
     */
    boolean isVanityDispatchCheckEnabled();
}
