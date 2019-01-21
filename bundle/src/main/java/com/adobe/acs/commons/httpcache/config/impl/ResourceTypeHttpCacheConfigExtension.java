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
package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.config.impl.keys.ResourcePathCacheKey;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.commons.jcr.JcrConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation for custom cache config extension and associated cache key creation based on resource type. This cache
 * config extension accepts the http request only if at least one of the configured patterns matches the resource type
 * of the request's resource.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service= {HttpCacheConfigExtension.class,CacheKeyFactory.class},
        property= {
              "webconsole.configurationFactory.nameHint" + "=" + "Allowed resource types: {httpcache.config.extension.resource-types.allowed}"
        }
)
@Designate(ocd=ResourceTypeHttpCacheConfigExtension.Config.class, factory=true)
public class ResourceTypeHttpCacheConfigExtension implements HttpCacheConfigExtension, CacheKeyFactory {
    private static final Logger log = LoggerFactory.getLogger(ResourceTypeHttpCacheConfigExtension.class);

    @ObjectClassDefinition(name= "ACS AEM Commons - HTTP Cache - ResourceType based extension for HttpCacheConfig and CacheKeyFactory.")
    public @interface Config {
        @AttributeDefinition(name = "Allowed paths",
                description = "Regex of content paths that can be cached.")
        String[] httpcache_config_extension_paths_allowed() default {};

        @AttributeDefinition(name = "Allowed resource types",
                description = "Regex of resource types that can be cached.")
        String[] httpcache_config_extension_resourcetypes_allowed() default {};

        @AttributeDefinition(name = "Check RT of ./jcr:content?",
                description = "Should the resourceType check be applied to ./jcr:content ?",
                defaultValue = "false")
        boolean httpcacheconfig_extension_resourcetypes_page_content() default false;

        @AttributeDefinition(name = "Config Name")
        String configName() default StringUtils.EMPTY;
    }

    // Custom cache config attributes
    private List<Pattern> pathPatterns;
    private List<Pattern> resourceTypePatterns;
    private boolean checkContentResourceType;

    //-------------------------<HttpCacheConfigExtension methods>

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
            HttpCacheRepositoryAccessException {

        if (log.isDebugEnabled()) {
            log.debug("ResourceType acceptance check on [ {} ~> {} ]", request.getResource(), request.getResource().getResourceType());
        }

        for (Pattern pattern : pathPatterns) {
            Matcher m = pattern.matcher(request.getResource().getPath());
            if (!m.matches()) {
                return false;
            }
        }
        // Passed the content path test..

        Resource candidateResource = request.getResource();
        if (checkContentResourceType) {
            candidateResource = candidateResource.getChild(JcrConstants.JCR_CONTENT);
            if (candidateResource == null) {
                return false;
            }
        }
        log.debug("ResourceHttpCacheConfigExtension checking for resource type matches");
        // Match resource types.
        for (Pattern pattern : resourceTypePatterns) {
            Matcher m = pattern.matcher(candidateResource.getResourceType());

            if (m.matches()) {
                if (log.isTraceEnabled()) {
                    log.trace("ResourceHttpCacheConfigExtension accepts request [ {} ]", candidateResource);
                }
                return true;
            }
        }

        return false;
    }

    //-------------------------<CacheKeyFactory methods>

    @Override
    public CacheKey build(final SlingHttpServletRequest slingHttpServletRequest, final HttpCacheConfig cacheConfig)
            throws HttpCacheKeyCreationException {
        return new ResourcePathCacheKey(slingHttpServletRequest, cacheConfig);
    }

    @Override
    public CacheKey build(final String resourcePath, final HttpCacheConfig cacheConfig)
            throws HttpCacheKeyCreationException {
        return new ResourcePathCacheKey(resourcePath, cacheConfig);
    }

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {

        // Check if key is instance of ResourcePathCacheKey.
        if (!(key instanceof ResourcePathCacheKey)) {
            return false;
        }
        // Validate if key request uri can be constructed out of uri patterns in cache config.
        return new ResourcePathCacheKey(key.getUri(), cacheConfig).equals(key);
    }

    //-------------------------<OSGi Component methods>

    @Activate
    protected void activate(Config config) {
        resourceTypePatterns = ParameterUtil.toPatterns(config.httpcache_config_extension_resourcetypes_allowed());
        pathPatterns = ParameterUtil.toPatterns(config.httpcache_config_extension_paths_allowed());
        checkContentResourceType = config.httpcacheconfig_extension_resourcetypes_page_content();

        log.info("ResourceHttpCacheConfigExtension activated/modified.");
    }
}
