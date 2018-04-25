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

import com.day.cq.commons.Externalizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;

@Component(
        label = "ACS AEM Commons - QR Code Configuration Servlet",
        policy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
@Properties({
        @Property(
                name = "sling.servlet.methods",
                value = "GET",
                propertyPrivate = true
        ),
        @Property(
                name = "sling.servlet.resourceTypes",
                value = "acs-commons/components/utilities/qr-code/config",
                propertyPrivate = true
        ),
        @Property(
                name = "sling.servlet.extensions",
                value = "json",
                propertyPrivate = true
        )
})
@Service
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
            final JSONObject json = new JSONObject();
            final String publishUrl = externalizer.publishLink(request.getResourceResolver(), request.getRequestPathInfo().getSuffix());

            log.debug("Externalized path [ {} ] for QR Code generation to [ {} ]",
                    request.getRequestPathInfo().getSuffix(),
                    publishUrl);

            if (StringUtils.isNotBlank(publishUrl)) {
                try {
                    json.put(JSON_KEY_ENABLED, true);
                    json.put(JSON_KEY_PUBLISH_URL, publishUrl);
                } catch (JSONException e) {
                    log.error("Could not construct the QR Code Servlet JSON response", e);
                    throw new ServletException(e);
                }

                response.getWriter().write(json.toString());
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