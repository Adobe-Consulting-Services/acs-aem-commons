package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigComparator;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigImpl;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCachePersistenceException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.util.CacheUtils;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ACS AEM Commons - HTTP Cache - Cache engine Controlling service for http cache implementation. Default implementation
 * for {@link HttpCacheEngine}. Binds multiple {@link HttpCacheConfig}. Multiple {@link HttpCacheStore} also get bound
 * to this.
 */
// @formatter:off
@Component
@Properties({
        @Property(name = "jmx.objectname",
                value = "com.adobe.acs.httpcache:type=HTTP Cache Engine",
                propertyPrivate = true)
})
@References({
        @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CONFIG,
                referenceInterface = HttpCacheConfig.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.MANDATORY_MULTIPLE),

        @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES,
               referenceInterface = HttpCacheHandlingRule.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.MANDATORY_MULTIPLE),

        @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_STORE,
               referenceInterface = HttpCacheStore.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.MANDATORY_MULTIPLE)
})
@Service(value = {DynamicMBean.class, HttpCacheEngine.class})
// @formatter:on
public class HttpCacheEngineImpl extends AnnotatedStandardMBean implements HttpCacheEngine, HttpCacheEngineMBean {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    /** Method name that binds cache configs */
    static final String METHOD_NAME_TO_BIND_CONFIG = "httpCacheConfig";

    /** Thread safe list to contain the registered HttpCacheConfig references. */
    private static final ConcurrentSkipListSet<HttpCacheConfig> cacheConfigs = new ConcurrentSkipListSet<>(new HttpCacheConfigComparator());

    /** Method name that binds cache store */
    static final String METHOD_NAME_TO_BIND_CACHE_STORE = "httpCacheStore";

    /** Thread safe hash map to contain the registered cache store references. */
    private static final ConcurrentHashMap<String, HttpCacheStore> cacheStoresMap = new ConcurrentHashMap<>();

    /** Method name that binds cache handling rules */
    static final String METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES = "httpCacheHandlingRule";

    /** Thread safe list to contain the registered HttpCacheHandlingRule references. */
    private static final CopyOnWriteArrayList<HttpCacheHandlingRule> cacheHandlingRules = new CopyOnWriteArrayList<>();

    /** Thread safe list that contains the OSGi configurations for the registered httpCacheConfigs **/
    private static final ConcurrentHashMap<HttpCacheConfig, Map<String, Object>> cacheConfigConfigs = new
            ConcurrentHashMap<>();

    //-------------------<OSGi specific methods>---------------//

    /**
     * Binds cache config. Cache config could come and go at run time.
     *
     * @param cacheConfig
     * @param configs
     */
    protected void bindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> configs) {

        // Validate cache config object
        if (!cacheConfig.isValid()) {
            log.info("Http cache config rejected as the request uri is absent.");
            return;
        }

        // Check if the same object is already there in the map.
        if (cacheConfigs.contains(cacheConfig)) {
            log.trace("Http cache config object already exists in the cacheConfigs map and hence ignored.");
            return;
        }

        cacheConfigs.add(cacheConfig);
        cacheConfigConfigs.put(cacheConfig, configs);

        log.debug("Total number of cache configs added: {}", cacheConfigs.size());
    }

    /**
     * Unbinds cache config.
     *
     * @param cacheConfig
     * @param config
     */
    protected void unbindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> config) {

        if (cacheConfigs.contains(cacheConfig)) {
            // Remove the associated cached items from the cache store.
            if (cacheStoresMap.containsKey(cacheConfig.getCacheStoreName())) {
                cacheStoresMap.get(cacheConfig.getCacheStoreName()).invalidate(cacheConfig);
            } else {
                log.debug("Configured cache store is unavailable and hence nothing to invalidate.");
            }

            // Remove the entry from the map.
            cacheConfigs.remove(cacheConfig);
            cacheConfigConfigs.remove(cacheConfig);

            log.debug("Total number of cache configs after removal: {}", cacheConfigs.size());
            return;
        }
        log.debug("This cache config entry was not bound and hence nothing to unbind.");
    }

    /**
     * Binds cache store implementation
     *
     * @param cacheStore
     * @param configs
     */
    protected void bindHttpCacheStore(final HttpCacheStore cacheStore, final Map<String, Object> configs) {

        if (configs.containsKey(HttpCacheStore.KEY_CACHE_STORE_TYPE) && !cacheStoresMap.containsKey((String) configs
                .get(HttpCacheStore.KEY_CACHE_STORE_TYPE))) {
            cacheStoresMap.put(PropertiesUtil.toString(configs.get(HttpCacheStore.KEY_CACHE_STORE_TYPE), null),
                    cacheStore);
            log.debug("Cache store implementation {} has been added", (String) configs.get(HttpCacheStore
                    .KEY_CACHE_STORE_TYPE));
            log.debug("Total number of cache stores in the map: {}", cacheStoresMap.size());
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
            log.debug("Cache store removed - {}.", (String) config.get(HttpCacheStore.KEY_CACHE_STORE_TYPE));
            log.debug("Total number of cache stores after removal: {}", cacheStoresMap.size());
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
            log.debug("Total number of cache handling rule available after addition: {}", cacheHandlingRules.size());
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
            log.debug("Total number of cache handling rules available after removal: {}", cacheHandlingRules.size());
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
    public boolean isRequestCacheable(SlingHttpServletRequest request) throws HttpCacheRepositoryAccessException {
        // Execute custom rules as long as one cache config accepts the request
        for (HttpCacheHandlingRule rule : cacheHandlingRules) {
            if (!rule.onRequestReceive(request)) {
                if (log.isDebugEnabled()) {
                    log.debug("Request cannot be cached for the url {} honoring the rule {}", request.getRequestURL(),
                            rule.getClass().getName());
                }
                // Only a single rule need to fail to cause the caching mechanism to be by-passed
                return false;
            }
        }

        log.debug("All rules allow caching");
        // All rules have accepted this request, so request is cache-able.
        return true;
    }

    @Override
    public HttpCacheConfig getCacheConfig(SlingHttpServletRequest request) throws HttpCacheRepositoryAccessException {
        // Get the first accepting cache config based on the cache config order.
        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (cacheConfig.accepts(request)) {
                return cacheConfig;
            }
        }

        log.debug("Could not find an acceptable HttpCacheConfig");
        return null;
    }

    @Override
    public boolean isCacheHit(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
            HttpCacheKeyCreationException, HttpCachePersistenceException {

        // Build a cache key and do a lookup in the configured cache store.
        return getCacheStore(cacheConfig).contains(cacheConfig.buildCacheKey(request));
    }

    @Override
    public boolean deliverCacheContent(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                       HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException,
            HttpCacheDataStreamException, HttpCachePersistenceException {

        // Get the cached content from cache
        CacheContent cacheContent = getCacheStore(cacheConfig).getIfPresent(cacheConfig.buildCacheKey(request));

        // Execute custom rules.
        for (HttpCacheHandlingRule rule : cacheHandlingRules) {
            if (!rule.onCacheDeliver(request, response, cacheConfig, cacheContent)) {
                if (log.isDebugEnabled()) {
                    log.debug("Request cannot be cached for the uri {} honoring the rule {}", request.getRequestURI(),
                            rule.getClass().getName());
                }
                return false;
            }
        }

        // Spool header info into the servlet response.
        for (String headerName : cacheContent.getHeaders().keySet()) {
            for (String headerValue : cacheContent.getHeaders().get(headerName)) {
                response.setHeader(headerName, headerValue);
            }
        }

        // Spool other attributes to the servlet response.
        response.setCharacterEncoding(cacheContent.getCharEncoding());
        response.setContentType(cacheContent.getContentType());

        // Copy the cached data into the servlet output stream.
        try {
            IOUtils.copy(cacheContent.getInputDataStream(), response.getOutputStream());
            if (log.isDebugEnabled()) {
                log.debug("Response delivered from cache for the url [ {} ]", request.getRequestURI());
            }

            return true;
        } catch (IOException e) {
            throw new HttpCacheDataStreamException("Unable to copy from cached data to the servlet output stream.");
        }
    }

    @Override
    public HttpCacheServletResponseWrapper wrapResponse(SlingHttpServletRequest request, SlingHttpServletResponse
            response, HttpCacheConfig cacheConfig) throws HttpCacheDataStreamException, HttpCacheKeyCreationException {

        // Create cache key.
        CacheKey cacheKey = cacheConfig.buildCacheKey(request);

        // Wrap the response to get the copy of the stream.
        // Note - Temporary file is used to contain the copy as it suits the ability to support large streams which
        // are meant to be cached in disk/jcr.
        try {
            return new HttpCacheServletResponseWrapper(response, CacheUtils.createTemporaryCacheFile(cacheKey));
        } catch (java.io.IOException e) {
            throw new HttpCacheDataStreamException(e);
        }
    }

    @Override
    public void cacheResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig) throws HttpCacheKeyCreationException, HttpCacheDataStreamException,
            HttpCachePersistenceException {

        // Construct the cache content.
        HttpCacheServletResponseWrapper responseWrapper = null;
        if (response instanceof HttpCacheServletResponseWrapper) {
            responseWrapper = (HttpCacheServletResponseWrapper) response;
        } else {
            throw new AssertionError("Programming error.");
        }
        CacheKey cacheKey = cacheConfig.buildCacheKey(request);
        CacheContent cacheContent = new CacheContent().build(responseWrapper);

        // Execute custom rules.
        for (HttpCacheHandlingRule rule : cacheHandlingRules) {
            if (!rule.onResponseCache(request, response, cacheConfig, cacheContent)) {
                log.debug("Per custom rule {} caching for this request {} has been cancelled.", rule.getClass()
                        .getName(), request.getRequestURI());
                return;
            }
        }

        // Persist in cache.
        getCacheStore(cacheConfig).put(cacheKey, cacheContent);

        // Delete the temporary cache file.
        responseWrapper.getTempCacheFile().delete();

        log.debug("Response for the URI cached - {}", request.getRequestURI());
    }

    @Override
    public boolean isPathPotentialToInvalidate(String path) {

        // Check all the configs to see if this path is of interest.
        for (HttpCacheConfig config : cacheConfigs) {
            if (config.canInvalidate(path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void invalidateCache(String path) throws HttpCachePersistenceException {

        // Execute custom rules.
        for (HttpCacheHandlingRule rule : cacheHandlingRules) {
            if (!rule.onCacheInvalidate(path)) {
                log.debug("Per custom rule {} this invalidation has been cancelled.", rule.getClass().getName());
                return;
            }
        }

        // Find out all the cache config which has this path applicable for invalidation.
        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (cacheConfig.canInvalidate(path)) {
                getCacheStore(cacheConfig).invalidate(cacheConfig);
            }
        }
    }

    /**
     * Get the cache store set for the config if available.
     *
     * @param cacheConfig
     * @return
     * @throws HttpCachePersistenceException
     */
    private HttpCacheStore getCacheStore(HttpCacheConfig cacheConfig) throws HttpCachePersistenceException {
        if (cacheStoresMap.containsKey(cacheConfig.getCacheStoreName())) {
            return cacheStoresMap.get(cacheConfig.getCacheStoreName());
        } else {
            throw new HttpCachePersistenceException("Configured cache store unavailable " + cacheConfig
                    .getCacheStoreName());
        }

    }

    //-------------------------<Mbean specific implementation>

    public HttpCacheEngineImpl() throws NotCompliantMBeanException {
        super(HttpCacheEngineMBean.class);
    }

    @Override
    public TabularData getRegisteredHttpCacheRules() throws OpenDataException {
        final CompositeType cacheEntryType = new CompositeType(
                "HTTP Cache Handling Rule",
                "HTTP Cache Handling Rule",
                new String[]{ "HTTP Cache Handling Rule" },
                new String[]{ "HTTP Cache Handling Rule" },
                new OpenType[]{ SimpleType.STRING});

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(
                        "HTTP Cache Handling Rules",
                        "HTTP Cache Handling Rules",
                        cacheEntryType,
                        new String[]{ "HTTP Cache Handling Rule" }));

        for (HttpCacheHandlingRule rule : this.cacheHandlingRules) {
            final Map<String, Object> row = new HashMap<>();

            row.put("HTTP Cache Handling Rule", rule.getClass().getName());
            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }

    @Override
    public TabularData getRegisteredHttpCacheConfigs() throws OpenDataException {
        // Exposing all google guava stats.
        final CompositeType cacheEntryType = new CompositeType(
                "HTTP Cache Config",
                "HTTP Cache Config",
                new String[]{ "Order", "OSGi Component" },
                new String[]{ "Order", "OSGi Component" },
                new OpenType[]{ SimpleType.INTEGER, SimpleType.STRING });

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(
                        "HTTP Cache Configs",
                        "HTTP Cache Configs",
                        cacheEntryType,
                        new String[]{ "OSGi Component" }));

        for (HttpCacheConfig cacheConfig : this.cacheConfigs) {
            final Map<String, Object> row = new HashMap<>();

            Map<String, Object> osgiConfig = cacheConfigConfigs.get(cacheConfig);

            row.put("Order", PropertiesUtil.toInteger(osgiConfig.get(HttpCacheConfigImpl.PROP_ORDER),
                    HttpCacheConfigImpl.DEFAULT_ORDER));
            row.put("OSGi Component", (String) osgiConfig.get(Constants.SERVICE_PID));

            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }

    @Override
    public TabularData getRegisteredPersistenceStores() throws OpenDataException {
        final CompositeType cacheEntryType = new CompositeType(
                "HTTP Cache Store",
                "HTTP Cache Store",
                new String[]{ "HTTP Cache Store" },
                new String[]{ "HTTP Cache Store" },
                new OpenType[]{ SimpleType.STRING});

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(
                        "HTTP Cache Stores",
                        "HTTP Cache Stores",
                        cacheEntryType,
                        new String[]{ "HTTP Cache Store" }));

        for (String storeName : this.cacheStoresMap.keySet()) {
            final Map<String, Object> row = new HashMap<>();

            row.put("HTTP Cache Store", storeName);
            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }
}
