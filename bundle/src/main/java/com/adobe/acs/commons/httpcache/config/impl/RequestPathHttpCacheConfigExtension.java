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
import com.adobe.acs.commons.httpcache.config.impl.keys.RequestPathCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {HttpCacheConfigExtension.class, CacheKeyFactory.class},
        property = {
                Constants.SERVICE_RANKING + ":Integer=40"
        })
@Designate(ocd = RequestPathHttpCacheConfigExtension.Config.class, factory = true)
public class RequestPathHttpCacheConfigExtension implements HttpCacheConfigExtension, CacheKeyFactory {

    @ObjectClassDefinition(
            name = "ACS AEM Commons - HTTP Cache - Extension - Request Path",
            description = "Extension for the ACS commons HTTP Cache. Based on request path info ( resource path, selectors, extensions )"
    )
    public @interface Config {

        @AttributeDefinition(
                name = "Configuration Name",
                description = "The unique identifier of this extension"
        )
        String config_name() default "";

        @AttributeDefinition(
                name = "Resource path patterns",
                description = "List of resource path patterns (regex) that will be valid for caching"
        )
        String[] httpcache_config_extension_paths_allowed();

        @AttributeDefinition(
                name = "Selector patterns",
                description = "List of selector patterns (regex) that will be valid for caching"
        )
        String[] httpcache_config_extension_selectors_allowed();

        @AttributeDefinition(
                name = "Extension patterns",
                description = "List of extension patterns (regex) that will be valid for caching"
        )
        String[] httpcache_config_extension_extensions_allowed();

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Config name: [ {config.name}  RequestPath: [ {httpcache.config.extension.paths.allowed}]";

    }

    private static final Logger log = LoggerFactory.getLogger(RequestPathHttpCacheConfigExtension.class);

    protected List<Pattern> resourcePathPatterns;
    protected List<Pattern> selectorPatterns;
    protected List<Pattern> extensionPatterns;

    protected String configName;

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {

        RequestPathInfo requestPathInfo = request.getRequestPathInfo();

        boolean match = matches(resourcePathPatterns, requestPathInfo.getResourcePath())
                && matches(selectorPatterns, requestPathInfo.getSelectorString())
                && matches(extensionPatterns, requestPathInfo.getExtension());

        log.debug("Extension {} : Passed : {} for {}", configName, match, requestPathInfo.getResourcePath());

        return match;
    }


    protected boolean matches(List<Pattern> patternList, String query) {
        if (isEmpty(patternList)) {
            log.debug("Extension {} : Non defined patternList {} : skipping check for query: {}", configName, patternList, query);
            return true;
        } else if (CollectionUtils.isNotEmpty(patternList) && StringUtils.isNotBlank(query)) {
            for (Pattern pattern : patternList) {
                if (pattern.matcher(query).find()) {
                    log.debug("Extension {} : Passed all patterns: {} for query: {}", configName, patternList, query);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public CacheKey build(SlingHttpServletRequest slingHttpServletRequest, HttpCacheConfig cacheConfig) {
        return new RequestPathCacheKey(slingHttpServletRequest, cacheConfig);
    }


    @Override
    public CacheKey build(String resourcePath, HttpCacheConfig httpCacheConfig) {
        return new RequestPathCacheKey(resourcePath, httpCacheConfig);
    }

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) {

        // Check if key is instance of GroupCacheKey.
        if (!(key instanceof RequestPathCacheKey)) {
            return false;
        }

        RequestPathCacheKey thatKey = (RequestPathCacheKey) key;

        return new RequestPathCacheKey(thatKey.getUri(), cacheConfig).equals(key);
    }

    @Activate
    protected void activate(RequestPathHttpCacheConfigExtension.Config config) {
        this.configName = config.config_name();
        this.resourcePathPatterns = compileToPatterns(config.httpcache_config_extension_paths_allowed());
        this.extensionPatterns = compileToPatterns(config.httpcache_config_extension_extensions_allowed());
        this.selectorPatterns = compileToPatterns(config.httpcache_config_extension_selectors_allowed());
    }

    protected List<Pattern> compileToPatterns(String[] regexes) {
        if (ArrayUtils.isEmpty(regexes)) {
            return emptyList();
        }

        List<Pattern> patterns = new ArrayList<>();
        for (String regex : regexes) {
            if (StringUtils.isNotBlank(regex)) {
                patterns.add(Pattern.compile(regex));
            }
        }

        return patterns;
    }

}
