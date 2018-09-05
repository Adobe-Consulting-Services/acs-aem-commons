/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigComparator;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigImpl;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.engine.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.exception.HttpCacheConfigConflictException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCachePersistenceException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

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

    /** jmx property labels */
    static final String JMX_PN_ORDER = "Order";
    static final String JMX_PN_OSGICOMPONENT = "OSGi Component";
    static final String JMX_PN_HTTPCACHE_CONFIGS = "HTTP Cache Configs";
    static final String JMX_PN_HTTPCACHE_CONFIG = "HTTP Cache Config";
    static final String JMX_PN_HTTPCACHE_STORE = "HTTP Cache Store";
    static final String JMX_PN_HTTPCACHE_STORES = "HTTP Cache Stores";
    static final String JMX_HTTPCACHE_HANDLING_RULE = "HTTP Cache Handling Rule";
    static final String JMX_PN_HTTPCACHE_HANDLING_RULES = "HTTP Cache Handling Rules";

    /** Thread safe list to contain the registered HttpCacheConfig references. */
    private CopyOnWriteArrayList<HttpCacheConfig> cacheConfigs = new CopyOnWriteArrayList<HttpCacheConfig>();

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
    private final ConcurrentHashMap<HttpCacheConfig, Map<String, Object>> cacheConfigConfigs = new
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

        // Sort cacheConfigs by order
        final CopyOnWriteArrayList<HttpCacheConfig> tmp = new CopyOnWriteArrayList<HttpCacheConfig>(this.cacheConfigs);
        tmp.add(cacheConfig);

        Collections.sort(tmp, new HttpCacheConfigComparator());
        this.cacheConfigs = tmp;

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
     * @param properties
     */
    protected void bindHttpCacheStore(final HttpCacheStore cacheStore, final Map<String, Object> properties) {
        final String cacheStoreType = PropertiesUtil.toString(properties.get(HttpCacheStore.KEY_CACHE_STORE_TYPE), null);
        if (cacheStoreType != null && cacheStoresMap.putIfAbsent(cacheStoreType, cacheStore) == null) {

            log.debug("Cache store implementation {} has been added", (String) properties.get(HttpCacheStore
                    .KEY_CACHE_STORE_TYPE));
            log.debug("Total number of cache stores in the map: {}", cacheStoresMap.size());
        }
    }

    /**
     * Unbinds cache store.
     *
     * @param cacheStore
     * @param properties
     */
    protected void unbindHttpCacheStore(final HttpCacheStore cacheStore, final Map<String, Object> properties) {
        final String cacheStoreType = PropertiesUtil.toString(properties.get(HttpCacheStore.KEY_CACHE_STORE_TYPE), null);
        if (cacheStoreType != null && cacheStoresMap.remove(cacheStoreType) != null) {
            log.debug("Cache store removed - {}.", (String) properties.get(HttpCacheStore.KEY_CACHE_STORE_TYPE));
            log.debug("Total number of cache stores after removal: {}", cacheStoresMap.size());
        }
    }

    /**
     * Binds cache handling rule
     *
     * @param cacheHandlingRule
     * @param properties
     */
    protected void bindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String, Object>
            properties) {

        // Get the service pid and make it as key.
        String servicePid = PropertiesUtil.toString(properties.get("service.pid"), StringUtils.EMPTY);
        if (cacheHandlingRules.putIfAbsent(servicePid, cacheHandlingRule) == null) {
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
        globalCacheHandlingRulesPid = new ArrayList<String>(Arrays.asList(PropertiesUtil.toStringArray(configs.get(
                PROP_GLOBAL_CACHE_HANDLING_RULES_PID), new String[]{})));
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
        return getCacheConfig(request, HttpCacheConfig.FilterScope.REQUEST);
    }

    @Override
    public HttpCacheConfig getCacheConfig(SlingHttpServletRequest request, HttpCacheConfig.FilterScope filterScope) throws HttpCacheConfigConflictException, HttpCacheRepositoryAccessException {

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
            } else if (filterScope.equals(cacheConfig.getFilterScope()) && cacheConfig.accepts(request)) {
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
        if (!isRequestDeliverableFromCacheAccordingToHandlingRules(request, response, cacheConfig, cacheContent)){
            return false;
        }

        prepareCachedResponse(response, cacheContent);
        return executeCacheContentDeliver(request, response, cacheContent);
    }



    @Override
    public HttpCacheServletResponseWrapper wrapResponse(SlingHttpServletRequest request, SlingHttpServletResponse
            response, HttpCacheConfig cacheConfig) throws HttpCacheDataStreamException,
            HttpCacheKeyCreationException, HttpCachePersistenceException {
        // Wrap the response to get the copy of the stream.
        // Temp sink for the duplicate stream is chosen based on the cache store configured at cache config.
        try {
            return new HttpCacheServletResponseWrapper(response, getCacheStore(cacheConfig).createTempSink());
        } catch (IOException e) {
            throw new HttpCacheDataStreamException(e);
        }
    }

    @Override
    public void cacheResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig) throws HttpCacheKeyCreationException, HttpCacheDataStreamException,
            HttpCachePersistenceException {

        // TODO - This can be made asynchronous to avoid performance penalty on response cache.

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

            // Persist in cache.
            if (isRequestCachableAccordingToHandlingRules(request, response, cacheConfig, cacheContent)) {
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
    public void invalidateCache(String path) throws HttpCachePersistenceException, HttpCacheKeyCreationException {
        // Find out all the cache config which has this path applicable for invalidation.
        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (cacheConfig.canInvalidate(path)) {
                // Execute custom rules.
                executeCustomRuleInvalidations(path, cacheConfig);
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
                JMX_HTTPCACHE_HANDLING_RULE,
                JMX_HTTPCACHE_HANDLING_RULE,
                new String[]{JMX_HTTPCACHE_HANDLING_RULE},
                new String[]{JMX_HTTPCACHE_HANDLING_RULE},
                new OpenType[]{SimpleType.STRING});

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(
                        JMX_PN_HTTPCACHE_HANDLING_RULES,
                        JMX_PN_HTTPCACHE_HANDLING_RULES,
                        cacheEntryType,
                        new String[]{JMX_HTTPCACHE_HANDLING_RULE}));
        // @formatter:on

        for (final Map.Entry<String, HttpCacheHandlingRule> entry : cacheHandlingRules.entrySet()) {
            final Map<String, Object> row = new HashMap<String, Object>();

            row.put(JMX_HTTPCACHE_HANDLING_RULE, entry.getValue().getClass().getName());
            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }

    @Override
    public TabularData getRegisteredHttpCacheConfigs() throws OpenDataException {
        // @formatter:off
        // Exposing all google guava stats.
        final CompositeType cacheEntryType = new CompositeType(
                JMX_PN_HTTPCACHE_CONFIG,
                JMX_PN_HTTPCACHE_CONFIG,
                new String[]{JMX_PN_ORDER, JMX_PN_OSGICOMPONENT },
                new String[]{ JMX_PN_ORDER, JMX_PN_OSGICOMPONENT },
                new OpenType[]{ SimpleType.INTEGER, SimpleType.STRING });

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(
                        JMX_PN_HTTPCACHE_CONFIGS,
                        JMX_PN_HTTPCACHE_CONFIGS,
                        cacheEntryType,
                        new String[]{ JMX_PN_OSGICOMPONENT }));

        // @formatter:on

        for (HttpCacheConfig cacheConfig : this.cacheConfigs) {
            final Map<String, Object> row = new HashMap<String, Object>();

            Map<String, Object> osgiConfig = cacheConfigConfigs.get(cacheConfig);

            row.put(JMX_PN_ORDER, cacheConfig.getOrder());
            row.put(JMX_PN_OSGICOMPONENT, (String) osgiConfig.get(Constants.SERVICE_PID));

            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }

    @Override
    public TabularData getRegisteredPersistenceStores() throws OpenDataException {
        // @formatter:off
        final CompositeType cacheEntryType = new CompositeType(
                JMX_PN_HTTPCACHE_STORE,
                JMX_PN_HTTPCACHE_STORE,
                new String[]{JMX_PN_HTTPCACHE_STORE},
                new String[]{JMX_PN_HTTPCACHE_STORE},
                new OpenType[]{ SimpleType.STRING});

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(
                        JMX_PN_HTTPCACHE_STORES,
                        JMX_PN_HTTPCACHE_STORES,
                        cacheEntryType,
                        new String[]{JMX_PN_HTTPCACHE_STORE}));
        // @formatter:on

        Enumeration<String> storeNames = cacheStoresMap.keys();
        while (storeNames.hasMoreElements()) {
            final String storeName = storeNames.nextElement();
            final Map<String, Object> row = new HashMap<String, Object>();

            row.put(JMX_PN_HTTPCACHE_STORE, storeName);
            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }

    private boolean isRequestCachableAccordingToHandlingRules(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig cacheConfig, CacheContent cacheContent){
        return checkOnHandlingRule(request, cacheConfig, rule -> rule.onResponseCache(request, response, cacheConfig, cacheContent), "Caching for request {} has been cancelled as per custom rule {}");
    }

    private boolean isRequestDeliverableFromCacheAccordingToHandlingRules(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig cacheConfig, CacheContent cacheContent) {
        return checkOnHandlingRule(request, cacheConfig, rule-> rule.onCacheDeliver(request, response, cacheConfig, cacheContent), "Cache cannot be delivered for the url {} honoring the rule {}");
    }

    private boolean checkOnHandlingRule(SlingHttpServletRequest request,  HttpCacheConfig cacheConfig, Function<HttpCacheHandlingRule, Boolean> check,String onFailLogMessage){
        for (final Map.Entry<String, HttpCacheHandlingRule> entry : cacheHandlingRules.entrySet()) {
            // Apply rule if it's a configured global or cache-config tied rule.
            if (globalCacheHandlingRulesPid.contains(entry.getKey()) || cacheConfig.acceptsRule(entry.getKey())) {
                HttpCacheHandlingRule rule = entry.getValue();
                if(!check.apply(rule)){
                    if (log.isDebugEnabled()) {
                        log.debug(onFailLogMessage, request
                                .getRequestURL(), rule.getClass().getName());
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private void prepareCachedResponse(SlingHttpServletResponse response, CacheContent cacheContent) {
        response.setStatus(cacheContent.getStatus());
        // Spool header info into the servlet response.
        for (String headerName : cacheContent.getHeaders().keySet()) {
            for (String headerValue : cacheContent.getHeaders().get(headerName)) {
                response.setHeader(headerName, headerValue);
            }
        }

        // Spool other attributes to the servlet response.
        response.setContentType(cacheContent.getContentType());
        response.setCharacterEncoding(cacheContent.getCharEncoding());
    }

    private boolean executeCacheContentDeliver(SlingHttpServletRequest request, SlingHttpServletResponse response, CacheContent cacheContent) throws HttpCacheDataStreamException {
        // Copy the cached data into the servlet output stream.
        try {
            serveCacheContentIntoResponse(response, cacheContent);

            if (log.isDebugEnabled()) {
                log.debug("Response delivered from cache for the url [ {} ]", request.getRequestURI());
            }
            return true;
        } catch (IOException e) {
            throw new HttpCacheDataStreamException("Unable to copy from cached data to the servlet output stream.");
        }
    }

    private void serveCacheContentIntoResponse(SlingHttpServletResponse response, CacheContent cacheContent)
            throws IOException {
        try {
            IOUtils.copy(cacheContent.getInputDataStream(), response.getOutputStream());
        } catch(IllegalStateException ex) {
            // in this case, either the writer has already been obtained or the response doesn't support getOutputStream()
            IOUtils.copy(cacheContent.getInputDataStream(), response.getWriter(), response.getCharacterEncoding());
        }
    }

    private void executeCustomRuleInvalidations(String path, HttpCacheConfig cacheConfig) throws HttpCachePersistenceException, HttpCacheKeyCreationException {
        for (final Map.Entry<String, HttpCacheHandlingRule> entry : cacheHandlingRules.entrySet()) {
            // Apply rule if it's a configured global or cache-config tied rule.
            if (globalCacheHandlingRulesPid.contains(entry.getKey()) || cacheConfig.acceptsRule(entry.getKey())) {
                HttpCacheHandlingRule rule = entry.getValue();
                if (rule.onCacheInvalidate(path)) {
                    getCacheStore(cacheConfig).invalidate(cacheConfig.buildCacheKey(path));
                } else {
                    log.debug("Cache invalidation rejected for path {} per custom rule {}", path, rule
                            .getClass().getName());
                }
            }
        }
    }
}
