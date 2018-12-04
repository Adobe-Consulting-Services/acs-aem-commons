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

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.ChecksumGeneratorOptionsFactory;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.RequestChecksumGeneratorOptions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
@Component(property= {
        "sling.servlet.paths="+ChecksumGeneratorServlet.SERVLET_PATH,
        "sling.auth.requirements=-"+ChecksumGeneratorServlet.SERVLET_PATH
})

public class ChecksumGeneratorServlet extends BaseChecksumServlet {
    public static final Logger log = LoggerFactory.getLogger(ChecksumGeneratorServlet.class);

    @Reference
    private ChecksumGenerator checksumGenerator;

    public static final String SERVLET_PATH =  ServletConstants.SERVLET_PATH  + "."
            + ServletConstants.CHECKSUM_SERVLET_SELECTOR + "."
            + ServletConstants.CHECKSUM_SERVLET_EXTENSION;

    @Override
    public final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
            ServletException {
        try {
            this.handleCORS(request, response);
            this.handleRequest(request, response);
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    public final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
            ServletException {
        try {
            this.handleCORS(request, response);
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
        response.setCharacterEncoding("UTF-8");

        String optionsName = request.getParameter(ServletConstants.OPTIONS_NAME);
        ChecksumGeneratorOptions options = ChecksumGeneratorOptionsFactory.getOptions(request, optionsName);

        if (log.isDebugEnabled()) {
            log.debug(options.toString());
        }

        Set<String> paths = RequestChecksumGeneratorOptions.getPaths(request);

        if (CollectionUtils.isEmpty(paths)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("ERROR: At least one path must be specified");
        }

        final Session session = request.getResourceResolver().adaptTo(Session.class);

        for (final String path : paths) {
            log.debug("Generating checksum for path [ {} ]", path);

            Map<String, String> checksums = checksumGenerator.generateChecksums(session, path, options);

            log.debug("Collected [ {} ] checksum entries under [ {} ]", checksums.size(), path);

            for(final Map.Entry<String, String> entry : checksums.entrySet()) {
                log.trace("Checksum [ {} ~> {} ]", entry.getKey(), entry.getValue());
                response.getWriter().println(entry.getKey() + "\t" + entry.getValue());
            }
        }
    }
}