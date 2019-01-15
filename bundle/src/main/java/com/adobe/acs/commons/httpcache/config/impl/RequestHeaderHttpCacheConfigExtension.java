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

import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueMapWrapperBuilder;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.RequestHeaderKeyValueWrapperBuilder;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Map;
import java.util.Set;

/**
 * RequestHeaderHttpCacheConfigExtension
 * <p>
 * This extension on the HTTP cache allows for specific header combinations to create separated cache entries.
  * </p>
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, service = {HttpCacheConfigExtension.class, CacheKeyFactory.class})
@Designate(ocd = KeyValueConfig.class, factory = true)
public class RequestHeaderHttpCacheConfigExtension extends AbstractKeyValueExtension implements CacheKeyFactory, HttpCacheConfigExtension {

    public static final String KEY_TOSTRING_REPRESENTATION = "RequestHeaders";

    @Override
    protected String getKeyToStringRepresentation() {
        return KEY_TOSTRING_REPRESENTATION;
    }

    @Override
    protected KeyValueMapWrapperBuilder getBuilder(SlingHttpServletRequest request, Set<String> allowedKeys, Map<String, String> allowedValues) {
        return new RequestHeaderKeyValueWrapperBuilder(allowedKeys, allowedValues, request);
    }

    @Activate
    public void activate(KeyValueConfig config){
        this.init(config);
    }
}
