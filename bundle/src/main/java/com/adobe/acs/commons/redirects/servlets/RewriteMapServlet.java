/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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
package com.adobe.acs.commons.redirects.servlets;

import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import static com.adobe.acs.commons.redirects.models.RedirectRule.NOTE_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.RedirectRule.SOURCE_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.RedirectRule.STATUS_CODE_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.RedirectRule.TARGET_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.Redirects.readRedirects;
import static com.adobe.acs.commons.redirects.servlets.CreateRedirectConfigurationServlet.REDIRECTS_RESOURCE_PATH;


/**
 * Servlet for generating an Apache RewriteMap text file to use with
 * he Pipeline-free URL Redirects feature in AEM as a Cloud Service
 *
 * Usage: http://localhost:4502/conf/my-site/settings/redirects.txt
 * To filter by status code: http://localhost:4502/conf/my-site/settings/redirects.301.txt
 *
 * See https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/content-delivery/pipeline-free-url-redirects
 *
 */
@Component(service = Servlet.class, property = {
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=txt",
        "sling.servlet.resourceTypes=" + REDIRECTS_RESOURCE_PATH
})
public class RewriteMapServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -3564475196678277711L;

    @Override
    @SuppressWarnings("java:S3457")
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(ContentType.TEXT_PLAIN.getMimeType());

        String[] selectors = request.getRequestPathInfo().getSelectors();
        int statusCodeSelector = 0;
        if(selectors != null && selectors.length > 0) {
            statusCodeSelector = Integer.parseInt(selectors[0]);
        }
        Collection<Resource> rules = readRedirects(request.getResource());
        PrintWriter out = response.getWriter();
        out.printf("# %s Redirects\n", statusCodeSelector == 0 ? "All" : "" + statusCodeSelector);
        for (Resource resource : rules) {
            ValueMap props = resource.getValueMap();
            String source = props.get(SOURCE_PROPERTY_NAME, String.class);
            String target = props.get(TARGET_PROPERTY_NAME, String.class);
            int statusCode = props.get(STATUS_CODE_PROPERTY_NAME, Integer.class);
            String note = props.get(NOTE_PROPERTY_NAME, String.class);

            if(statusCodeSelector != 0 && statusCodeSelector != statusCode) {
                continue;
            }
            if(note != null && !note.isEmpty()) {
                out.printf("# %s\n", note);
            }
            out.printf("%s %s\n", source.trim(), target.trim());
        }
    }
}
