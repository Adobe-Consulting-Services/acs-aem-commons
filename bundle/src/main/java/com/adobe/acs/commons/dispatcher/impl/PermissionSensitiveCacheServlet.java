/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.dispatcher.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        label = "ACS AEM Commons - Permission Sensitive Cache Servlet",
        description = "Servlet that checks if the current sessions has access to a cached object",
        metatype = true, immediate = true
)
@Properties({
        @Property(
                name = "sling.servlet.paths",
                cardinality = Integer.MAX_VALUE,
                label = "Sling Servlet Paths",
                description = "Paths that this servlet will resolve to"
        )
})
@Service
public class PermissionSensitiveCacheServlet extends SlingSafeMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(PermissionSensitiveCacheServlet.class);


    public void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try{

            //retrieve the requested URL
            ResourceResolver resourceResolver = request.getResourceResolver();
            String requestUri = request.getParameter( "uri" );

            log.debug( "Checking access for URI {}", requestUri );

            Resource requestedResource = resourceResolver.resolve( request, requestUri );

            if( !ResourceUtil.isNonExistingResource( requestedResource ) ){
                log.debug("Current Session has access to {}", requestUri );
                response.setStatus(SlingHttpServletResponse.SC_OK);
            } else {
                log.info("Current Session does not have access to {}", requestUri );
                response.setStatus(SlingHttpServletResponse.SC_UNAUTHORIZED);
            }

        } catch(Exception e) {
            log.error("Authchecker servlet exception", e);
            response.setStatus( SlingHttpServletResponse.SC_UNAUTHORIZED );
        }
    }
}
