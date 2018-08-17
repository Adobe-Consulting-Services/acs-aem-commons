package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.adobe.granite.confmgr.Conf;
import com.adobe.granite.confmgr.ConfMgr;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
/*
@Component(
        immediate = true,
        service = Servlet.class,
        property = {
                "sling.servlet.extensions=load",
                "sling.servlet.extensions=js",
                "sling.servlet.selectors=pwa",
                "sling.servlet.selectors=manifest",
                "sling.servlet.selectors=service-worker",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.resourceTypes=cq:Page"
        }
)*/
public class PWAProxyServlet extends SlingSafeMethodsServlet implements OptingServlet {
    private static final Logger log = LoggerFactory.getLogger(PWAProxyServlet.class);
    @Reference
    ConfMgr confMgr;
    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
            ServletException, IOException {

        RequestPathInfo requestPathInfo = request.getRequestPathInfo();

        boolean swRequest = false;

        String[] selectors = requestPathInfo.getSelectors();
        for (String selector : selectors) {
            if ("service-worker".equals(selector)) {
                swRequest = true;
            }
            if ("manifest".equals(selector)) {
                swRequest = true;
            }

        }
        if ("/".equalsIgnoreCase(requestPathInfo.getSuffix()) && "load".equalsIgnoreCase(requestPathInfo.getExtension())) {
            request.getRequestDispatcher(requestPathInfo.getResourcePath() + ".html").forward(request, response);
        }
        Resource proxyResource = null;
        if ("/manifest.json".equalsIgnoreCase(requestPathInfo.getSuffix())) {
            response.getWriter().write(handleManifest(request, response));

        } else if ("/cacheDetails.json".equalsIgnoreCase(requestPathInfo.getSuffix())) {
            response.getWriter().write(handleCacheDetails(request, response));
        } else if (swRequest) {
            proxyResource = request.getResourceResolver().getResource(handleServiceWorker(response));

            if (proxyResource != null) {
                final InputStream in = proxyResource.adaptTo(InputStream.class);

                if (in != null) {
                    IOUtils.copy(in, response.getOutputStream());
                    response.flushBuffer();
                    return;
                }
            }
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
        } else if ("/root-service-worker.json".equalsIgnoreCase(requestPathInfo.getSuffix())) {
            response.getWriter().write(handleRootServiceWorker(request, response));
        }

    }

    private String handleCacheDetails(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        JSONObject jsonObject = new JSONObject();

        ValueMap manifestSettings = getConfigProperties(request);
        if (manifestSettings != null) {
            try {
                jsonObject.put("fallback", manifestSettings.get("fallbackUrl", "/"));
                jsonObject.put("deniedHTMLCache", getCachePaths(manifestSettings, "deniedHTMLCache"));
                jsonObject.put("deniedAssetsCache", getCachePaths(manifestSettings, "deniedAssetsCache"));
                jsonObject.put("staticHTMLCache", getCachePaths(manifestSettings, "staticHTMLCache"));
                jsonObject.put("staticAssetsCache", getCachePaths(manifestSettings, "staticAssetsCache"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return jsonObject.toString();
    }

    private JSONArray getCachePaths(ValueMap manifestSettings, String cachePaths) {
        String[] paths = manifestSettings.get(cachePaths, new String[0]);
        JSONArray jsonArray = new JSONArray();
        for (String path : paths) {
            if ("deniedHTMLCache".equals(cachePaths) || "staticHTMLCache".equals(cachePaths)) {
                path += ".html";
            }
            jsonArray.put(path);
        }
        return jsonArray;
    }


    private String handleServiceWorker(SlingHttpServletResponse response) {
        response.setContentType("application/javascript");
        response.setHeader("Content-Disposition", "attachment");
        response.setCharacterEncoding("UTF-8");
        return "/etc/clientlibs/acs-commons/clientlib-sw/service-worker.js";
    }


    /**
     * Example: HTTP GET http://localhost:4502/content/we-retail/us/en.pwa.load/manifest.json
     * <p>
     * End-point for loading the PWA manifest.json in the context providing scope.
     * <p>
     * This would load a JSON file available internally at some path specified at:
     * <p>
     * [cq:Page]/jcr:content/pwa/manifestPath = /apps/some/path/in/jcr/manifest.json
     * <p>
     * This could let different PWA's on an AEM instance have different manifest implementations.
     * <p>
     * TODO: Ideally this JSON could be built from edittable page properties instead of a file.
     *
     * @param request
     * @param response
     * @return
     */
    private String handleManifest(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        JSONObject jsonObject = new JSONObject();

        ValueMap manifestSettings = getConfigProperties(request);
        if (manifestSettings != null) {
            try {
                jsonObject.put("name", manifestSettings.get("appName", "PWAName"));
                jsonObject.put("short_name", manifestSettings.get("shortName", "PWA ShortName"));
                jsonObject.put("icons", getManifestIcons(manifestSettings));
                jsonObject.put("start_url", manifestSettings.get("startUrl", "."));
                jsonObject.put("background_color", manifestSettings.get("bgColor", "#FFFFFF"));
                jsonObject.put("display", manifestSettings.get("display", "standalone"));
                jsonObject.put("scope", manifestSettings.get("scope", manifestSettings.get("rootPath", ".")));
                jsonObject.put("theme_color", manifestSettings.get("themeColor", "#000000"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return jsonObject.toString();
    }

    private JSONArray getManifestIcons(ValueMap manifestSettings) {

        String[] iconImages = manifestSettings.get("src", new String[]{});
        String[] iconSizes = manifestSettings.get("size", new String[]{});
        JSONArray jsonArray = new JSONArray();
        if (iconImages.length == iconSizes.length) {
            for (int i = 0; i < iconImages.length; i++) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("src", iconImages[i]);
                    jsonObject.put("type", "image/png");
                    jsonObject.put("sizes", iconSizes[i]);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                jsonArray.put(jsonObject);
            }
        }

        return jsonArray;
    }

    /**
     * End-point for loading the PWA service-worker.js in the context providing scope.
     * <p>
     * Example: HTTP GET http://localhost:4502/content/we-retail/us/en.pwa.load/service-worker.js
     * <p>
     * This would load a JS file available internally at some path specified at:
     * <p>
     * [cq:Page]/jcr:content/pwa/serviceWorkerJSPath = /apps/some/path/in/jcr/service-worker.js
     * <p>
     * This could let different PWA's on an AEM instance have different service worker implementations.
     *
     * @param request
     * @param response
     * @return
     */
    private String handleRootServiceWorker(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String navRootPath = StringUtils.EMPTY;
        String versionNumber = StringUtils.EMPTY;
        ValueMap configProps = getConfigProperties(request);
        if (configProps != null && configProps.containsKey("rootPath")) {
            navRootPath = configProps.get("rootPath").toString();
            versionNumber = configProps.get("versionNum").toString();
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("path", navRootPath);
            jsonObject.put("version", versionNumber);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return jsonObject.toString();
    }

    private ValueMap getConfigProperties(SlingHttpServletRequest request) {
        ResourceResolver serviceResolver = getServiceResolver();
        PageManager pageManager = serviceResolver.adaptTo(PageManager.class);
        // PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());
        Conf conf = confMgr.getConf(page.adaptTo(Resource.class), serviceResolver);
        return conf.getItem("cloudconfigs/pwa");


    }

    private ResourceResolver getServiceResolver() {
        final Map<String, Object> authInfo =
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE,
                        "pwa-service-handler");
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resolverFactory.getServiceResourceResolver(authInfo);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }


        return resourceResolver;
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
        return true;
        /*
        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());

        if (page != null) {
            // Only accept for
            return page.getContentResource().getValueMap().get("pwa/enabled", true);
        }

        return false;
        */
    }
}