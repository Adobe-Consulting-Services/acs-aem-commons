package com.adobe.acs.commons.legacyurls.impl;


import com.adobe.acs.commons.legacyurls.PreviouslyPublishedURLManager;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

@Component
@Service
public class PreviouslyPublishedURLManagerImpl implements PreviouslyPublishedURLManager {
    private static final Logger log = LoggerFactory.getLogger(PreviouslyPublishedURLManagerImpl.class);


    private static final String LOOKUP_MAP_PROPERTY = "pathReference";
    private static final String NODE_TYPE = "oak:unstructured";
    private static final String ROOT_PATH = "/var/previously-published-urls";

    @Reference
    private QueryBuilder queryBuilder;

    private static final String DEFAULT_PROPERTY_NAME = "legacyURLs";
    private String propertyName = DEFAULT_PROPERTY_NAME;
    @Property(label = "Property Name",
            description = "The property name that contains the legacy URLs to match on.",
            value = DEFAULT_PROPERTY_NAME)
    public static final String PROP_PROPERTY_NAME = "property-name";

    @Override
    public void create(ResourceResolver resourceResolver, String path, String... urls) throws RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);
        for(final String url : urls) {
            path = makePath(url);

            final Node node = JcrUtil.createPath(path, NODE_TYPE, NODE_TYPE, session, false);
            JcrUtil.setProperty(node, "pathReference", path);
        }
        session.save();
    }

    @Override
    public void delete(ResourceResolver resourceResolver, String... urls) throws RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);

        boolean save = false;

        for(final String url : urls) {
            final String path = makePath(url);
            final Node node = session.getNode(path);

            if(node != null) {
                node.remove();
                save = true;
            }
        }

        if(save) {
            session.save();
        }
    }

    @Override
    public void update(ResourceResolver resourceResolver, String path, String... urls) throws RepositoryException {
        this.create(resourceResolver, path, urls);
    }

    @Override
    public Resource find(ResourceResolver resourceResolver, String url) {
       if(1 == 1) {
           return this.findByLookup(resourceResolver, url);
       } else {
           return this.findByQuery(resourceResolver, url);
       }
    }

    private Resource findByLookup(ResourceResolver resourceResolver, String url) {
        final String path = makePath(url);

        final Resource resource = resourceResolver.getResource(path);
        if(resource != null) {
            final ValueMap properties = resource.adaptTo(ValueMap.class);
            String pathReference = properties.get(LOOKUP_MAP_PROPERTY, String.class);
            if(StringUtils.isNotBlank(pathReference)) {
                return resourceResolver.getResource(pathReference);
            }
        }
        return null;
    }

    private Resource findByQuery(ResourceResolver resourceResolver, String requestURI) {

        final Session session = resourceResolver.adaptTo(Session.class);
        final Map<String, String> params = new HashMap<String, String>();

        params.put("property", propertyName);
        params.put("property.value", requestURI);

        final Query query = queryBuilder.createQuery(PredicateGroup.create(params), session);

        final SearchResult result = query.getResult();

        final int size = result.getHits().size();
        if (size > 0) {
            if (size > 1) {
                log.warn("Found multiple [ {} ] matches for legacyURL [ {} ]", size, requestURI);

                if (log.isDebugEnabled()) {
                    for (final Hit hit : result.getHits()) {
                        try {
                            log.debug("Legacy URLs [ {} ] maps to [ {} ]", requestURI, hit.getResource().getPath());
                        } catch (RepositoryException ex) {
                            log.error(ex.getMessage());
                        }
                    }
                }
            }

            try {
                return result.getHits().get(0).getResource();
            } catch (RepositoryException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private String makePath(String url) {
        return ROOT_PATH + StringUtils.removeStart(url, "/");
    }

    @Activate
    protected final void activate(Map<String, String> config) {
        propertyName = PropertiesUtil.toString(config.get(PROP_PROPERTY_NAME), DEFAULT_PROPERTY_NAME);
    }
}
