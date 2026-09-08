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

package com.adobe.acs.commons.wcm.impl;

import com.day.cq.commons.Externalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * This servlet generates a JSON response with the externalized URLs for the given path using configured keys in its
 * configuration. They keys need to match the environment keys configured in the Externalizer configuration. The servlet
 * uses Externalizer service to generate the externalized URLs via Externalizer.externalLink() method. The response is
 * in format {"Author": "Author URL", "Publish": "Publish URL", ...}.
 */
@Component(service = Servlet.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@SlingServletResourceTypes(
        resourceTypes = PublishUrlServlet.RESOURCE_TYPE,
        methods = HttpConstants.METHOD_GET,
        extensions = PublishUrlServlet.JSON_EXTENSION
)
@Designate(ocd = PublishUrlServlet.PublishUrlServletConfig.class)
public class PublishUrlServlet extends SlingSafeMethodsServlet implements Serializable {

    private static final long serialVersionUID = 1L;
    protected static final String RESOURCE_TYPE = "acs-commons/components/utilities/publish-url";
    protected static final String JSON_EXTENSION = "json";
    private static final String PATH = "path";
    private static final String JSON_TYPE = "application/json";
    private String[] externalizerKeys;

    @Activate
    protected void activate(final PublishUrlServletConfig config) {
        this.externalizerKeys = config.externalizerKeys();
    }

    /**
     * Gets the path parameter from the request and generates the externalized URLs for the given path using the
     * Externalizer service. Writes the JSON response with the externalized URLs to the response.
     *
     * @param request  SlingHttpServletRequest
     * @param response SlingHttpServletResponse
     * @throws IOException if response.getWriter() fails
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException {
        String path = request.getParameter(PATH);
        ResourceResolver resolver = request.getResourceResolver();
        Externalizer externalizer = resolver.adaptTo(Externalizer.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonResponse = mapper.createObjectNode();

        if (externalizer != null) {
            Arrays.asList(externalizerKeys).forEach(key -> {
                String capitalizedKey = StringUtils.capitalize(key);
                String externalLink = externalizer.externalLink(resolver, key, request.getScheme(), path);
                jsonResponse.put(capitalizedKey, externalLink);
            });
        }

        response.setContentType(JSON_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonResponse.toString());
    }

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Publish URL Servlet",
            description = "Servlet that accepts a GET request with a resource path in the path parameter, and returns a JSON object of the externalized URLs for the path."
    )
    public @interface PublishUrlServletConfig {
        @AttributeDefinition(
                name = "Externalizer environment keys",
                description = "Keys must match the environment keys configured in the AEM Externalizer OSGi configuration.",
                type = AttributeType.STRING
        )
        String[] externalizerKeys() default {};
    }
}
