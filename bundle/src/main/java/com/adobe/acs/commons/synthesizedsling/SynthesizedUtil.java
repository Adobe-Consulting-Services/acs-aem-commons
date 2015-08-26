package com.adobe.acs.commons.synthesizedsling;

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

public final class SynthesizedUtil {

    private SynthesizedUtil() {}

    public static Resource buildSynthesizedResource(ResourceResolver resourceResolver, String path,
                                                    String resourceType, Map<String, Object> properties) {

        SynthesizedResource synthesizedResource = new SynthesizedResource(resourceResolver, path, resourceType);
        synthesizedResource.setValueMap(properties);

        return synthesizedResource;
    }

    public static String renderSynthesized(String path, String resourceType, Map<String, Object> properties,
                                           SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        Resource synthesizedResource = buildSynthesizedResource(request.getResourceResolver(), path, resourceType, properties);

        return renderResource(synthesizedResource, SynthesizedSlingHttpServletRequest.METHOD_GET, "html", request, response);
    }



    public static String renderResource(Resource resource, String requestMethod, String requestExtension,
                                        SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        SynthesizedSlingHttpServletRequest synthesizedRequest = new SynthesizedSlingHttpServletRequest(request)
                .setMethod(requestMethod)
                .setExtension(requestExtension)
                .setResource(resource);

        return renderResource(resource, new RequestDispatcherOptions(), synthesizedRequest, response);
    }

    public static String renderResource(Resource resource, RequestDispatcherOptions requestDispatcherOptions,
                                        SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        // @ROCK-SOLID MAGIC .. remove the decoration <div> around the component by setting it to "" empty string
        return renderResource(resource, requestDispatcherOptions, "", request, response);
    }

    public static String renderResource(Resource resource, RequestDispatcherOptions requestDispatcherOptions,
                                        String decorationTagName, SlingHttpServletRequest request,
                                        SlingHttpServletResponse response)
            throws ServletException, IOException {

        RequestDispatcher requestDispatcher = request.getRequestDispatcher(resource, requestDispatcherOptions);

        IncludeOptions.getOptions(request, true).setDecorationTagName(decorationTagName);

        StringResponseWrapper responseWrapper = new StringResponseWrapper(response);
        requestDispatcher.include(request, responseWrapper);

        return responseWrapper.getString();
    }

}
