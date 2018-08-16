package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.adobe.acs.commons.cloudservices.pwa.Configuration;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.mime.MimeTypeService;
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
                "sling.servlet.selectors=pwa.manifest",
                "sling.servlet.extensions=json",
                "sling.servlet.extensions=webmanifest"

        }
)
public class PwaManifestServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(PwaManifestServlet.class);

    @Reference
    private MimeTypeService mimeTypeService;

    @Reference
    private ModelFactory modelFactory;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/x-web-app-manifest+json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        ResourceResolver serviceResourceResolver = null;
        try {
            serviceResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);

            response.getWriter().write(getManifest(new ServiceUserRequest(request, serviceResourceResolver)).toString());
        } catch (LoginException e) {
            log.error("Could not obtain service user [ {} ]", SERVICE_NAME, e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (serviceResourceResolver != null) {
                serviceResourceResolver.close();
            }
        }
    }

    private JsonObject getManifest(SlingHttpServletRequest request) throws ServletException {
        final Configuration configuration = modelFactory.createModel(request, Configuration.class);
        final ResourceResolver resourceResolver = request.getResourceResolver();
        final ValueMap manifest = configuration.getProperties();
        final JsonObject json = new JsonObject();

        /**
         * // TODO collect?
         * description: "...",
         * dir: "ltr",
         */
        final String scope = configuration.getScopePath();

        json.addProperty(KEY_NAME,
                manifest.get(PN_NAME,
                         "AEM Progressive Web App"));

        json.addProperty(KEY_SHORT_NAME,
                manifest.get(PN_SHORT_NAME, "AEM PWA"));


        json.addProperty(KEY_THEME_COLOR,
                manifest.get(PN_THEME_COLOR, "#000000"));

        json.addProperty(KEY_BACKGROUND_COLOR,
                manifest.get(PN_BACKGROUND_COLOR,
                        "#ffffff"));

        json.addProperty(KEY_DISPLAY,
                manifest.get(PN_DISPLAY, "standalone"));

        json.addProperty(KEY_LANGUAGE,
                configuration.getConfPage().getLanguage(false).getVariant());


        json.addProperty(KEY_SCOPE,".");

        String startPath = manifest.get(PN_START_PATH, configuration.getScopePath());
        if (!StringUtils.equals(scope, startPath) && !StringUtils.startsWith(startPath, scope + "/")) {
            startPath = scope;
        }

        json.addProperty(KEY_START_URL, resourceResolver.map(request, addHtmlExtension(startPath)));

        json.add(KEY_ICONS, getIcons(configuration));

        return json;
    }

    private JsonArray getIcons(final Configuration configuration) {
        final JsonArray icons = new JsonArray();

        final Resource iconsResource = configuration.getConfPage().getContentResource(NN_ICONS);

        if (iconsResource != null) {
            StreamSupport.stream(iconsResource.getChildren().spliterator(), false)
                    .map(Resource::getValueMap)
                    .filter(p -> p.get(PN_ICON_PATH, String.class) != null)
                    .forEach(p -> {
                        final JsonObject json = new JsonObject();
                        final String path = p.get(PN_ICON_PATH, String.class);

                        json.addProperty(KEY_ICON_SRC, path);
                        json.addProperty(KEY_ICON_SIZE, p.get(PN_ICON_SIZE, String.class));
                        json.addProperty(KEY_ICON_TYPE, mimeTypeService.getMimeType(path));

                        icons.add(json);
                    });
        }
        return icons;
    }

    private String addHtmlExtension(final String path) {
        String tmp = StringUtils.removeEnd(path, HTML_EXTENSION);
        if ("/".equals(tmp)) {
            return tmp;
        } else {
            return tmp + HTML_EXTENSION;
        }
    }
}