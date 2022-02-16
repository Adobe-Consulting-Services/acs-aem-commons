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
package com.adobe.acs.commons.httpcache.engine.impl.delegate;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigComparator;
import com.adobe.acs.commons.httpcache.engine.impl.HttpCacheEngineImpl;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * HttpCacheEngineBindingsDelegate
 * <p>
 * Handles the bulk of the bindings logic for the HttpCacheEngineImpl
 * </p>
 */
public class HttpCacheEngineBindingsDelegate {

    private static final Logger log = LoggerFactory.getLogger(HttpCacheEngineImpl.class);

    /** Thread safe list to contain the registered HttpCacheConfig references. */
    private CopyOnWriteArrayList<HttpCacheConfig> cacheConfigs = new CopyOnWriteArrayList<>();

    /** Thread safe hash map to contain the registered cache store references. */
    private final ConcurrentHashMap<String, HttpCacheStore> cacheStoresMap = new ConcurrentHashMap<>();

    /** Thread safe map to contain the registered HttpCacheHandlingRule references. */
    private final ConcurrentHashMap<String, HttpCacheHandlingRule> cacheHandlingRules = new
            ConcurrentHashMap<>();

    /** Thread safe list containing the OSGi configurations for the registered httpCacheConfigs. Used only for mbean.*/
    private final ConcurrentHashMap<HttpCacheConfig, Map<String, Object>> cacheConfigConfigs = new
            ConcurrentHashMap<>();

    /**
     * Binds cache config. Cache config could come and go at run time.
     *
     * @param cacheConfig
     * @param configs
     */
    public void bindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> configs) {

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
     */
    public void unbindHttpCacheConfig(final HttpCacheConfig cacheConfig) {

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
     */
    public void bindHttpCacheStore(final HttpCacheStore cacheStore) {
        final String cacheStoreType = cacheStore.getStoreType();
        if (cacheStoreType != null && cacheStoresMap.putIfAbsent(cacheStoreType, cacheStore) == null) {
            log.debug("HTTP Cache Store [ {} -> ADDED ] for a total of [ {} ]", cacheStore.getStoreType(), cacheStoresMap.size());
        }
    }



    /**
     * Unbinds cache store.
     *
     * @param cacheStore
     */
    public void unbindHttpCacheStore(final HttpCacheStore cacheStore) {
        final String cacheStoreType = cacheStore.getStoreType();
        if (cacheStoreType != null && cacheStoresMap.remove(cacheStoreType) != null) {
            log.debug("HTTP Cache Store [ {} -> REMOVED ] for a total of [ {} ]", cacheStore.getStoreType(), cacheStoresMap.size());
        }
    }

    /**
     * Binds cache handling rule
     *
     * @param cacheHandlingRule
     * @param properties
     */
    public void bindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String, Object>
            properties) {

        // Get the service pid and make it as key.
        if (cacheHandlingRules.putIfAbsent(getServicePid(properties), cacheHandlingRule) == null) {
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
    public void unbindHttpCacheHandlingRule(final HttpCacheHandlingRule cacheHandlingRule, final Map<String,
            Object> configs) {

        if (cacheHandlingRules.remove(getServicePid(configs) ) != null) {
            log.debug("Cache handling rule removed - {}.", cacheHandlingRule.getClass().getName());
            log.debug("Total number of cache handling rules available after removal: {}", cacheHandlingRules.size());
        }
    }

    public List<HttpCacheConfig> getCacheConfigs() {
        return Collections.unmodifiableList(cacheConfigs);
    }

    public Map<String, HttpCacheStore> getCacheStoresMap() {
        return cacheStoresMap;
    }

    public Map<String, HttpCacheHandlingRule> getCacheHandlingRules() {
        return cacheHandlingRules;
    }

    public Map<HttpCacheConfig, Map<String, Object>> getCacheConfigConfigs() {
        return cacheConfigConfigs;
    }

    private String getServicePid(Map<String, Object> configs) {
        String servicePid = PropertiesUtil.toString(configs.get("service.pid"), StringUtils.EMPTY);

        if(StringUtils.isBlank(servicePid)){
            servicePid =PropertiesUtil.toString(configs.get("component.name"), StringUtils.EMPTY);
        }
        return servicePid;
    }

}
