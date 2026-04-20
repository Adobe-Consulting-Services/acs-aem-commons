/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.httpcache.engine.impl.delegate;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.osgi.framework.Constants;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * HttpCacheEngineMBeanDelegate
 * <p>
 * Handles the bulk of the mbean logic for the HttpCacheEngineImpl.
 * </p>
 *
 */
public class HttpCacheEngineMBeanDelegate  {

    static final String JMX_PN_ORDER = "Order";
    static final String JMX_PN_STORE_TYPE = "Type";
    static final String JMX_PN_OSGICOMPONENT = "OSGi Component";
    static final String JMX_PN_HTTPCACHE_CONFIGS = "HTTP Cache Configs";
    static final String JMX_PN_HTTPCACHE_CONFIG = "HTTP Cache Config";
    static final String JMX_PN_HTTPCACHE_STORE = "HTTP Cache Store";
    static final String JMX_PN_HTTPCACHE_STORES = "HTTP Cache Stores";
    static final String JMX_HTTPCACHE_HANDLING_RULE = "HTTP Cache Handling Rule";
    static final String JMX_PN_HTTPCACHE_HANDLING_RULES = "HTTP Cache Handling Rules";

    public TabularData getRegisteredHttpCacheRules(Map<String, HttpCacheHandlingRule> cacheHandlingRules) throws OpenDataException {
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


    public TabularData getRegisteredHttpCacheConfigs(List<HttpCacheConfig> configs, Map<HttpCacheConfig, Map<String, Object>> cacheConfigConfigs) throws OpenDataException {
        // @formatter:off
        // Exposing all google guava stats.
        final CompositeType cacheEntryType = new CompositeType(
                JMX_PN_HTTPCACHE_CONFIG,
                JMX_PN_HTTPCACHE_CONFIG,
                new String[]{JMX_PN_ORDER, JMX_PN_STORE_TYPE, JMX_PN_OSGICOMPONENT },
                new String[]{ JMX_PN_ORDER, JMX_PN_STORE_TYPE, JMX_PN_OSGICOMPONENT },
                new OpenType[]{ SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING });

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(
                        JMX_PN_HTTPCACHE_CONFIGS,
                        JMX_PN_HTTPCACHE_CONFIGS,
                        cacheEntryType,
                        new String[]{ JMX_PN_OSGICOMPONENT }));

        // @formatter:on

        for (HttpCacheConfig cacheConfig : configs) {
            final Map<String, Object> row = new HashMap<String, Object>();

            Map<String, Object> osgiConfig = cacheConfigConfigs.get(cacheConfig);

            row.put(JMX_PN_ORDER, cacheConfig.getOrder());
            row.put(JMX_PN_STORE_TYPE, cacheConfig.getCacheStoreName());
            row.put(JMX_PN_OSGICOMPONENT, osgiConfig.get(Constants.SERVICE_PID));

            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }

        return tabularData;
    }


    public TabularData getRegisteredPersistenceStores(Map<String, HttpCacheStore> cacheStoresMap) throws OpenDataException {
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

        for(Iterator<Map.Entry<String, HttpCacheStore>> entrySet = cacheStoresMap.entrySet().iterator(); entrySet.hasNext();){

            Map.Entry<String, HttpCacheStore> entry = entrySet.next();
            final String storeName = entry.getKey();
            final Map<String, Object> row = new HashMap<>();

            row.put(JMX_PN_HTTPCACHE_STORE, storeName);
            tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        }

        return tabularData;
    }

}
