/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum.impl.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Provides CORS functionality required by the server-to-server XHR communication
 */
@SuppressWarnings({"serial", "checkstyle:abbreviationaswordinname"})
public class BaseChecksumServlet extends SlingAllMethodsServlet {
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    private static final String ORIGIN = "Origin";


    protected final boolean isAnonymous(SlingHttpServletRequest request) {
        return "anonymous".equals(request.getResourceResolver().getUserID());
    }

    protected final void doOptions(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
            IOException,
            ServletException {

        handleCORS(request, response);
    }

    protected void handleCORS(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        final String origin = request.getHeader(ORIGIN);

        if (origin != null && origin.length() > 0) {
            response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, "Authorization");
            response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST");
        }
    }
}