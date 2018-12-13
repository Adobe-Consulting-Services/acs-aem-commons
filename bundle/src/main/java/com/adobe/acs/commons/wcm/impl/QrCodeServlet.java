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
package com.adobe.acs.commons.wcm.impl;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.Externalizer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component(service = Servlet.class,
factory = "com.adobe.acs.commons.wcm.impl.SiteMapServlet", 
configurationPolicy = ConfigurationPolicy.REQUIRE,
property = 
{ SLING_SERVLET_RESOURCE_TYPES + "=acs-commons/components/utilities/qr-code/config",
  SLING_SERVLET_EXTENSIONS + "=json", 
  SLING_SERVLET_METHODS + "=GET"})
public class QrCodeServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(QrCodeServlet.class);

    private static final String PN_ENABLED = "enabled";
    private static final String JSON_KEY_ENABLED = "enabled";
    private static final String JSON_KEY_PUBLISH_URL = "publishURL";

    @Reference
    private Externalizer externalizer;

    @Override
    protected final void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws
            ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        if (externalizer == null) {
            log.warn("Externalizer is not configured. This is required for QR Code servlet to work.");
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        } else if (request.getResource().getValueMap().get(PN_ENABLED, false)) {
            final JsonObject json = new JsonObject();
            final String publishUrl = externalizer.publishLink(request.getResourceResolver(), request.getRequestPathInfo().getSuffix());

            log.debug("Externalized path [ {} ] for QR Code generation to [ {} ]",
                    request.getRequestPathInfo().getSuffix(),
                    publishUrl);

            if (StringUtils.isNotBlank(publishUrl)) {
                json.addProperty(JSON_KEY_ENABLED, true);
                json.addProperty(JSON_KEY_PUBLISH_URL, publishUrl);
                Gson gson = new Gson();
                gson.toJson(json, response.getWriter());
                response.getWriter().flush();
            } else {
                log.warn("Externalizer configuration for AEM Publish did not yield a valid URL");
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.warn("Externalizer configuration for AEM Publish did not yield a valid URL");
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
