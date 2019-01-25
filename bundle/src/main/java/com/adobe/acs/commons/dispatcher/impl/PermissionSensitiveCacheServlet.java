/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        configurationPolicy=ConfigurationPolicy.REQUIRE
)
@Designate(ocd=PermissionSensitiveCacheServlet.Config.class)
public class PermissionSensitiveCacheServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(PermissionSensitiveCacheServlet.class);
    
    @ObjectClassDefinition(
            name = "ACS AEM Commons - Permission Sensitive Cache Servlet"
    )
    public @interface Config {
        @AttributeDefinition(
                name="Sling Servlet Paths",
                description="Paths that this servlet will resolve to"
        )
        String[] sling_servlet_paths();
    }

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
                    response.setStatus(SlingHttpServletResponse.SC_OK);
                } else {
                    log.info("Current Session does not have access to {}", requestUri );
                    response.setStatus(SlingHttpServletResponse.SC_UNAUTHORIZED);
                }

            } else {
                log.debug( "Invalid URI {}", requestUri );
                response.setStatus( SlingHttpServletResponse.SC_UNAUTHORIZED );
            }
        } catch(Exception e) {
            log.error("Authchecker servlet exception", e);
            response.setStatus( SlingHttpServletResponse.SC_UNAUTHORIZED );
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
