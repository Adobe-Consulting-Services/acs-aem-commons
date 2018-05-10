package com.adobe.acs.commons.wcm.pwa.impl;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;

@SlingServlet(
        methods = {"GET"},
        resourceTypes = {"cq/Page"},
        selectors = {"pwa"},
        extensions = {"load"}
)
public class PWAProxyServlet extends SlingSafeMethodsServlet implements OptingServlet {
    private static final Logger log = LoggerFactory.getLogger(PWAProxyServlet.class);

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
            ServletException, IOException {

        RequestPathInfo requestPathInfo = request.getRequestPathInfo();

        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());

        Resource proxyResource = null;

        if ("/manifest.json".equalsIgnoreCase(requestPathInfo.getSuffix())) {
            proxyResource = request.getResourceResolver().getResource(handleManifest(page, response));
        } else if ("/service-worker.js".equalsIgnoreCase(requestPathInfo.getSuffix())) {
            proxyResource = request.getResourceResolver().getResource(handleServiceWorker(page, response));

        }
        if (proxyResource != null) {
            final InputStream in = proxyResource.adaptTo(InputStream.class);

            if (in != null) {
                IOUtils.copy(in, response.getOutputStream());
                response.flushBuffer();
                return;
            }
        }

        response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
    }


    /**
     * Example: HTTP GET http://localhost:4502/content/we-retail/us/en.pwa.load/manifest.json
     *
     * End-point for loading the PWA manifest.json in the context providing scope.
     *
     * This would load a JSON file available internally at some path specified at:
     *
     *  [cq:Page]/jcr:content/pwa/manifestPath = /apps/some/path/in/jcr/manifest.json
     *
     * This could let different PWA's on an AEM instance have different manifest implementations.
     *
     * TODO: Ideally this JSON could be built from edittable page properties instead of a file.
     *
     * @param page
     * @param response
     * @return
     */
    private String handleManifest(Page page, SlingHttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        return page.getContentResource().getValueMap().get("pwa/manifestPath",
                "/apps/acs-commons/pwa/manifest.json");
    }

    /**
     * End-point for loading the PWA service-worker.js in the context providing scope.
     *
     * Example: HTTP GET http://localhost:4502/content/we-retail/us/en.pwa.load/service-worker.js
     *
     * This would load a JS file available internally at some path specified at:
     *
     *  [cq:Page]/jcr:content/pwa/serviceWorkerJSPath = /apps/some/path/in/jcr/service-worker.js
     *
     * This could let different PWA's on an AEM instance have different service worker implementations.
     *
     * @param page
     * @param response
     * @return
     */
    private String handleServiceWorker(Page page, SlingHttpServletResponse response) {
        response.setContentType("application/javascript");
        response.setCharacterEncoding("UTF-8");

        return page.getContentResource().getValueMap().get("pwa/serviceWorkerJSPath",
                "/apps/acs-commons/pwa/service-worker.js");
    }

    /**
     * OptingServlet Acceptance Method
     **/

    /**
     * This should ONLY accept requests that have a [cq:Page]/jcr:content/pwa/enabled = true
     * This could be set at the PWA site root.
     *
     * @param request
     * @return
     */
    @Override
    public final boolean accepts(SlingHttpServletRequest request) {
        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());

        if (page != null) {
            // Only accept for
            return page.getContentResource().getValueMap().get("pwa/enabled", true);
        }

        return false;
    }
}