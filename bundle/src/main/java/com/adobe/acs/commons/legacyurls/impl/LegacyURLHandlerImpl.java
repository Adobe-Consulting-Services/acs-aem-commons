package com.adobe.acs.commons.legacyurls.impl;

import com.adobe.acs.commons.legacyurls.LegacyURLHandler;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Service
public class LegacyURLHandlerImpl implements LegacyURLHandler {
    private static final Logger log = LoggerFactory.getLogger(LegacyURLHandlerImpl.class);

    @Reference
    private QueryBuilder queryBuilder;

    private static final String DEFAULT_PROPERTY_NAME = "legacyURLs";
    private String propertyName = DEFAULT_PROPERTY_NAME;
    @Property(label = "Property Name",
            description = "The property name that contains the legacy URLs to match on.",
            value = DEFAULT_PROPERTY_NAME)
    public static final String PROP_PROPERTY_NAME = "property-name";

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
            intValue = { SlingHttpServletResponse.SC_MOVED_PERMANENTLY, SlingHttpServletResponse.SC_TEMPORARY_REDIRECT })
    public static final String PROP_REDIRECT_STATUS_CODE = "redirect-status-code";

    public final boolean doRedirect(SlingHttpServletRequest request,
                                    SlingHttpServletResponse response) throws IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        final Session session = resourceResolver.adaptTo(Session.class);
        final String requestURI = request.getRequestURI();

        final Map<String, String> params = new HashMap<String, String>();

        params.put("property", propertyName);
        params.put("property.value", requestURI);

        final Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);

        final SearchResult result = query.getResult();

        final int size = result.getHits().size();
        if (size > 0) {
            if (size > 1) {
                log.warn("Found multiple [ {} ] matches for legacyURL [ {} ]", size, requestURI);

                if(log.isDebugEnabled()) {
                    for (final Hit hit : result.getHits()) {
                        try {
                            log.debug("Legacy URLs [ {} ] maps to [ {} ]", requestURI, hit.getResource().getPath());
                        } catch (RepositoryException ex) {
                            log.error(ex.getMessage());
                        }
                    }
                }
            }

            String redirectURI = null;

            final Hit hit = result.getHits().get(0);
            final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

            try {
                final Page page = pageManager.getContainingPage(hit.getResource());

                if (page != null) {
                    redirectURI = resourceResolver.map(page.getPath() + pageExtension);
                } else if (DamUtil.isAsset(hit.getResource())) {
                    final Asset asset = DamUtil.resolveToAsset(hit.getResource());
                    redirectURI = asset.getPath();
                } else {
                    redirectURI = hit.getResource().getPath();
                }

                if (!StringUtils.isBlank(redirectURI)) {
                    redirectURI = resourceResolver.map(redirectURI);

                    response.setStatus(redirectStatusCode);
                    response.sendRedirect(redirectURI);
                    return true;
                }
            } catch (RepositoryException e) {
                log.error("RepositoryException retrieving the resource the legacy URL should redirect to [ {} ]",
                        requestURI);
            }
        }

        return false;
    }

    @Activate
    protected final void activate(Map<String, String> config) {
        propertyName = PropertiesUtil.toString(config.get(PROP_PROPERTY_NAME), DEFAULT_PROPERTY_NAME);

        pageExtension = PropertiesUtil.toString(config.get(PROP_PAGE_EXTENSION), DEFAULT_PAGE_EXTENSION);
        if (!StringUtils.startsWith(pageExtension, ".")) {
            pageExtension = "." + pageExtension;
        }

        redirectStatusCode = PropertiesUtil.toInteger(config.get(PROP_REDIRECT_STATUS_CODE),
                DEFAULT_REDIRECT_STATUS_CODE);

        log.info("Legacy URL Handler - Property Name: {}", propertyName);
        log.info("Legacy URL Handler - Page Extension: {}", pageExtension);
        log.info("Legacy URL Handler - Redirect Status Code: {}", redirectStatusCode);
    }
}
