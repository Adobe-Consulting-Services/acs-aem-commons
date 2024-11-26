/*
 * ACS AEM Commons Bundle
 *
 * Copyright (C) 2024 Adobe
 *
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
 */

package com.adobe.acs.commons.wcm.impl;

import com.day.cq.commons.Externalizer;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.servlet.Servlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.apache.tika.mime.MediaType;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = PublishUrlServlet.RESOURCE_TYPE,
        methods = HttpConstants.METHOD_GET,
        extensions = PublishUrlServlet.TXT_EXTENSION
)
@Designate(ocd = PublishUrlServlet.PublishUrlServletConfig.class)
public class PublishUrlServlet extends SlingSafeMethodsServlet implements Serializable {

    private static final long serialVersionUID = 1L;
    protected static final String RESOURCE_TYPE = "acs-commons/components/utilities/publish-url";
    public static final String TXT_EXTENSION = "txt";
    public static final String PATH = "path";
    private String[] externalizerKeys;

    @Activate
    protected void activate(final PublishUrlServletConfig config) {
        this.externalizerKeys = config.externalizerKeys();
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
        throws IOException {
        String path = request.getParameter(PATH);
        StringBuilder builder = new StringBuilder();
        ResourceResolver resolver = request.getResourceResolver();
        Externalizer externalizer = resolver.adaptTo(Externalizer.class);

        if (externalizer != null) {
            Arrays.asList(externalizerKeys)
                .forEach(key -> builder.append(StringUtils.capitalize(key))
                    .append(" : ")
                    .append(externalizer.externalLink(resolver, key, request.getScheme(), path))
                    .append("\n"));
        }

        response.setContentType(MediaType.TEXT_PLAIN.getType());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(builder.toString());
    }

    @ObjectClassDefinition(
        name = "ACS AEM Commons - Publish URL Servlet Configuration",
        description = "Configuration for the Publish URL Servlet"
    )
    public @interface PublishUrlServletConfig {
        @AttributeDefinition(
            name = "Externalizer Environment Keys",
            description = "Externalizer Environment Keys. They need to match the environment keys configured in the Externalizer configuration.",
            type = AttributeType.STRING
        )
        String[] externalizerKeys() default {};
    }
}
