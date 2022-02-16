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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP POST /system/console/ensure-oak-index
 * Parameters
 * force = true | false (optional; when blank defaults to false)
 * path = /abs/path/to/ensure/definition (optional; when blank indicates all)
 */
//@formatter:off
@Component(
        service = Servlet.class,
        property = {
                "felix.webconsole.title=Ensure Oak Index (ACS AEM Commons)",
                "felix.webconsole.label=ensure-oak-index",
                "felix.webconsole.category=Sling"
        }
)
//@formatter:on
public class EnsureOakIndexServlet extends HttpServlet {
    //@formatter:off
    private static final Logger log = LoggerFactory.getLogger(EnsureOakIndexServlet.class);

    private static final String PARAM_FORCE = "force";
    private static final String PARAM_PATH = "path";

    @SuppressWarnings("squid:S2226")
    @Reference
    private transient EnsureOakIndexManager ensureOakIndexManager;
    //@formatter:on

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {

        try {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.getWriter().println(IOUtils.toString(this.getClass().getResourceAsStream("EnsureOakIndexServlet_doGet.html"), "UTF-8"));


        } catch (IOException e) {
            log.warn("Caught IOException while handling doGet() in the Ensure Oak Index Servlet.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String forceParam = StringUtils.defaultIfEmpty(request.getParameter(PARAM_FORCE), "false");
        boolean force = Boolean.parseBoolean(forceParam);

        String path = StringUtils.stripToNull(request.getParameter(PARAM_PATH));
        try {

            int count;
            String message;
            if (StringUtils.isBlank(path)) {
                count = ensureOakIndexManager.ensureAll(force);
                message = String.format("Initiated the FORCE ensuring of [ %d ] all oak index ensure definitions", count);
            } else {
                count = ensureOakIndexManager.ensure(force, path);
                message = String.format("Initiated the [ %s ] ensuring of [ %d ] oak index ensure definitions at path [ %s ]",
                        force ? "FORCE" : "UN-FORCED",
                        count,
                        path);
            }

            response.setContentType("text/plain; charset=utf-8");
            response.getWriter().println(message);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            log.warn("Caught IOException while handling doPost() in the Ensure Oak Index Servlet", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
