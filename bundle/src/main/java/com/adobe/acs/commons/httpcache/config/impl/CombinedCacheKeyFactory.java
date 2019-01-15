/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.config.impl.keys.CombinedCacheKey;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Aggregates multiple cache key factories extensions into 1.
 * This is useful when you need differentiation of 2 cache keys together.
 * Instead of duplicating and merging the 2 extensions / factories into 1 class, you can leverage this class.
 * It will use existing cache key factories to create a key for each one, and put them in a list.
 */
@Component(
        service = {CacheKeyFactory.class},
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                Constants.SERVICE_RANKING + ":Integer=" + Integer.MIN_VALUE
        },
        reference = {
                @Reference(
                        name = "cacheKeyFactory",
                        bind = "bindCacheKeyFactory",
                        unbind = "unbindCacheKeyFactory",
                        service = CacheKeyFactory.class,
                        policy = ReferencePolicy.DYNAMIC,
                        cardinality = ReferenceCardinality.MULTIPLE)
        }

)
@Designate(ocd = CombinedCacheKeyFactory.Config.class, factory = true)
public class CombinedCacheKeyFactory implements CacheKeyFactory {


    @ObjectClassDefinition(name = "ACS AEM Commons - HTTP Cache - Combined extension for HttpCacheConfig and CacheKeyFactory.",
            description = "Aggregates multiple extensions into 1")
    public @interface Config {

        @AttributeDefinition(name = "Config Name")
        String configName() default StringUtils.EMPTY;

        @AttributeDefinition(name = "CacheKeyFactory service pids",
                description = "Service pid(s) of target implementation of CacheKeyFactory to be used."
        )
        String cacheKeyFactory_target();

    }

    private static final Logger log = LoggerFactory.getLogger(CombinedCacheKeyFactory.class);

    private String configName;
    private String cacheKeyFactoriesTarget;

    private RankedServices<CacheKeyFactory> cacheKeyFactories = new RankedServices<>(Order.ASCENDING);

    @Override
    public CacheKey build(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {
        return new CombinedCacheKey(request, cacheConfig, cacheKeyFactories.getList());
    }

    @Override
    public CacheKey build(String resourcePath, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {
        return new CombinedCacheKey(resourcePath, cacheConfig, cacheKeyFactories.getList());
    }

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {
        if (!(key instanceof CombinedCacheKey)) {
            return false;
        }

        return new CombinedCacheKey(key.getUri(), cacheConfig, cacheKeyFactories.getList()).equals(key);
    }

    protected void bindCacheKeyFactory(CacheKeyFactory factory, Map<String, Object> properties) {
        if (factory != this) {
            cacheKeyFactories.bind(factory, properties);
        } else {
            log.error("Invalid key factory LDAP target string! Self is target(ed)! Breaking up infinite loop. Target: {}", this.cacheKeyFactoriesTarget);
        }
    }

    protected void unbindCacheKeyFactory(CacheKeyFactory factory, Map<String, Object> properties) {
        if (factory != this) {
            cacheKeyFactories.unbind(factory, properties);
        }
    }

    @Activate
    @Modified
    protected void activate(CombinedCacheKeyFactory.Config config) {
        this.configName = config.configName();
        this.cacheKeyFactoriesTarget = config.cacheKeyFactory_target();
    }

}
