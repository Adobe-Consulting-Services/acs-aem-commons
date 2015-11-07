/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.synth;

import com.adobe.acs.commons.synth.impl.SynthesizedSlingHttpServletRequest;
import com.adobe.acs.commons.synth.impl.SynthesizedResource;
import com.day.cq.commons.feed.StringResponseWrapper;
import com.day.cq.wcm.api.components.IncludeOptions;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

public final class Synthesizer {

    private static final String SOME_PATH = "/var/acs-commons/synthesized/%s";

    private Synthesizer() {
    }

    /**
     * Builds a synthesized resource intended for "one-off usage".
     *
     * @param resourceResolver ResourceResolver
     * @param resourceType Sling resource type, e.g. "myapp/components/my-fancy-show-off-stuff"
     * @param properties Properties of the synthesized resource
     * @return A resource with the given resourceType and properties
     */
    public static Resource buildResource(ResourceResolver resourceResolver, String resourceType,
                                         Map<String, Object> properties) {

        SynthesizedResource synthesizedResource = new SynthesizedResource(resourceResolver, SOME_PATH, resourceType,
                properties);

        return synthesizedResource;
    }

    /**
     * Renders a "one-off" resource with the given resourceType and properties.
     *
     * @param resourceType Sling resource type. This is the Sling script that will used for rendering.
     * @param properties Properties of the "one-off" resource
     * @param request Original request used for dispatching
     * @param response Original response used for dispatching
     * @return HTML result of rendering the script
     * @throws ServletException
     * @throws IOException
     */
    public static String render(String resourceType, Map<String, Object> properties,
                                SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        Resource synthesizedResource = buildResource(request.getResourceResolver(), resourceType, properties);

        return doRender(synthesizedResource, SynthesizedSlingHttpServletRequest.METHOD_GET, "html", request, response);
    }

    /**
     * Renders "GET {resource}.html"
     *
     * @param resource Resource
     * @param request Original request used for dispatching
     * @param response Original response used for dispatching
     * @return HTML result of rendering the resource
     * @throws ServletException
     * @throws IOException
     */
    public static String render(Resource resource, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        return doRender(resource, SynthesizedSlingHttpServletRequest.METHOD_GET, "html", request, response);
    }

    private static String doRender(Resource resource, String requestMethod, String requestExtension,
                                   SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        SynthesizedSlingHttpServletRequest synthesizedRequest = new SynthesizedSlingHttpServletRequest(request)
                .setMethod(requestMethod)
                .setExtension(requestExtension)
                .setResource(resource);

        RequestDispatcherOptions rdo = new RequestDispatcherOptions();
        IncludeOptions.getOptions(synthesizedRequest, true).setDecorationTagName(""); // @ROCK SOLID MAGIC .. remove decoration <div> by setting it to "" empty string

        StringResponseWrapper responseWrapper = new StringResponseWrapper(response);

        RequestDispatcher requestDispatcher = request.getRequestDispatcher(resource, rdo);
        requestDispatcher.include(synthesizedRequest, responseWrapper);

        return responseWrapper.getString();
    }

}
