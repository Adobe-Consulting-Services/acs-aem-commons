package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigImpl;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.engine.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.exception.HttpCacheConfigConflictException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheReposityAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.util.CacheUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ACS AEM Commons - HTTP Cache - Cache engine Controlling service for http cache implementation. Default implementation
 * for {@link HttpCacheEngine}. Binds multiple {@link HttpCacheConfig}. Multiple {@link HttpCacheStore} also get bound
 * to this.
 */

// TODO - Make this service tied to osgi config nodes
// TODO - Remove the immediate false annotation.


// @formatter:off
@Component(immediate = true)
@Service
@References({@Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CONFIG,
                        referenceInterface = HttpCacheConfig.class,
                        policy = ReferencePolicy.DYNAMIC,
                        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE),

                   /* @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_STORE,
                               referenceInterface = HttpCacheStore.class,
                               policy = ReferencePolicy.DYNAMIC,
                               cardinality = ReferenceCardinality.MANDATORY_MULTIPLE),

                    @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES,
                               referenceInterface = HttpCacheHandlingRule.class,
                               policy = ReferencePolicy.DYNAMIC,
                               cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)*/})
// @formatter:on
public class HttpCacheEngineImpl implements HttpCacheEngine {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    /** Method name that binds cache configs */
    static final String METHOD_NAME_TO_BIND_CONFIG = "httpCacheConfig";

    /** Thread safe list to contain the registered HttpCacheConfig references. */
    private static CopyOnWriteArrayList<HttpCacheConfig> cacheConfigs = new CopyOnWriteArrayList<>();

    /** Method name that binds cache store */
    static final String METHOD_NAME_TO_BIND_CACHE_STORE = "httpCacheStore";

    /** Thread safe hash map to contain the registered cache store references. */
    private static ConcurrentHashMap<String, HttpCacheStore> cacheStoresMap = new ConcurrentHashMap<>();

    /** Method name that binds cache handling rules */
    static final String METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES = "httpCacheHandlingRule";

    /** Thread safe list to contain the registered HttpCacheHandlingRule references. */
    private static CopyOnWriteArrayList<HttpCacheHandlingRule> cacheHandlingRules = new CopyOnWriteArrayList<>();

    //-------------------<OSGi specific methods>---------------//

    /**
     * Binds cache config. Cache config could come and go at run time.
     *
     * @param cacheConfig
     * @param config
     */
    protected void bindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> config) {
        // Validate cache config object
        // Check if the request uri is present.
        if (cacheConfig.isValid()) {
            log.info("Http cache config rejected at the request uri is absent.");
            return;
        }

        /*
        // Remove the user groups array if the config is tied to anonymous requests.
        if (!AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST.equals(cacheConfig.getAuthenticationRequirement())
                && !cacheConfig.getUserGroupNames().isEmpty()) {
            cacheConfig.getUserGroupNames().clear();
            log.debug("Config is for unauthenticated requests and hence list of groups configured are rejected.");
        }
        */

        // Check if the same object is already there in the map.
        if (cacheConfigs.contains(cacheConfig)) {
            log.trace("Http cache config object already exists in the cacheConfigs map and hence ignored.");
            return;
        }

        // Add it to the map.
        cacheConfigs.add(cacheConfig);
        //log.info("Cache config for request URIs {} added.", cacheConfig.getRequestURIs().toString());
        log.debug("Total number of cache configs added - {}", cacheConfigs.size());
    }

    /**
     * Unbinds cache config.
     *
     * @param cacheConfig
     * @param config
     */
    protected void unbindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> config) {
        if (cacheConfigs.contains(cacheConfig)) {
            cacheConfigs.remove(cacheConfig);
            // TODO - When a cache config is unbound, associated cached items should be removed from the cache store.
            //log.info("Cache config for request URI {} removed.", cacheConfig.getRequestURIs().toString());
            log.debug("Total number of cache configs after removal - {}", cacheConfigs.size());
            return;
        }
        log.debug("This cache config entry was not bound and hence nothing to unbind.");
    }

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
            log.debug("Cache handling rule implementation {} has been added", cacheHandlingRule.getClass().getName());
            log.debug("Total number of cache handling rule available after addition - {}", cacheHandlingRules.size());
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
            log.debug("Cache handling rule removed - {}.", cacheHandlingRule.getClass().getName());
            log.debug("Total number of cache handling rules available after removal - {}", cacheHandlingRules.size());
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

        // Check if any of the cache config accepts this request.
        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (cacheConfig.accepts(request)) {
                isRequestCacheable = true;
            }
        }

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
    public HttpCacheConfig getCacheConfig(SlingHttpServletRequest request) throws HttpCacheReposityAccessException,
            HttpCacheConfigConflictException {

        List<HttpCacheConfig> matchingConfigs = new ArrayList<>();

        // Collect all the matching cache configs.
        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (cacheConfig.accepts(request)) {
                matchingConfigs.add(cacheConfig);
            }
        }

        // If there is more than one matching cache config, throw Cache Conflict exception.
        // Ideally, when there are multiple configs matching, the one with closest match has to be chosen based on
        // certain ranking mechanism. For the sake of simplicity, it' been reserved for future implementation.
        if (matchingConfigs.size() == 1) {
            return matchingConfigs.get(0);
        } else if (matchingConfigs.size() > 1) {
            throw new HttpCacheConfigConflictException("Multiple matching cache configs found and unable to " +
                    "determine the closest match");
        } else {
            log.debug("Could not find an acceptable HttpCacheConfig");
            return null;
        }
    }

    @Override
    public boolean isCacheHit(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
        // Check if the cache set in the config contains the key.
        if (cacheStoresMap.contains(cacheConfig.getCacheStoreName())) {
            return cacheStoresMap.get(cacheConfig.getCacheStoreName()).contains(cacheConfig.buildCacheKey(request));
        } else {
            log.debug("Cache store set in the config is not available.");
        }
        return false;
    }

    @Override
    public void deliverCacheContent(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                    HttpCacheConfig cacheConfig) {
        // Get the cached content from cache
        CacheContent cacheContent = cacheStoresMap.get(cacheConfig.getCacheStoreName()).getIfPresent(cacheConfig
                .buildCacheKey(request));

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
        CacheKey cacheKey = cacheConfig.buildCacheKey(request);

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
        CacheKey cacheKey = cacheConfig.buildCacheKey(request);
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

        for (HttpCacheConfig config : cacheConfigs) {
            if (config.canInvalidate(path)) {
                return true;
            }
        }

        return true;
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

        for (HttpCacheConfig config : cacheConfigs) {
            if (config.canInvalidate(path)) {
                HttpCacheStore cacheStore = cacheStoresMap.get(config.getCacheStoreName());
                if (cacheStore != null) {
                    // FIXME - This is against the multiple cache config concept.
                    /*if (config.isInvalidateAll()) {
                        // Config is marked to always invalidate all entries for each invalidation event
                        cacheStore.invalidateAll();
                        ;
                    } else {
                        // Config is marked to check each entry for selective invalidation
                        // TODO this is O(N) where N is # of entries in cache
                        cacheStore.invalidate(path);
                    }*/
                }
            }
        }

        // TODO - Find out all the cache config which has this path applicable for invalidation.
        // TODO - Hit the corresponding store and check if corresponding key is present.
        // TODO - If present, invalidate the cache.
    }
}
