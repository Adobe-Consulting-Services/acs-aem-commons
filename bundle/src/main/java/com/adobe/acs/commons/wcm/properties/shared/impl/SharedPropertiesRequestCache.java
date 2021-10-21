package com.adobe.acs.commons.wcm.properties.shared.impl;


import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.ServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple cache for global and shared properties bindings keyed by path and persisted in a request attribute.
 */
final class SharedPropertiesRequestCache {
    private static final String REQUEST_ATTRIBUTE_NAME = SharedPropertiesRequestCache.class.getName();

    private final Map<String, Bindings> cache = new HashMap<>();

    /**
     * Constructor.
     */
    private SharedPropertiesRequestCache() {
        /* only me */
    }

    public Bindings getBindings(final String propertiesPath,
                                final Consumer<Bindings> computeIfNotFound) {
        return cache.computeIfAbsent(propertiesPath, key -> {
            final Bindings bindings = new SimpleBindings();
            computeIfNotFound.accept(bindings);
            return bindings;
        });
    }

    public static SharedPropertiesRequestCache fromRequest(ServletRequest req) {
        SharedPropertiesRequestCache cache = (SharedPropertiesRequestCache) req.getAttribute(REQUEST_ATTRIBUTE_NAME);
        if (cache == null) {
            cache = new SharedPropertiesRequestCache();
            cache.toRequest(req);
        }
        return cache;
    }

    public SharedPropertiesRequestCache toRequest(ServletRequest req) {
        SharedPropertiesRequestCache prev = (SharedPropertiesRequestCache) req.getAttribute(REQUEST_ATTRIBUTE_NAME);
        req.setAttribute(REQUEST_ATTRIBUTE_NAME, this);
        return prev;
    }
}
