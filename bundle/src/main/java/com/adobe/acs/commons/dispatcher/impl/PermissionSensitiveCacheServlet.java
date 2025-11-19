/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.dispatcher.impl;

import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    property = "sling.servlet.paths=",
    immediate = true
)
public class PermissionSensitiveCacheServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(PermissionSensitiveCacheServlet.class);


    public void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try{

            //retrieve the requested URL
            ResourceResolver resourceResolver = request.getResourceResolver();
            String requestUri = request.getParameter( "uri" );

            log.debug( "Checking access for URI {}", requestUri );

            if( isUriValid( requestUri ) ){
                Resource requestedResource = resourceResolver.resolve( request, requestUri );

                if( !ResourceUtil.isNonExistingResource( requestedResource ) ){
                    log.debug("Current Session has access to {}", requestUri );
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    log.info("Current Session does not have access to {}", requestUri );
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }

            } else {
                log.debug( "Invalid URI {}", requestUri );
                response.setStatus( HttpServletResponse.SC_UNAUTHORIZED );
            }
        } catch(Exception e) {
            log.error("Authchecker servlet exception", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED );
        }
    }

    public boolean isUriValid( String requestUri ){
        boolean isValidUri = true;

        if(!StringUtils.startsWith( requestUri, "/" ) ){
            isValidUri = false;
        }

        return isValidUri;
    }
}
