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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.adobe.acs.commons.dispatcher.impl.PermissionSensitiveCacheServlet.REMOVEABLE_EXTENSIONS;

@Component(
        label = "ACS AEM Commons - Permission Sensitive Cache Servlet",
        description = "Servlet that checks if the current sessions has access to a cached object",
        metatype = true, immediate = true
)
@Properties({
        @Property(
                label = "File extensions to be removed from the URI",
                name = REMOVEABLE_EXTENSIONS,
                description = "Extensions that can be removed from the URI. For example, .html",
                cardinality = Integer.MAX_VALUE,
                value = {"html"}
        ),
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
    public static final String REMOVEABLE_EXTENSIONS = "removable-extensions";

    private List<String> removableExtensions = new ArrayList<String>();

    @Activate
    public final void activate(final Map<String, String> config) {

        String[] propertyArray = PropertiesUtil.toStringArray(config.get(REMOVEABLE_EXTENSIONS),
                new String[]{"html"});
        removableExtensions = Arrays.asList( propertyArray );
    }

    public void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        doHeadRequest(request, response);
    }

    public void doHeadRequest(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try{
            //retrieve the requested URL
            String requestUri = request.getParameter( "uri" );
            log.debug( "Request URI {}", requestUri );

            if(StringUtils.isNotEmpty( requestUri ) ){

                String uri = getUri( requestUri );

                log.info( "Checking permissions for {}", uri );

                Resource requestedResource = request.getResourceResolver().getResource( uri );

                if (requestedResource != null) {
                    log.debug("Current Session has access to {}", uri );
                    response.setStatus(SlingHttpServletResponse.SC_OK);
                } else {
                    log.debug("Current Session does not have access to {}", uri );
                    response.setStatus(SlingHttpServletResponse.SC_UNAUTHORIZED);
                }
            } else {
                response.setStatus(SlingHttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch(Exception e){
            log.error("authchecker servlet exception: " + e.getMessage());
        }
    }

    public String getUri(String requestUri){

        String extension = FilenameUtils.getExtension( requestUri );

        String uri = requestUri;

        if( removableExtensions.contains( extension ) ){
            uri = FilenameUtils.removeExtension( requestUri );
        }
        return uri;
    }

}
