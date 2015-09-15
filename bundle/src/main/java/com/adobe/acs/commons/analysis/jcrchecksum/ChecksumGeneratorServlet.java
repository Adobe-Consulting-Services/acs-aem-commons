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

package com.adobe.acs.commons.analysis.jcrchecksum;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeSet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@Component(
        label = "ACS AEM Commons - JCR Checksum Servlet",
        metatype = false
)
@Properties({
        @Property(
                name = "sling.servlet.paths",
                value = "/bin/acs-commons/jcr-compare.hashes.txt",
                propertyPrivate = true)
})
@Service
public class ChecksumGeneratorServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(ChecksumGeneratorServlet.class);

    private static final String DEFAULT_ALLOW_ORIGIN = "*";

    private String allowOrigin = DEFAULT_ALLOW_ORIGIN;

    @Property(label = "Access-Control-Allow-Origin response header value",
            description = "Set to the hostname(s) of the AEM Author environment",
            value = DEFAULT_ALLOW_ORIGIN)
    public static final String PROP_ALLOW_ORIGIN = "access-control-allow-origin";


    @Override
    public void doGet(SlingHttpServletRequest request,
                      SlingHttpServletResponse response) {

        response.setContentType("text/plain");

        if (StringUtils.isNotBlank(this.allowOrigin)) {
            response.setHeader("Access-Control-Allow-Origin", this.allowOrigin);
        }
        
        try {
            ServletInputProcessor sip = new ServletInputProcessor(request, response);
            handleRequest(request, response, sip);
        } catch (IOException e) {
            log.error("Unable to handle Checksum request", e);
        } catch (ServletException e) {
            log.error("Unable to handle Checksum request", e);
        }

    }

    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException {
        response.setContentType("text/plain");

        ServletInputProcessor sip;
        try {
            sip = new ServletInputProcessor(request, response);
            handleRequest(request, response, sip);
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

    private void handleRequest(SlingHttpServletRequest request,
        SlingHttpServletResponse response, ServletInputProcessor sip)
        throws IOException {



        PrintWriter out = response.getWriter();
        Session session = request.getResourceResolver().adaptTo(Session.class);

        TreeSet<String> paths = sip.getPaths();
        if (paths != null) {
            for (String path : paths) {
                if (path == null)
                    continue;

                try {
                    ChecksumGenerator.generateChecksums(session, path,
                        sip.getChecksumGeneratorOptions(), out);
                } catch (RepositoryException e) {
                    // do nothing as errors should have been printed to output
                }
            }
        }
    }

    @Activate
    protected final void activate(Map<String, Object> config) {
        this.allowOrigin =
                PropertiesUtil.toString(config.get(PROP_ALLOW_ORIGIN), DEFAULT_ALLOW_ORIGIN);
    }
}