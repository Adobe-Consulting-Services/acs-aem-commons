package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.httpcache.config.CacheConfigResolver;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigImpl;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.CacheKey;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.engine.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.exception.*;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.util.CacheUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// TODO - Make this service tied to osgi config nodes
// TODO - Remove the immediate false annotation.

/**
 * Default implementation for {@link HttpCacheEngine}. Binds multiple {@link HttpCacheConfig}. Multiple {@link
 * HttpCacheStore} also get bound to this.
 */
// @formatter:off
@Component(label = "ACS AEM Commons - HTTP Cache - Cache engine",
           description = "Controlling service for http cache implementation.",
           metatype = true,
           immediate = true)
@Service

@References({
                    @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_STORE,
                               referenceInterface = HttpCacheStore.class,
                               policy = ReferencePolicy.DYNAMIC,
                               cardinality = ReferenceCardinality.MANDATORY_MULTIPLE),

                    @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES,
                               referenceInterface = HttpCacheHandlingRule.class,
                               policy = ReferencePolicy.DYNAMIC,
                               cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)})
// @formatter:on
public class HttpCacheEngineImpl implements HttpCacheEngine {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    /** Method name that binds cache store */
    static final String METHOD_NAME_TO_BIND_CACHE_STORE = "httpCacheStore";

    /** Thread safe hash map to contain the registered cache store references. */
    private static ConcurrentHashMap<String, HttpCacheStore> cacheStoresMap = new ConcurrentHashMap<>();

    /** Method name that binds cache handling rules */
    static final String METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES = "httpCacheHandlingRule";

    /** Thread safe list to contain the registered HttpCacheHandlingRule references. */
    private static CopyOnWriteArrayList<HttpCacheHandlingRule> cacheHandlingRules = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CacheConfigResolver configResolver;

    //-------------------<OSGi specific methods>---------------//

    /**
     * Binds cache store implementation
     *
     * @param cacheStore
     * @param config
     */
    protected void bindHttpCacheStore(final HttpCacheStore cacheStore, final Map<String, Object> config) {
        if (config.containsKey(HttpCacheStore.KEY_CACHE_STORE_TYPE) && !cacheStoresMap.containsKey((String) config
                .get(HttpCacheStore.KEY_CACHE_STORE_TYPE))) {
            cacheStoresMap.put((String) config.get(HttpCacheStore.KEY_CACHE_STORE_TYPE), cacheStore);
            log.info("Cache store implementation {} has been added", (String) config.get(HttpCacheStore
                    .KEY_CACHE_STORE_TYPE));
            log.debug("Total number of cache stores in the map - {}", cacheStoresMap.size());
        }
    }

    /**
     * Unbinds cache store.
     *
     * @param cacheStore
     * @param config
     */
    protected void unbindHttpCacheStore(final HttpCacheStore cacheStore, final Map<String, Object> config) {
        if (config.containsKey(HttpCacheStore.KEY_CACHE_STORE_TYPE) && cacheStoresMap.containsKey((String) config.get
                (HttpCacheStore.KEY_CACHE_STORE_TYPE))) {
            cacheStoresMap.remove((String) config.get(HttpCacheStore.KEY_CACHE_STORE_TYPE));
            log.info("Cache store removed - {}.", (String) config.get(HttpCacheStore.KEY_CACHE_STORE_TYPE));
            log.debug("Total number of cache stores after removal - {}", cacheStoresMap.size());
        }
    }

    /**
     * Binds cache handling rule
     *
     * @param cacheHandlingRule
     * @param config
     */
    protected void bindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String, Object>
            config) {
        if (!cacheHandlingRules.contains(cacheHandlingRule)) {
            cacheHandlingRules.add(cacheHandlingRule);
            log.info("Cache handling rule implementation {} has been added", cacheHandlingRule.getClass().getName());
            log.debug("Total number of cache handling rule available after addition - {}", cacheHandlingRules.size());
        }else{
            log.debug("Rule {} already present in the map and hence ignored.", cacheHandlingRule.getClass().getName());
        }

    }

    /**
     * Unbinds handling rule.
     *
     * @param cacheHandlingRule
     * @param config
     */
    protected void unbindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String,
            Object> config) {
        if (cacheHandlingRules.contains(cacheHandlingRule)) {
            cacheHandlingRules.remove(cacheHandlingRule);
            log.info("Cache handling rule removed - {}.", cacheHandlingRule.getClass().getName());
            log.debug("Total number of cache handling rules available after removal - {}", cacheHandlingRules.size());
        }else{
            log.debug("Rule {} absent present in the map and hence no action to be taken.", cacheHandlingRule.getClass().getName());
        }
    }

    @Activate
    protected void activate(Map<String, Object> configs) {
        log.info("HttpCacheEngineImpl activated.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("HttpCacheEngineImpl deactivated.");
    }

    //-----------------------<Interface specific implementation>--------//
    @Override
    public boolean isRequestCacheable(SlingHttpServletRequest request) throws HttpCacheReposityAccessException {
        boolean isRequestCacheable = false;

        // Check if there is a config matching this request.
        isRequestCacheable = configResolver.isConfigFound(request);

        // Execute custom rules.
        for (HttpCacheHandlingRule rule : cacheHandlingRules) {
            if (!rule.onRequestReceive(request)) {
                log.debug("Request cannot be cached for the uri {} honoring the rule {}", request.getRequestURI(),
                        rule.getClass().getName());
                isRequestCacheable = false;
            }
        }

        return isRequestCacheable;
    }

    @Override
    public HttpCacheConfig getCacheConfig(SlingHttpServletRequest request) throws HttpCacheConfigConflictException,
            HttpCacheReposityAccessException {
        return configResolver.resolveConfig(request);
    }

    @Override
    public boolean isCacheHit(SlingHttpServletRequest request, HttpCacheConfig cacheConfig)throws HttpCachePersistenceException {
        // Check if the cache set in the config contains the key.
        if (cacheStoresMap.contains(cacheConfig.getCacheStoreName())) {
            return cacheStoresMap.get(cacheConfig.getCacheStoreName()).contains(new CacheKey().build(request,
                    cacheConfig));
        } else {
            throw new HttpCachePersistenceException("Cache store set in the config is not available.");
        }
    }

    @Override
    public void deliverCacheContent(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                    HttpCacheConfig cacheConfig) {
        // Get the cached content from cache
        CacheContent cacheContent = cacheStoresMap.get(cacheConfig.getCacheStoreName()).getIfPresent(new CacheKey()
                .build(request, cacheConfig));

        // Execute custom rules.
        for (HttpCacheHandlingRule rule : cacheHandlingRules) {
            if (!rule.onCacheDeliver(request, response)) {
                // TODO log details
                return;
            }
        }

        // TODO - Populate the response
    }

    @Override
    public void markRequestCacheable(SlingHttpServletRequest request) {
        request.setAttribute(HttpCacheEngine.FLAG_IS_REQUEST_CACHEABLE_KEY, HttpCacheEngine
                .FLAG_IS_REQUEST_CACHEABLE_VALUE_YES);
    }

    @Override
    public void markRequestNotCacheable(SlingHttpServletRequest request) {
        request.setAttribute(HttpCacheEngine.FLAG_IS_REQUEST_CACHEABLE_KEY, HttpCacheEngine
                .FLAG_IS_REQUEST_CACHEABLE_VALUE_NO);
    }

    @Override
    public boolean isResponseCacheable(SlingHttpServletRequest request) {
        // TODO - Verify if the given request has the flag set by #markRequestCacheable
        return false;
    }

    @Override
    public HttpCacheServletResponseWrapper wrapResponse(SlingHttpServletRequest request, SlingHttpServletResponse
            response, HttpCacheConfig cacheConfig) throws HttpCacheException {
        CacheKey cacheKey = new CacheKey().build(request, cacheConfig);

        try {
            return new HttpCacheServletResponseWrapper(response, CacheUtils.createTemporaryCacheFile(cacheKey));
        } catch (FileNotFoundException e) {
            // TODO - Handle and rethrow.
            throw new HttpCacheException(e);
        }
    }

    @Override
    public void cacheResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig) {
        HttpCacheServletResponseWrapper responseWrapper = null;

        if (response instanceof HttpCacheServletResponseWrapper) {
            responseWrapper = (HttpCacheServletResponseWrapper) response;
        } else {
            // Assert error.
        }

        // Execute custom rules.
        for (HttpCacheHandlingRule rule : cacheHandlingRules) {
            if (!rule.onResponseCache(request, response)) {
                log.debug("Per custom rule {} caching for this request {} has been cancelled.", rule.getClass()
                        .getName(), request.getRequestURI());
                return;
            }
        }

        // TODO - Find out the when the stream gets closed as it's tied to servlet response stream closure.
        CacheKey cacheKey = new CacheKey().build(request, cacheConfig);
        CacheContent cacheContent = null;
        try {
            cacheContent = new CacheContent().build(responseWrapper);
            cacheStoresMap.get(cacheConfig.getCacheStoreName()).put(cacheKey, cacheContent);
        } catch (HttpCacheDataStreamException e) {
            // TODO - Revamp exception handling strategy.
        }
    }

    @Override
    public boolean isPathPotentialToInvalidate(String path) {
        // TODO - Find out all the cache config which has this path applicable for invalidation. If one of them is
        // found, return false;
        return false;
    }

    @Override
    public void invalidateCache(String path) {
        // Execute custom rules.
        for (HttpCacheHandlingRule rule : cacheHandlingRules) {
            if (!rule.onCacheInvalidate(path)) {
                log.debug("Per custom rule {} this invalidation has been cancelled.", rule.getClass().getName());
                return;
            }
        }

        // TODO - Find out all the cache config which has this path applicable for invalidation.
        // TODO - Hit the corresponding store and check if corresponding key is present.
        // TODO - If present, invalidate the cache.
    }
}
