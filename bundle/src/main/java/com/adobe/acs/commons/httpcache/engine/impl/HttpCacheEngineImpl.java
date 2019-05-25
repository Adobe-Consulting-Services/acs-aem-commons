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

import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.engine.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.engine.impl.delegate.HttpCacheEngineBindingsDelegate;
import com.adobe.acs.commons.httpcache.engine.impl.delegate.HttpCacheEngineMBeanDelegate;
import com.adobe.acs.commons.httpcache.exception.HttpCacheException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheConfigConflictException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCachePersistenceException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.apache.commons.io.IOUtils;


import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default implementation for {@link HttpCacheEngine}. Binds multiple {@link HttpCacheConfig}. Multiple {@link
 * HttpCacheStore} also get bound to this.
 */
// @formatter:off
@Component(service = {DynamicMBean.class, HttpCacheEngine.class}, property = {
        "jmx.objectname" + "=" + "com.adobe.acs.httpcache:type=HTTP Cache Engine",
        "webconsole.configurationFactory.nameHint" + "=" + "Global handling rules: {httpcache.engine.cache-handling-rules.global}"
}, reference = {
        @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CONFIG,
                service = HttpCacheConfig.class,
                policy = org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC,
                cardinality = org.osgi.service.component.annotations.ReferenceCardinality.AT_LEAST_ONE),

        @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES,
                service = HttpCacheHandlingRule.class,
                policy = org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC,
                cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE),

        @Reference(name = HttpCacheEngineImpl.METHOD_NAME_TO_BIND_CACHE_STORE,
                service = HttpCacheStore.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.AT_LEAST_ONE)})
@Designate(ocd = HttpCacheEngineImpl.Config.class)
// @formatter:on
public class HttpCacheEngineImpl extends AnnotatedStandardMBean implements HttpCacheEngine, HttpCacheEngineMBean {


    @ObjectClassDefinition(name = "ACS AEM Commons - HTTP Cache - Engine",
            description = "Controlling service for http cache implementation.")
    public @interface Config {

        @AttributeDefinition(name = "Global HttpCacheHandlingRules",
                description = "List of Service pid of HttpCacheHandlingRule applicable for all cache configs.",
                defaultValue = {
                        "com.adobe.acs.commons.httpcache.rule.impl.CacheOnlyGetRequest",
                        "com.adobe.acs.commons.httpcache.rule.impl.CacheOnlyResponse200",
                        "com.adobe.acs.commons.httpcache.rule.impl.HonorCacheControlHeaders",
                        "com.adobe.acs.commons.httpcache.rule.impl.DoNotCacheZeroSizeResponse"
                })
        String[] httpcache_engine_cachehandling_rules_global();

        @AttributeDefinition(name = "Globally excluded response headers",
                description = "List of header keys (as regex statements) that should NOT be put in the cached response, to be served to the output."
        )
        String[] httpcache_engine_excluded_response_headers_global();

        @AttributeDefinition(name = "Globally excluded cookies",
                description = "List of cookie keys that should NOT be put in the cached response (Set-Cookie), to be served to the output."
        )
        String[] httpcache_engine_excluded_cookie_keys_global();
    }

    private static final Logger log = LoggerFactory.getLogger(HttpCacheEngineImpl.class);

    /** Method name that binds cache configs */
    static final String METHOD_NAME_TO_BIND_CONFIG = "httpCacheConfig";

    /** Method name that binds cache store */
    static final String METHOD_NAME_TO_BIND_CACHE_STORE = "httpCacheStore";

    /** Method name that binds cache handling rules */
    static final String METHOD_NAME_TO_BIND_CACHE_HANDLING_RULES = "httpCacheHandlingRule";

    // formatter:off
    static final String PROP_GLOBAL_CACHE_HANDLING_RULES_PID = "httpcache.engine.cache-handling-rules.global";
    private List<String> globalCacheHandlingRulesPid;

    static final String PROP_GLOBAL_RESPONSE_HEADER_EXCLUSIONS = "httpcache.engine.excluded.response.headers.global";
    private List<Pattern> globalHeaderExclusions;

    static final String PROP_GLOBAL_RESPONSE_COOKIE_KEYS_EXCLUSIONS = "httpcache.engine.excluded.cookie.keys.global";
    private List<String> globalCookieExclusions;

    // formatter:on

    @Reference
    private ThrottledTaskRunner throttledTaskRunner;

    private final HttpCacheEngineMBeanDelegate mBeanDelegate = new HttpCacheEngineMBeanDelegate();
    private final HttpCacheEngineBindingsDelegate bindingsDelegate = new HttpCacheEngineBindingsDelegate();
    //-------------------<OSGi specific methods>---------------//

    @Activate
    protected void activate(HttpCacheEngineImpl.Config config) {

        // PIDs of global cache handling rules.
        globalHeaderExclusions = ParameterUtil.toPatterns(config.httpcache_engine_excluded_response_headers_global());

        globalCacheHandlingRulesPid = new ArrayList<>(Arrays.asList(config.httpcache_engine_cachehandling_rules_global()));

        globalCookieExclusions = new ArrayList<>(Arrays.asList(config.httpcache_engine_excluded_cookie_keys_global()));

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
        for (final Map.Entry<String, HttpCacheHandlingRule> entry : bindingsDelegate.getCacheHandlingRules().entrySet()) {
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

        for (HttpCacheConfig cacheConfig : bindingsDelegate.getCacheConfigs()) {
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
            HttpCachePersistenceException {
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
            cacheConfig) {

        final HttpCacheServletResponseWrapper responseWrapper;
        if (response instanceof HttpCacheServletResponseWrapper) {
            responseWrapper = (HttpCacheServletResponseWrapper) response;
        } else {
            throw new AssertionError("Programming error.");
        }
        final Map<String, List<String>> extractedHeaders = extractHeaders(responseWrapper, cacheConfig);
        final int status = responseWrapper.getStatus();
        final String charEncoding = responseWrapper.getCharacterEncoding();
        final String contentType = responseWrapper.getContentType();

        throttledTaskRunner.scheduleWork(() -> {
            CacheContent cacheContent = null;
            // Construct the cache content.
            try {
                CacheKey cacheKey = cacheConfig.buildCacheKey(request);
                cacheContent = new CacheContent().build(responseWrapper, status, charEncoding, contentType, extractedHeaders);

                // Persist in cache.
                if (isRequestCachableAccordingToHandlingRules(request, response, cacheConfig, cacheContent)) {
                    getCacheStore(cacheConfig).put(cacheKey, cacheContent);
                    log.debug("Response for the URI cached - {}", request.getRequestURI());
                }
            } catch (HttpCacheException e) {
                log.error("Error storing http response in httpcache", e);
            } finally {
                // Close the temp sink input stream.
                if (null != cacheContent) {
                    IOUtils.closeQuietly(cacheContent.getInputDataStream());
                }
            }
        });

    }


    @Override
    public boolean isPathPotentialToInvalidate(String path) {

        // Check all the configs to see if this path is of interest.
        for (HttpCacheConfig config : bindingsDelegate.getCacheConfigs()) {
            if (config.canInvalidate(path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void invalidateCache(String path) throws HttpCachePersistenceException, HttpCacheKeyCreationException {
        // Find out all the cache config which has this path applicable for invalidation.
        for (HttpCacheConfig cacheConfig : bindingsDelegate.getCacheConfigs()) {
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
        if (bindingsDelegate.getCacheStoresMap().containsKey(cacheConfig.getCacheStoreName())) {
            return bindingsDelegate.getCacheStoresMap().get(cacheConfig.getCacheStoreName());
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
        return mBeanDelegate.getRegisteredHttpCacheRules(bindingsDelegate.getCacheHandlingRules());
    }

    @Override
    public TabularData getRegisteredHttpCacheConfigs() throws OpenDataException {
        return mBeanDelegate.getRegisteredHttpCacheConfigs(bindingsDelegate.getCacheConfigs(), bindingsDelegate.getCacheConfigConfigs());
    }

    @Override
    public TabularData getRegisteredPersistenceStores() throws OpenDataException {
        return mBeanDelegate.getRegisteredPersistenceStores(bindingsDelegate.getCacheStoresMap());
    }

    /**
     * Binds cache config. Cache config could come and go at run time.
     *
     * @param cacheConfig
     * @param configs
     */
    protected void bindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> configs) {
        bindingsDelegate.bindHttpCacheConfig(cacheConfig, configs);
    }

    /**
     * Unbinds cache config.
     *
     * @param cacheConfig
     * @param config
     */
    protected void unbindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> config) {
        bindingsDelegate.unbindHttpCacheConfig(cacheConfig);
    }

    /**
     * Binds cache store implementation
     *
     * @param cacheStore
     */
    protected void bindHttpCacheStore(final HttpCacheStore cacheStore) {
        bindingsDelegate.bindHttpCacheStore(cacheStore);
    }

    /**
     * Unbinds cache store.
     *
     * @param cacheStore
     */
    protected void unbindHttpCacheStore(final HttpCacheStore cacheStore) {
        bindingsDelegate.unbindHttpCacheStore(cacheStore);
    }

    /**
     * Binds cache handling rule
     *
     * @param cacheHandlingRule
     * @param properties
     */
    protected void bindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String, Object>
            properties) {
        bindingsDelegate.bindHttpCacheHandlingRule(cacheHandlingRule, properties);
    }

    /**
     * Unbinds handling rule.
     *
     * @param cacheHandlingRule
     * @param configs
     */
    protected void unbindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String,
            Object> configs) {
        bindingsDelegate.unbindHttpCacheHandlingRule(cacheHandlingRule, configs);
    }

    private boolean isRequestCachableAccordingToHandlingRules(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig cacheConfig, CacheContent cacheContent){
        return checkOnHandlingRule(request, cacheConfig, rule -> rule.onResponseCache(request, response, cacheConfig, cacheContent), "Caching for request {} has been cancelled as per custom rule {}");
    }

    private boolean isRequestDeliverableFromCacheAccordingToHandlingRules(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig cacheConfig, CacheContent cacheContent) {
        return checkOnHandlingRule(request, cacheConfig, rule-> rule.onCacheDeliver(request, response, cacheConfig, cacheContent), "Cache cannot be delivered for the url {} honoring the rule {}");
    }

    private boolean checkOnHandlingRule(SlingHttpServletRequest request,  HttpCacheConfig cacheConfig, Function<HttpCacheHandlingRule, Boolean> check,String onFailLogMessage){
        for (final Map.Entry<String, HttpCacheHandlingRule> entry : bindingsDelegate.getCacheHandlingRules().entrySet()) {
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
        if(HttpCacheServletResponseWrapper.ResponseWriteMethod.OUTPUTSTREAM.equals(cacheContent.getWriteMethod())){
            try {
                IOUtils.copy(cacheContent.getInputDataStream(), response.getOutputStream());
            } catch(IllegalStateException ex) {
                // in this case, either the writer has already been obtained or the response doesn't support getOutputStream()
                IOUtils.copy(cacheContent.getInputDataStream(), response.getWriter(), response.getCharacterEncoding());
            }
        }else{
            IOUtils.copy(cacheContent.getInputDataStream(), response.getWriter(), response.getCharacterEncoding());
        }
    }

    private void executeCustomRuleInvalidations(String path, HttpCacheConfig cacheConfig) throws HttpCachePersistenceException, HttpCacheKeyCreationException {
        for (final Map.Entry<String, HttpCacheHandlingRule> entry : bindingsDelegate.getCacheHandlingRules().entrySet()) {
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

    private Map<String, List<String>> extractHeaders(SlingHttpServletResponse response, HttpCacheConfig cacheConfig) {

        List<Pattern> excludedHeaders = Stream.concat(globalHeaderExclusions.stream(), cacheConfig.getExcludedResponseHeaderPatterns().stream())
                .collect(Collectors.toList());

        List<String> excludedCookieKeys = Stream.concat(globalCookieExclusions.stream(), cacheConfig.getExcludedCookieKeys().stream())
                .collect(Collectors.toList());

        return response.getHeaderNames().stream()
                .filter(headerName -> excludedHeaders.stream()
                        .noneMatch(pattern -> pattern.matcher(headerName).matches())
                ).collect(
                        Collectors.toMap(headerName -> headerName, headerName -> filterCookieHeaders(response, excludedCookieKeys, headerName)
                        ));
    }

    private List<String> filterCookieHeaders(SlingHttpServletResponse response, List<String> excludedCookieKeys, String headerName) {
        if(!headerName.equals("Set-Cookie")){
            return new ArrayList<>(response.getHeaders(headerName));
        }
        //for set-cookie we apply another exclusion filter.
        return new ArrayList<>(response.getHeaders(headerName)).stream().filter(
                header -> {
                    String key;
                    if(header.startsWith("__Host-")){
                        key = StringUtils.removeStart( header,"__Host-");
                    }else if(header.startsWith("__Secure-")){
                        key = StringUtils.removeStart( header,"__Secure-");
                    }else{
                        key = header;
                    }
                    key = StringUtils.substringBefore(key, "=");

                    return !excludedCookieKeys.contains(key);
                }
        ).collect(Collectors.toList());
    }


}
