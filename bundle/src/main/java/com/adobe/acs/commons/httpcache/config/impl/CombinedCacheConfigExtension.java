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
package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * Aggregates multiple cache config extensions into one.
 * This is useful when you need functionality of two or more extensions together.
 * Instead of duplicating and merging the multiple extensions / factories into a single class, this factory can be used to combine them.
 *
 *  Use as follows in your HTTP cache config to leverage multiple factories:
 *  cacheConfigExtensions.target="
 *             (|
 *                 (&amp;(service.factoryPid=com.adobe.acs.commons.httpcache.config.impl.RequestPathHttpCacheConfigExtension)(config.name=someConfig))
 *                 (&amp;(service.factoryPid=com.adobe.acs.commons.httpcache.config.impl.ResourceTypeHttpCacheConfigExtension)(config.name=someOtherConfig))
 *             )
 *           "
 */
@Component(
        service = {HttpCacheConfigExtension.class},
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                Constants.SERVICE_RANKING + ":Integer=" + Integer.MAX_VALUE,
                "webconsole.configurationFactory.nameHint=Config name: [ config.name ]"
        },
        reference = {
                @Reference(
                        name = "cacheConfigExtensions",
                        bind = "bindCacheConfigExtension",
                        unbind = "unbindCacheConfigExtension",
                        service = HttpCacheConfigExtension.class,
                        policy = ReferencePolicy.DYNAMIC,
                        cardinality = ReferenceCardinality.AT_LEAST_ONE)
        }
)
@Designate(
        ocd = CombinedCacheConfigExtension.Config.class,
        factory = true
)
public class CombinedCacheConfigExtension implements HttpCacheConfigExtension {
    private static final Logger log = LoggerFactory.getLogger(CombinedCacheConfigExtension.class);

    @ObjectClassDefinition(
            name = "ACS AEM Commons - HTTP Cache - Extension Combiner",
            description = "Aggregates multiple extensions into a single referencable extension."
    )
    public @interface Config {
        @AttributeDefinition(
                name = "HttpCacheConfigExtension service PIDs",
                description = "Service PIDs of target implementation of HttpCacheConfigExtensions to be combined and used."
        )
        String cacheConfigExtensions_target();

        @AttributeDefinition(
                name = "Require all extensions to accept",
                description = ""
        )
        boolean httpcache_config_extension_combiner_require_all_to_accept() default true;


        @AttributeDefinition(
                name = "Config Name"
        )
        String config_name() default EMPTY;
    }

    private RankedServices<HttpCacheConfigExtension> cacheConfigExtensions = new RankedServices<>(Order.ASCENDING);

    private Config cfg;

    @Override
    public boolean accepts(final SlingHttpServletRequest request, final HttpCacheConfig cacheConfig) throws HttpCacheRepositoryAccessException {
        if (!cfg.httpcache_config_extension_combiner_require_all_to_accept()) {
            for (final HttpCacheConfigExtension extension : cacheConfigExtensions) {
                if (extension.accepts(request, cacheConfig)) {
                    // Return true as long as AT LEAST one extension accepts.
                    log.debug("require one: extension {} accepting: {}", cfg.config_name(), extension.getClass().getName());
                    return true;
                }
            }
            return false;
        } else {
            for (final HttpCacheConfigExtension extension : cacheConfigExtensions) {
                if (!extension.accepts(request, cacheConfig)) {
                    // Return true as long as AT LEAST one extension accepts.
                    log.debug("require all: extension {} not accepting: {}", cfg.config_name(), extension.getClass().getName());
                    return false;
                }
            }
            return true;
        }
    }

    @Activate
    @Modified
    protected void activate(CombinedCacheConfigExtension.Config config) {
        cfg = config;
    }

    protected void bindCacheConfigExtension(HttpCacheConfigExtension extension, Map<String, Object> properties) {
        if (extension != this)
        {
            // Only accept extensions whose service.pid's are enumerated in the configuration.
            cacheConfigExtensions.bind(extension, properties);
        }
    }

    protected void unbindCacheConfigExtension(HttpCacheConfigExtension extension, Map<String, Object> properties) {
        if (extension != this) {
            cacheConfigExtensions.unbind(extension, properties);
        }
    }
}
