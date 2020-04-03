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
package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.oak.EnsureOakIndexManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP POST /system/console/ensure-oak-index
 * Parameters
 * force = true | false (optional; when blank defaults to false)
 * path = /abs/path/to/ensure/definition (optional; when blank indicates all)
 */
//@formatter:off
@Component(immediate = true)
@Properties({
        @Property(
                name = "felix.webconsole.title",
                value = "Ensure Oak Index"
        ),
        @Property(
                name = "felix.webconsole.label",
                value = "ensure-oak-index"
        ),
        @Property(
                name = "felix.webconsole.category",
                value = "Sling"
        )
})
@Service(Servlet.class)
//@formatter:on
public class EnsureOakIndexServlet extends SlingAllMethodsServlet {
    //@formatter:off

    private static final Logger log = LoggerFactory.getLogger(EnsureOakIndexServlet.class);

    private static final String PARAM_FORCE = "force";
    private static final String PARAM_PATH = "path";

    @Reference
    private EnsureOakIndexManager ensureOakIndexManager;
    //@formatter:on

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response) {

        try {

            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.getWriter().println("<pre>");
            response.getWriter().println();
            response.getWriter().println();
            response.getWriter().println("HTTP method GET is not supported by this URL");
            response.getWriter().println("Use HTTP POST to access this end-point");
            response.getWriter().println("--------------------------------------------");
            response.getWriter().println("HTTP POST /system/console/ensure-oak-index");
            response.getWriter().println(" Parameters");
            response.getWriter().println("   * force = true | false (optional; when blank defaults to false)");
            response.getWriter().println("   * path = /abs/path/to/ensure/definition (optional; when blank indicates all)");
            response.getWriter().println();
            response.getWriter().println();
            response.getWriter().println("Example: curl --user admin:admin --data \"force=true\" https://localhost:4502/system/console/ensure-oak-index");
            response.getWriter().println("</pre>");

        } catch (IOException e) {
            log.warn("Caught IOException while handling doGet() in the Ensure Oak Index Servlet.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        String forceParam = StringUtils.defaultIfEmpty(request.getParameter(PARAM_FORCE), "false");
        boolean force = Boolean.parseBoolean(forceParam);

        String path = StringUtils.stripToNull(request.getParameter(PARAM_PATH));
        try {

            int count = 0;
            if (StringUtils.isBlank(path)) {
                count = ensureOakIndexManager.ensureAll(force);
            } else {
                count = ensureOakIndexManager.ensure(force, path);
            }

            response.setContentType("text/plain; charset=utf-8");
            response.getWriter().println("Initiated the ensuring of " + count + " oak indexes");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            log.warn("Caught IOException while handling doPost() in the Ensure Oak Index Servlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
