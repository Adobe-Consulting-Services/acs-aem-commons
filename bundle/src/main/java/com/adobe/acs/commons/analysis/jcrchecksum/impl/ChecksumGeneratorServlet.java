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

package com.adobe.acs.commons.analysis.jcrchecksum.impl;

import java.io.IOException;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.commons.collections.CollectionUtils;
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

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.ChecksumGeneratorOptionsFactory;

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
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException {
        try {
            this.handleRequest(request, response);
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException {
        try {
            this.handleRequest(request, response);
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    private void handleRequest(SlingHttpServletRequest request,
                               SlingHttpServletResponse response) throws IOException, RepositoryException {

        response.setContentType("text/plain");

        if (StringUtils.isNotBlank(this.allowOrigin)) {
            response.setHeader("Access-Control-Allow-Origin", this.allowOrigin);
        }

        String optionsName = request.getParameter("optionsName");
        ChecksumGeneratorOptions options = ChecksumGeneratorOptionsFactory.getOptions(request, optionsName);

        log.debug(options.toString());

        if (CollectionUtils.isEmpty(options.getPaths())) {
            try {
                response.setStatus(400);
                response.getWriter().print("ERROR: At least one path must be specified");
            } catch (IOException ioe) {
               throw ioe;
            }
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);

        for (final String path : options.getPaths()) {
            log.debug("Generating checksum for path [ {} ]", path);
            ChecksumGenerator.generateChecksums(session, path, options, response.getWriter());
        }
    }

    @Activate
    protected final void activate(Map<String, Object> config) {
        this.allowOrigin =
                PropertiesUtil.toString(config.get(PROP_ALLOW_ORIGIN), DEFAULT_ALLOW_ORIGIN);
    }
}