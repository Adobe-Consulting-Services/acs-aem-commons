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
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
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
 * Aggregates multiple cache config extensions into 1.
 * This is useful when you need functionality of 2 extensions together.
 * Instead of duplicating and merging the 2 extensions / factories into 1 class, you can leverage this class.
 */
@Component(
        service = {CacheKeyFactory.class, HttpCacheConfigExtension.class},
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                Constants.SERVICE_RANKING + ":Integer=" + Integer.MIN_VALUE
        },
        reference = {
            @Reference(
                    name = "cacheConfigExtension",
                    bind = "bindCacheConfigExtension",
                    unbind = "unbindCacheConfigExtension",
                    service = HttpCacheConfigExtension.class,
                    policy = ReferencePolicy.DYNAMIC,
                    cardinality = ReferenceCardinality.AT_LEAST_ONE)
        }

)
@Designate(ocd=CombinedCacheConfigExtension.Config.class,factory=true)
public class CombinedCacheConfigExtension implements HttpCacheConfigExtension {

    @ObjectClassDefinition(name = "ACS AEM Commons - HTTP Cache - Combined extension for HttpCacheConfig and CacheKeyFactory.",
            description = "Aggregates multiple extensions into 1")
    public @interface Config {

        String DEFAULT_EXTENSION_TARGET = "(|(service.factoryPid=com.adobe.acs.commons.httpcache.config.impl.GroupHttpCacheConfigExtension)(configName=unique-confg-name-of-extension)(((service.factoryPid=com.adobe.acs.commons.httpcache.config.impl.ResourceTypeHttpCacheConfigExtension)(configName=unique-confg-name-of-extension))";

        @AttributeDefinition(name = "Config Name")
        String configName() default StringUtils.EMPTY;

        @AttributeDefinition(
                name = "HttpCacheConfigExtension service pids",
                description = "Service pid of target implementation of HttpCacheConfigExtension to be used. Example - "
                        + "(service.pid=" + DEFAULT_EXTENSION_TARGET + ")."
                        + " Optional parameter.",
                defaultValue = DEFAULT_EXTENSION_TARGET)
        String cacheConfigExtension_target() default DEFAULT_EXTENSION_TARGET;

    }

    private static final Logger log = LoggerFactory.getLogger(CombinedCacheConfigExtension.class);
    private String configName;

    private RankedServices<HttpCacheConfigExtension> cacheConfigExtensions = new RankedServices<>(Order.ASCENDING);
    private String cacheConfigExtensionsTarget;

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws HttpCacheRepositoryAccessException {

        for(HttpCacheConfigExtension extension: cacheConfigExtensions){
            if(!extension.accepts(request, cacheConfig)){
                return false;
            }
        }

        return true;
    }

    @Activate
    @Modified
    protected void activate(CombinedCacheConfigExtension.Config config) {
        this.configName = config.configName();
        this.cacheConfigExtensionsTarget = config.cacheConfigExtension_target();
    }



    protected void bindCacheConfigExtension(HttpCacheConfigExtension extension, Map<String,Object> properties){
        if(extension != this){
            cacheConfigExtensions.bind(extension, properties);
        }else{
            log.error("Invalid http cache config LDAP target string! Self is target(ed)! Breaking up infinite loop. Target: {}", this.cacheConfigExtensionsTarget);
        }
    }

    protected void unbindCacheConfigExtension(HttpCacheConfigExtension extension, Map<String,Object> properties){
        if(extension != this) {
            cacheConfigExtensions.unbind(extension, properties);
        }
    }


}
