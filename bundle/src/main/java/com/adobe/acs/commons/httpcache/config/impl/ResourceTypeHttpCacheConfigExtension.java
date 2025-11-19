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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.config.impl.keys.ResourcePathCacheKey;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.commons.jcr.JcrConstants;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation for custom cache config extension and associated cache key creation based on resource type. This cache
 * config extension accepts the http request only if at least one of the configured patterns matches the resource type
 * of the request's resource.
 */
@Component(
    property = "webconsole.configurationFactory.nameHint=Allowed resource types: [ {httpcache.config.extension.resource-types.allowed} ] Config name: [ {config.name} ]",
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ResourceTypeHttpCacheConfigExtension implements HttpCacheConfigExtension, CacheKeyFactory {
    private static final Logger log = LoggerFactory.getLogger(ResourceTypeHttpCacheConfigExtension.class);

    // Custom cache config attributes
    @Property(label = "Allowed paths",
            description = "Regex of content paths that can be cached.",
            unbounded = PropertyUnbounded.ARRAY)
    static final String PROP_PATHS = "httpcache.config.extension.paths.allowed";
    private List<Pattern> pathPatterns;

    @Property(label = "Allowed resource types",
            description = "Regex of resource types that can be cached.",
            unbounded = PropertyUnbounded.ARRAY)
    static final String PROP_RESOURCE_TYPES = "httpcache.config.extension.resource-types.allowed";
    private List<Pattern> resourceTypePatterns;

    @Property(label = "Check RT of ./jcr:content?",
            description = "Should the resourceType check be applied to ./jcr:content ?",
            boolValue = false)
    public static final String PROP_CHECK_CONTENT_RESOURCE_TYPE = "httpcache.config.extension.resource-types.page-content";
    private boolean checkContentResourceType;


    @Property(label = "Check resourceSuperType",
            description = "Should the resourceType check check super Types?",
            boolValue = false)
    static final String PROP_CHECK_RESOURCE_SUPER_TYPE = "httpcache.config.extension.resource-types.superType";
    private boolean checkResourceSuperType;

    @Property(label = "Config Name",
            description = "")
    static final String PROP_CONFIG_NAME = "config.name";
    private String configName;

    //-------------------------<HttpCacheConfigExtension methods>

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
            HttpCacheRepositoryAccessException {

        if (log.isDebugEnabled()) {
            log.debug("ResourceHttpCacheConfigExtension {} : ResourceType acceptance check on [ {} ~> {} ]", configName, request.getResource(), request.getResource().getResourceType());
        }
  
        if (!checkContentPath(request.getResource().getPath())) {
          return false;
        }
        
        // Passed the content path test..

        Resource candidateResource = request.getResource();
        if (checkContentResourceType) {
            candidateResource = candidateResource.getChild(JcrConstants.JCR_CONTENT);
            if (candidateResource == null) {
                return false;
            }
        }
        log.debug("ResourceHttpCacheConfigExtension {} :  checking for resource type matches", configName);
        // Match resource types.
        return checkResourceType(candidateResource);
    }
    
    private boolean checkContentPath(String contentPath) {
        return pathPatterns.stream().anyMatch( pattern -> pattern.matcher(contentPath).matches());
    }

    private boolean checkResourceType(Resource candidateResource) {
        if(checkResourceSuperType){
            return resourceTypePatterns.stream()
                    .anyMatch( pattern -> candidateResource.getResourceResolver().isResourceType(candidateResource, pattern.pattern()));
        }else{
            return resourceTypePatterns.stream()
                    .anyMatch( pattern -> pattern.matcher(candidateResource.getResourceType()).matches());
        }
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
    protected void activate(Map<String, Object> configs) {
        resourceTypePatterns = ParameterUtil.toPatterns(PropertiesUtil.toStringArray(configs.get(PROP_RESOURCE_TYPES), new String[]{}));
        pathPatterns = ParameterUtil.toPatterns(PropertiesUtil.toStringArray(configs.get(PROP_PATHS), new String[]{}));
        checkContentResourceType = PropertiesUtil.toBoolean(configs.get(PROP_CHECK_CONTENT_RESOURCE_TYPE), false);
        checkResourceSuperType = PropertiesUtil.toBoolean(configs.get(PROP_CHECK_RESOURCE_SUPER_TYPE), false);
        configName = PropertiesUtil.toString(configs.get(PROP_CONFIG_NAME), "");
        log.info("ResourceHttpCacheConfigExtension activated/modified.");
    }
}
