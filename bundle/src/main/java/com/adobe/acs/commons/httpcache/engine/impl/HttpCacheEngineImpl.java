package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigImpl;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import org.apache.felix.scr.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

// TODO - Make this service tied to osgi config nodes
// TODO - Remove the immediate false annotation.

/**
 * Default implementation for {@link HttpCacheEngine}. Binds multiple {@link HttpCacheConfig}.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache engine",
           description = "Controlling service for http cache implementation.",
           metatype = true,
           immediate = true
)
@Service
@Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CONFIG,
           referenceInterface = HttpCacheConfig.class,
           policy = ReferencePolicy.DYNAMIC,
           cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class HttpCacheEngineImpl implements HttpCacheEngine {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    /** Method name that binds cache configs */
    static final String METHOD_NAME_TO_BIND_CONFIG = "cacheConfig";

    /** Thread safe hash map to contain the registered LogService references. */
    private static CopyOnWriteArrayList<HttpCacheConfig> cacheConfigs = new CopyOnWriteArrayList<>();

    //-------------------<OSGi specific>---------------//

    /**
     * Bind cache config. Cache config could come and go at run time.
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
     * Unbind cache config.
     *
     * @param cacheConfig
     * @param config
     */
    protected void unbindCacheConfig(final HttpCacheConfig cacheConfig,
                                     final Map<String, Object> config) {
        cacheConfigs.remove(cacheConfig);
        log.debug("Cache config for request URI {} removed.", cacheConfig.getRequestUri());
        log.debug("Total number of cache configs after removal - {}", cacheConfigs.size());
        // TODO - When a cache config is unbound, associated cached items should be removed from cache store.
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
