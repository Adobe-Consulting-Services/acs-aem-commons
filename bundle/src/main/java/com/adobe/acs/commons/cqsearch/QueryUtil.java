package com.adobe.acs.commons.cqsearch;

import com.day.cq.search.Query;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Simple utility to use as an alternative to deprecated CloseableQuery in older versions of AEM.
 */
public class QueryUtil {
    private static final Logger log = LoggerFactory.getLogger(QueryUtil.class);

    /**
     * Uses reflection to forcibly set the Query object's ResourceResolver to the provided.
     */
    public static void setResourceResolverOn(ResourceResolver resolver, Query query) {
        Class<? extends Query> clazz = query.getClass();
        try {
            Field resourceResolverField = clazz.getDeclaredField("resourceResolver");
            resourceResolverField.setAccessible(true);
            resourceResolverField.set(query, resolver);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("Could not set ResourceResolver on provided Query: {} => {}",
                    e.getClass().getName(), e.getMessage());
        }
    }
}
