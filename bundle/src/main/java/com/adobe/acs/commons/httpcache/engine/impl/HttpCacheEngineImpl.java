package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigComparator;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigImpl;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.exception.*;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation for {@link HttpCacheEngine}. Binds multiple {@link HttpCacheConfig}. Multiple {@link
 * HttpCacheStore} also get bound to this.
 */
// @formatter:off
@Component(
        label = "ACS AEM Commons - HTTP Cache - Engine",
        description = "Controlling service for http cache implementation.",
        metatype = true
)
@Properties({
        @Property(name = "jmx.objectname",
                value = "com.adobe.acs.httpcache:type=HTTP Cache Engine",
                propertyPrivate = true),
        @Property(name = "webconsole.configurationFactory.nameHint",
                value = "Global handling rules: {httpcache.engine.cache-handling-rules.global}",
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
               cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE),

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
    private static final CopyOnWriteArrayList<HttpCacheConfig> cacheConfigs = new CopyOnWriteArrayList<HttpCacheConfig>();

    /** Method name that binds cache store */
    static final String METHOD_NAME_TO_BIND_CACHE_STORE = "httpCacheStore";
    /** Thread safe hash map to contain the registered cache store references. */
    private static final ConcurrentHashMap<String, HttpCacheStore> cacheStoresMap = new ConcurrentHashMap<String, HttpCacheStore>();

    /** Method name that binds cache handling rules */
    static final String METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES = "httpCacheHandlingRule";
    /** Thread safe map to contain the registered HttpCacheHandlingRule references. */
    private static final ConcurrentHashMap<String, HttpCacheHandlingRule> cacheHandlingRules = new
            ConcurrentHashMap<String, HttpCacheHandlingRule>();

    // formatter:off
    @Property(label = "Global HttpCacheHandlingRules",
              description = "List of Service pid of HttpCacheHandlingRule applicable for all cache configs.",
              unbounded = PropertyUnbounded.ARRAY,
              value = {"com.adobe.acs.commons.httpcache.rule.impl.CacheOnlyGetRequest",
                      "com.adobe.acs.commons.httpcache.rule.impl.CacheOnlyResponse200",
                      "com.adobe.acs.commons.httpcache.rule.impl.HonorCacheControlHeaders",
                      "com.adobe.acs.commons.httpcache.rule.impl.DoNotCacheZeroSizeResponse"
              })
    // formatter:on
    private static final String PROP_GLOBAL_CACHE_HANDLING_RULES_PID = "httpcache.engine.cache-handling-rules.global";
    private List<String> globalCacheHandlingRulesPid;

    /** Thread safe list containing the OSGi configurations for the registered httpCacheConfigs. Used only for mbean.*/
    private static final ConcurrentHashMap<HttpCacheConfig, Map<String, Object>> cacheConfigConfigs = new
            ConcurrentHashMap<HttpCacheConfig, Map<String, Object>>();

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

        // Sort cacheConfigs by order; Synchronized since this bind/un-bind is a rare/limited event
        synchronized (this.cacheConfigs) {
            final List<HttpCacheConfig> tmp = new ArrayList<HttpCacheConfig>(this.cacheConfigs);
            tmp.add(cacheConfig);

            Collections.sort(tmp, new HttpCacheConfigComparator());
            this.cacheConfigs.clear();
            this.cacheConfigs.addAll(tmp);
        }

        this.cacheConfigConfigs.put(cacheConfig, configs);

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
     * @param configs
     */
    protected void bindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String, Object>
            configs) {

        // Get the service pid and make it as key.
        String servicePid = PropertiesUtil.toString(configs.get("service.pid"), StringUtils.EMPTY);
        if (!cacheHandlingRules.containsKey(servicePid)) {
            cacheHandlingRules.put(servicePid, cacheHandlingRule);
            log.debug("Cache handling rule implementation {} has been added", cacheHandlingRule.getClass().getName());
            log.debug("Total number of cache handling rule available after addition: {}", cacheHandlingRules.size());
        }
    }

    /**
     * Unbinds handling rule.
     *
     * @param cacheHandlingRule
     * @param configs
     */
    protected void unbindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String,
            Object> configs) {

        String servicePid = PropertiesUtil.toString(configs.get("service.pid"), StringUtils.EMPTY);
        if (cacheHandlingRules.remove(servicePid) != null) {
            log.debug("Cache handling rule removed - {}.", cacheHandlingRule.getClass().getName());
            log.debug("Total number of cache handling rules available after removal: {}", cacheHandlingRules.size());
        }
    }

    @Activate
    protected void activate(Map<String, Object> configs) {

        // PIDs of global cache handling rules.
        globalCacheHandlingRulesPid = new ArrayList<String>(Arrays.asList(PropertiesUtil.toStringArray(configs.get
                (PROP_GLOBAL_CACHE_HANDLING_RULES_PID), new String[]{})));
        ListIterator<String> listIterator = globalCacheHandlingRulesPid.listIterator();
        while (listIterator.hasNext()) {
            String value = listIterator.next();
            if (StringUtils.isBlank(value)) {
                listIterator.remove();
            }
        }
        log.info("HttpCacheEngineImpl activated.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("HttpCacheEngineImpl deactivated.");
    }

    //-----------------------<Interface specific implementation>--------//
    @Override
    public boolean isRequestCacheable(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
            HttpCacheRepositoryAccessException {

        // Execute custom rules.
        for (final Map.Entry<String, HttpCacheHandlingRule> entry : cacheHandlingRules.entrySet()) {
            // Apply rule if it's a configured global or cache-config tied rule.
            if (globalCacheHandlingRulesPid.contains(entry.getKey()) || cacheConfig.acceptsRule(entry.getKey())) {
                HttpCacheHandlingRule rule = entry.getValue();
                if (!rule.onRequestReceive(request)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Request cannot be cached for the url {} honoring the rule {}", request
                                .getRequestURL(), rule.getClass().getName());
                    }
                    // Only a single rule need to fail to cause the caching mechanism to be by-passed
                    return false;
                }
            }
        }

        // All rules have accepted this request, so request is cache-able.
        return true;
    }

    @Override
    public HttpCacheConfig getCacheConfig(SlingHttpServletRequest request) throws HttpCacheRepositoryAccessException,
            HttpCacheConfigConflictException {

        // Get the first accepting cache config based on the cache config order.
        HttpCacheConfig bestCacheConfig = null;

        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (bestCacheConfig != null) {
                // A matching HttpCacheConfig has been found, so check for order + acceptance conflicts
                if (bestCacheConfig.getOrder() == cacheConfig.getOrder()) {
                    if (cacheConfig.accepts(request)) {
                        // Throw an exception if two HttpCacheConfigs w the same order accept the same request
                        throw new HttpCacheConfigConflictException();
                    }
                } else if (bestCacheConfig.getOrder() < cacheConfig.getOrder()) {
                    // Since cacheConfigs is sorted by order, this means all other orders will not match
                    break;
                }
            } else if (cacheConfig.accepts(request)) {
                bestCacheConfig = cacheConfig;
            }
        }

        if ((bestCacheConfig == null) && log.isDebugEnabled()) {
            log.debug("Matching cache config not found.");
        }

        return bestCacheConfig;
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
        for (final Map.Entry<String, HttpCacheHandlingRule> entry : cacheHandlingRules.entrySet()) {
            // Apply rule if it's a configured global or cache-config tied rule.
            if (globalCacheHandlingRulesPid.contains(entry.getKey()) || cacheConfig.acceptsRule(entry.getKey())) {
                HttpCacheHandlingRule rule = entry.getValue();
                if (!rule.onCacheDeliver(request, response, cacheConfig, cacheContent)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cache cannot be delivered for the url {} honoring the rule {}", request
                                .getRequestURL(), rule.getClass().getName());
                    }
                    return false;
                }
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
            response, HttpCacheConfig cacheConfig) throws HttpCacheDataStreamException,
            HttpCacheKeyCreationException, HttpCachePersistenceException {

        // Create cache key.
        CacheKey cacheKey = cacheConfig.buildCacheKey(request);

        // Wrap the response to get the copy of the stream.
        // Temp sink for the duplicate stream is chosen based on the cache store configured at cache config.
        try {
            return new HttpCacheServletResponseWrapper(response, getCacheStore(cacheConfig).createTempSink());
        } catch (java.io.IOException e) {
            throw new HttpCacheDataStreamException(e);
        }
    }

    @Override
    public void cacheResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig) throws HttpCacheKeyCreationException, HttpCacheDataStreamException,
            HttpCachePersistenceException {

        // TODO - This can be made asynchronous to avoid performance penality on response cache.

        CacheContent cacheContent = null;
        try {
            // Construct the cache content.
            HttpCacheServletResponseWrapper responseWrapper = null;
            if (response instanceof HttpCacheServletResponseWrapper) {
                responseWrapper = (HttpCacheServletResponseWrapper) response;
            } else {
                throw new AssertionError("Programming error.");
            }
            CacheKey cacheKey = cacheConfig.buildCacheKey(request);
            cacheContent = new CacheContent().build(responseWrapper);

            // Execute custom rules.
            boolean canCacheResponse = true;
            for (final Map.Entry<String, HttpCacheHandlingRule> entry : cacheHandlingRules.entrySet()) {
                // Apply rule if it's a configured global or cache-config tied rule.
                if (globalCacheHandlingRulesPid.contains(entry.getKey()) || cacheConfig.acceptsRule(entry.getKey())) {
                    HttpCacheHandlingRule rule = entry.getValue();
                    if (!rule.onResponseCache(request, response, cacheConfig, cacheContent)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Per custom rule {} caching for this request {} has been cancelled.", rule
                                    .getClass().getName(), request.getRequestURI());
                        }
                        canCacheResponse = false;
                        break;
                    }
                }
            }

            // Persist in cache.
            if (canCacheResponse) {
                getCacheStore(cacheConfig).put(cacheKey, cacheContent);
                log.debug("Response for the URI cached - {}", request.getRequestURI());
            }
        } finally {
            // Close the temp sink input stream.
            if (null != cacheContent) {
                IOUtils.closeQuietly(cacheContent.getInputDataStream());
            }
        }

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

        // Find out all the cache config which has this path applicable for invalidation.
        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (cacheConfig.canInvalidate(path)) {

                // Execute custom rules.
                for (final Map.Entry<String, HttpCacheHandlingRule> entry : cacheHandlingRules.entrySet()) {
                    // Apply rule if it's a configured global or cache-config tied rule.
                    if (globalCacheHandlingRulesPid.contains(entry.getKey()) || cacheConfig.acceptsRule(entry.getKey
                            ())) {
                        HttpCacheHandlingRule rule = entry.getValue();
                        if (rule.onCacheInvalidate(path)) {
                            getCacheStore(cacheConfig).invalidate(cacheConfig);
                        } else {
                            log.debug("Cache invalidation rejected for path {} per custom rule {}", path, rule
                                    .getClass().getName());
                        }
                    }
                }

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
        // @formatter:off
        final CompositeType cacheEntryType = new CompositeType(
                "HTTP Cache Handling Rule",
                "HTTP Cache Handling Rule",
                new String[]{"HTTP Cache Handling Rule"},
                new String[]{"HTTP Cache Handling Rule"},
                new OpenType[]{SimpleType.STRING});

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(
                        "HTTP Cache Handling Rules",
                        "HTTP Cache Handling Rules",
                        cacheEntryType,
                        new String[]{"HTTP Cache Handling Rule"}));
        // @formatter:on

        for (final Map.Entry<String, HttpCacheHandlingRule> entry : cacheHandlingRules.entrySet()) {
            final Map<String, Object> row = new HashMap<String, Object>();

            row.put("HTTP Cache Handling Rule", entry.getValue().getClass().getName());
            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }

    @Override
    public TabularData getRegisteredHttpCacheConfigs() throws OpenDataException {
        // @formatter:off
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

        // @formatter:on

        for (HttpCacheConfig cacheConfig : this.cacheConfigs) {
            final Map<String, Object> row = new HashMap<String, Object>();

            Map<String, Object> osgiConfig = cacheConfigConfigs.get(cacheConfig);

            row.put("Order", cacheConfig.getOrder());
            row.put("OSGi Component", (String) osgiConfig.get(Constants.SERVICE_PID));

            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }

    @Override
    public TabularData getRegisteredPersistenceStores() throws OpenDataException {
        // @formatter:off
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
        // @formatter:on

        for (String storeName : this.cacheStoresMap.keySet()) {
            final Map<String, Object> row = new HashMap<String, Object>();

            row.put("HTTP Cache Store", storeName);
            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }
}
