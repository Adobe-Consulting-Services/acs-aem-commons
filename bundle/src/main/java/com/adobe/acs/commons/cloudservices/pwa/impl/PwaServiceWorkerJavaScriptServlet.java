package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.adobe.acs.commons.cloudservices.pwa.Configuration;
import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
 
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
 
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
import java.util.Collection;
import java.util.Objects;
 
import static com.adobe.acs.commons.cloudservices.pwa.impl.Constants.AUTH_INFO;
import static com.adobe.acs.commons.cloudservices.pwa.impl.Constants.SERVICE_NAME;
 
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=cq:Page",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.selectors=pwa.service-worker",
                "sling.servlet.extensions=js",
        }
)
public class PwaServiceWorkerJavaScriptServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(PwaServiceWorkerJavaScriptServlet.class);

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    @Reference
    private ModelFactory modelFactory;

 
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
 
    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
            ServletException, IOException {

        response.setContentType("application/javascript");
        response.setHeader("Content-Disposition", "attachment");
        response.setCharacterEncoding("UTF-8");

 

        ResourceResolver serviceResourceResolver = null;
        try {
            serviceResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);
            writeJavaScript(new ServiceUserRequest(request, serviceResourceResolver), response);

        } catch (LoginException e) {
            log.error("Could not obtain service user [ {} ]", SERVICE_NAME, e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (serviceResourceResolver != null) {
                serviceResourceResolver.close();
            }
        }
    }

    private void writeJavaScript(SlingHttpServletRequest request, SlingHttpServletResponse response) {
 
        final Configuration configuration = modelFactory.createModel(request, Configuration.class);

        final Collection<ClientLibrary> htmlLibraries =
                htmlLibraryManager.getLibraries(configuration.getServiceWorkerJsCategories(),
                        LibraryType.JS, true, false);

        if (htmlLibraries.size() > 0) {
            htmlLibraries.stream()
                    .map(hl -> htmlLibraryManager.getLibrary(LibraryType.JS, hl.getPath()))
                    .filter(Objects::nonNull)
                    .forEach(library -> {
                        try {
                            response.getWriter().write(IOUtils.toString(library.getInputStream()));
                            response.flushBuffer();
                        } catch (IOException e) {
                            log.error("Error streaming JS Client Library at [ {} ] to response for PWA Service Worker JS request.", library.getPath());
                        }
                    });

        } else {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
        }
    }
}