package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.adobe.acs.commons.cloudservices.pwa.Configuration;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.stream.StreamSupport;

import static com.adobe.acs.commons.cloudservices.pwa.impl.Constants.*;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=cq:Page",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.selectors=pwa.service-worker",
                "sling.servlet.extensions=json"
        }
)
public class PwaServiceWorkerConfigServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(PwaServiceWorkerConfigServlet.class);

    private static final JsonElement NO_CACHE_CSRF = getNoCacheCSRF();

    @Reference
    private ModelFactory modelFactory;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        ResourceResolver serviceResourceResolver = null;
        try {
            serviceResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);

            response.getWriter().write(getConfig(new ServiceUserRequest(request, serviceResourceResolver)).toString());
        } catch (LoginException e) {
            log.error("Could not obtain service user [ {} ]", SERVICE_NAME, e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (serviceResourceResolver != null) {
                serviceResourceResolver.close();
            }
        }
    }

    private JsonObject getConfig(SlingHttpServletRequest request) throws ServletException {
        final Configuration configuration = modelFactory.createModel(request, Configuration.class);
        final ValueMap properties = configuration.getProperties();
        final JsonObject json = new JsonObject();
        final int version = properties.get(PN_VERSION, 1);

        json.addProperty(KEY_CACHE_NAME,
                "pwa__"
                        + properties.get(PN_SHORT_NAME, configuration.getConfPage().getName())
                        + "-v" + String.valueOf(version));

        json.addProperty(KEY_VERSION, version);

        json.add(KEY_FALLBACK, getFallback(request, configuration));
        json.add(KEY_NO_CACHE, getNoCache(configuration));
        json.add(KEY_PRE_CACHE, getPreCache(request, configuration));

        return json;
    }

    private JsonArray getFallback(final SlingHttpServletRequest request, final Configuration configuration) {
        final JsonArray jsons = new JsonArray();
        final Resource resource = configuration.getConfPage().getContentResource(NN_FALLBACK);

        if (resource != null) {
            StreamSupport.stream(resource.getChildren().spliterator(), false)
                    .map(Resource::getValueMap)
                    .filter(p -> p.get(PN_PATTERN, String.class) != null)
                    .filter(p -> p.get(PN_PATH, String.class) != null)
                    .forEach(p -> {
                        final JsonObject json = new JsonObject();

                        json.addProperty(KEY_PATTERN, p.get(PN_PATTERN, String.class));
                        // TODO handle multiple?
                        json.addProperty(KEY_PATH, request.getResourceResolver().map(request, p.get(PN_PATH, String.class)));

                        jsons.add(json);
                    });

        }
        return jsons;
    }

    private JsonArray getNoCache(final Configuration configuration) {
        final JsonArray jsons = new JsonArray();
        final Resource resource = configuration.getConfPage().getContentResource(NN_NO_CACHE);

        if (resource != null) {
            StreamSupport.stream(resource.getChildren().spliterator(), false)
                    .map(Resource::getValueMap)
                    .filter(p -> p.get(PN_PATTERN, String.class) != null)
                    .forEach(p -> {
                        jsons.add(new JsonPrimitive(p.get(PN_PATTERN, String.class)));
                    });

        }

        jsons.add(NO_CACHE_CSRF);

        return jsons;
    }

    private JsonArray getPreCache(final SlingHttpServletRequest request, final Configuration configuration) {
        final JsonArray jsons = new JsonArray();
        final Resource resource = configuration.getConfPage().getContentResource(NN_PRE_CACHE);

        if (resource != null) {
            StreamSupport.stream(resource.getChildren().spliterator(), false)
                    .map(Resource::getValueMap)
                    .filter(p -> p.get(PN_PATH, String.class) != null)
                    .forEach(p -> {
                        jsons.add(
                                new JsonPrimitive(
                                        request.getResourceResolver().map(request, p.get(PN_PATH, String.class))));
                    });

        }
        return jsons;
    }

    private static JsonElement getNoCacheCSRF() {
        return new JsonPrimitive("(.*)/libs/granite/csrf/token.json(\\?.*)?");
    }
}