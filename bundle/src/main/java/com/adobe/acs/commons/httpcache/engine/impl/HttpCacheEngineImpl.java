package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigImpl;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.felix.scr.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// TODO - Make this service tied to osgi config nodes
// TODO - Remove the immediate false annotation.

/**
 * Default implementation for {@link HttpCacheEngine}. Binds multiple {@link HttpCacheConfig}. Multiple {@link
 * HttpCacheStore} also get bound to this.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache engine",
           description = "Controlling service for http cache implementation.",
           metatype = true,
           immediate = true
)
@Service
@References({@Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CONFIG,
                        referenceInterface = HttpCacheConfig.class,
                        policy = ReferencePolicy.DYNAMIC,
                        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE),
                    @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_STORE,
                               referenceInterface = HttpCacheStore.class,
                               policy = ReferencePolicy.DYNAMIC,
                               cardinality = ReferenceCardinality.MANDATORY_MULTIPLE),
                    @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES,
                               referenceInterface = HttpCacheHandlingRule.class,
                               policy = ReferencePolicy.DYNAMIC,
                               cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
            })
public class HttpCacheEngineImpl implements HttpCacheEngine {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    /** Method name that binds cache configs */
    static final String METHOD_NAME_TO_BIND_CONFIG = "cacheConfig";

    /** Thread safe list to contain the registered HttpCacheConfig references. */
    private static CopyOnWriteArrayList<HttpCacheConfig> cacheConfigs = new CopyOnWriteArrayList<>();

    /** Method name that binds cache store */
    static final String METHOD_NAME_TO_BIND_CACHE_STORE = "cacheStore";

    /** Thread safe hash map to contain the registered cache store references. */
    private static ConcurrentHashMap<String, HttpCacheStore> cacheStoresMap = new ConcurrentHashMap<>();

    /** Method name that binds cache handling rules */
    static final String METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES = "cacheHandlingRule";

    /** Thread safe list to contain the registered HttpCacheHandlingRule references. */
    private static CopyOnWriteArrayList<HttpCacheHandlingRule> cacheHandlingRules = new CopyOnWriteArrayList<>();

    //-------------------<OSGi specific>---------------//

    /**
     * Binds cache config. Cache config could come and go at run time.
     *
     * @param cacheConfig
     * @param config
     */
    protected void bindCacheConfig(final HttpCacheConfig cacheConfig,
                                   final Map<String, Object> config) {
        cacheConfigs.add(cacheConfig);
        log.debug("Cache config for request URI {} added.", cacheConfig.getRequestUri());
        log.debug("Total number of cache configs addition - {}", cacheConfigs.size());
    }

    /**
     * Unbinds cache config.
     *
     * @param cacheConfig
     * @param config
     */
    protected void unbindCacheConfig(final HttpCacheConfig cacheConfig,
                                     final Map<String, Object> config) {
        if (cacheConfigs.contains(cacheConfig)) {
            cacheConfigs.remove(cacheConfig);
        }
        log.debug("Cache config for request URI {} removed.", cacheConfig.getRequestUri());
        log.debug("Total number of cache configs after removal - {}", cacheConfigs.size());
        // TODO - When a cache config is unbound, associated cached items should be removed from cache store.
    }

    /**
     * Binds cache store implementation
     *
     * @param cacheStore
     * @param config
     */
    protected void bindCacheStore(final HttpCacheStore cacheStore,
                                  final Map<String, Object> config) {
        if (config.containsKey(HttpCacheStore.KEY_CACHE_STORE_TYPE)
                && !cacheStoresMap.containsKey((String) config.get(HttpCacheStore.KEY_CACHE_STORE_TYPE))) {
            cacheStoresMap.put((String) config.get(HttpCacheStore.KEY_CACHE_STORE_TYPE), cacheStore);
            log.debug("Cache store implementation {} has been added", (String) config.get(HttpCacheStore
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
    protected void unbindCacheStore(final HttpCacheStore cacheStore,
                                    final Map<String, Object> config) {
        if (config.containsKey(HttpCacheStore.KEY_CACHE_STORE_TYPE) && cacheStoresMap.containsKey((String) config.get
                (HttpCacheStore.KEY_CACHE_STORE_TYPE))) {
            cacheStoresMap.remove((String) config.get(HttpCacheStore.KEY_CACHE_STORE_TYPE));
            log.debug("Cache store removed - {}.", (String) config.get(HttpCacheStore.KEY_CACHE_STORE_TYPE));
            log.debug("Total number of cache stores after removal - {}", cacheStoresMap.size());
        }
    }

    /**
     * Binds cache handling rule
     *
     * @param cacheHandlingRule
     * @param config
     */
    protected void bindCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule,
                                  final Map<String, Object> config) {
        cacheHandlingRules.add(cacheHandlingRule);
        log.debug("Cache handling rule implementation {} has been added", cacheHandlingRule.getClass().getName());
        log.debug("Total number of cache handling rule available after addition - {}", cacheHandlingRules.size());
    }

    /**
     * Unbinds handling rule.
     *
     * @param cacheHandlingRule
     * @param config
     */
    protected void unbindCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule,
                                    final Map<String, Object> config) {
        if (cacheHandlingRules.contains(cacheHandlingRule)) {
            cacheHandlingRules.remove(cacheHandlingRule);
        }
        log.debug("Cache handling rule removed - {}.", cacheHandlingRule.getClass().getName());
        log.debug("Total number of cache handling rules available after removal - {}", cacheHandlingRules.size());
    }

    @Activate
    protected void activate(Map<String, Object> configs) {
        log.info("HttpCacheEngineImpl activated.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("HttpCacheEngineImpl deactivated.");
    }
}
