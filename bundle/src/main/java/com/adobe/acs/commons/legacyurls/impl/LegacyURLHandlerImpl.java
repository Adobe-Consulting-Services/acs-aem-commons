package com.adobe.acs.commons.legacyurls.impl;

import com.adobe.acs.commons.legacyurls.LegacyURLHandler;
import com.adobe.acs.commons.legacyurls.PreviouslyPublishedURLManager;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
@Service
public class LegacyURLHandlerImpl implements LegacyURLHandler {
    private static final Logger log = LoggerFactory.getLogger(LegacyURLHandlerImpl.class);

    @Reference
    private PreviouslyPublishedURLManager previouslyPublishedURLManager;

    private static final String DEFAULT_PAGE_EXTENSION = ".html";
    private String pageExtension = DEFAULT_PAGE_EXTENSION;
    @Property(label = "Page extension",
            description = "",
            value = DEFAULT_PAGE_EXTENSION)
    public static final String PROP_PAGE_EXTENSION = "page-extension";


    private static final int DEFAULT_REDIRECT_STATUS_CODE = SlingHttpServletResponse.SC_MOVED_PERMANENTLY;
    private int redirectStatusCode = DEFAULT_REDIRECT_STATUS_CODE;
    @Property(label = "Redirect Status Code",
            description = "[ 301: Moved Permanently ] , [ 302: Found (Temporary) ] - [ Default: 301 ]",
            cardinality = 2,
            intValue = { SlingHttpServletResponse.SC_MOVED_PERMANENTLY,
                    SlingHttpServletResponse.SC_TEMPORARY_REDIRECT })
    public static final String PROP_REDIRECT_STATUS_CODE = "redirect-status-code";

    public final boolean doRedirect(HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {

        if (!(request instanceof SlingHttpServletRequest)) {
            log.warn("Request for [ {} ] was not a SlingHttpServletRequest", request.getRequestURI());
            return false;
        }

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

        log.debug("Invoking LegacyURLHandler");

        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();
        final String requestURI = request.getRequestURI();

        String redirectURI;
        Resource targetResource;

        targetResource = previouslyPublishedURLManager.find(resourceResolver, requestURI);

        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        final Page page = pageManager.getContainingPage(targetResource);

        if (page != null) {
            redirectURI = resourceResolver.map(page.getPath() + pageExtension);
        } else if (DamUtil.isAsset(targetResource)) {
            final Asset asset = DamUtil.resolveToAsset(targetResource);
            redirectURI = asset.getPath();
        } else {
            redirectURI = targetResource.getPath();
        }

        if (!StringUtils.isBlank(redirectURI)) {
            redirectURI = resourceResolver.map(redirectURI);

            log.info("Redirecting legacy URI [ {} ] to [ {} ]", requestURI, redirectURI);

            response.setStatus(redirectStatusCode);
            response.sendRedirect(redirectURI);
            return true;
        }

        return false;
    }


    @Activate
    protected final void activate(Map<String, String> config) {

        pageExtension = PropertiesUtil.toString(config.get(PROP_PAGE_EXTENSION), DEFAULT_PAGE_EXTENSION);
        if (!StringUtils.startsWith(pageExtension, ".")) {
            pageExtension = "." + pageExtension;
        }

        redirectStatusCode = PropertiesUtil.toInteger(config.get(PROP_REDIRECT_STATUS_CODE),
                DEFAULT_REDIRECT_STATUS_CODE);

        log.info("Legacy URL Handler - Page Extension: {}", pageExtension);
        log.info("Legacy URL Handler - Redirect Status Code: {}", redirectStatusCode);
    }
}
