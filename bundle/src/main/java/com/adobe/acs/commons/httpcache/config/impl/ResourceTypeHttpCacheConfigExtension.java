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
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.AbstractCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.commons.jcr.JcrConstants;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation for custom cache config extension and associated cache key creation based on resource type. This cache
 * config extension accepts the http request only if at least one of the configured patterns matches the resource type
 * of the request's resource.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - ResourceType based extension for HttpCacheConfig and CacheKeyFactory.",
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(name = "webconsole.configurationFactory.nameHint",
                value = "Allowed resource types: {httpcache.config.extension.resource-types.allowed}",
                propertyPrivate = true)
})
@Service
public class ResourceTypeHttpCacheConfigExtension implements HttpCacheConfigExtension, CacheKeyFactory {
    private static final Logger log = LoggerFactory.getLogger(ResourceTypeHttpCacheConfigExtension.class);

    // Custom cache config attributes
    @Property(label = "Allowed paths",
            description = "Regex of content paths that can be cached.",
            unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_PATHS = "httpcache.config.extension.paths.allowed";
    private List<Pattern> pathPatterns;

    @Property(label = "Allowed resource types",
            description = "Regex of resource types that can be cached.",
            unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_RESOURCE_TYPES = "httpcache.config.extension.resource-types.allowed";
    private List<Pattern> resourceTypePatterns;

    @Property(label = "Check RT of ./jcr:content?",
            description = "Should the resourceType check be applied to ./jcr:content ?",
            boolValue = false)
    public static final String PROP_CHECK_CONTENT_RESOURCE_TYPE = "httpcache.config.extension.resource-types.page-content";
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
        return new ResourceTypeCacheKey(slingHttpServletRequest, cacheConfig);
    }

    @Override
    public CacheKey build(final String resourcePath, final HttpCacheConfig cacheConfig)
            throws HttpCacheKeyCreationException {
        return new ResourceTypeCacheKey(resourcePath, cacheConfig);
    }

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {

        // Check if key is instance of ResourceTypeCacheKey.
        if (!(key instanceof ResourceTypeCacheKey)) {
            return false;
        }
        // Validate if key request uri can be constructed out of uri patterns in cache config.
        return new ResourceTypeCacheKey(key.getUri(), cacheConfig).equals(key);
    }

    /**
     * The ResourceTypeCacheKey is a custom CacheKey bound to this particular factory.
     */
    static class ResourceTypeCacheKey extends AbstractCacheKey implements CacheKey, Serializable
    {
        public ResourceTypeCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
                HttpCacheKeyCreationException {
            super(request, cacheConfig);
        }

        public ResourceTypeCacheKey(String uri, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {
            super(uri, cacheConfig);
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }

            if (o == null) {
                return false;
            }

            ResourceTypeCacheKey that = (ResourceTypeCacheKey) o;
            return new EqualsBuilder()
                    .append(getUri(), that.getUri())
                    .append(getAuthenticationRequirement(), that.getAuthenticationRequirement())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(getUri())
                    .append(getAuthenticationRequirement()).toHashCode();
        }

        @Override
        public String toString() {
            return this.resourcePath + " [AUTH_REQ:" + getAuthenticationRequirement() + "]";

        }

        @Override
        public String getUri() {
            return this.resourcePath;
        }

        /** For Serialization **/
        private void writeObject(ObjectOutputStream o) throws IOException
        {
            parentWriteObject(o);
        }


        /** For De serialization **/
        private void readObject(ObjectInputStream o)
                throws IOException, ClassNotFoundException {

            parentReadObject(o);
        }
    }

    //-------------------------<OSGi Component methods>

    @Activate
    protected void activate(Map<String, Object> configs) {
        resourceTypePatterns = ParameterUtil.toPatterns(PropertiesUtil.toStringArray(configs.get(PROP_RESOURCE_TYPES), new String[]{}));
        pathPatterns = ParameterUtil.toPatterns(PropertiesUtil.toStringArray(configs.get(PROP_PATHS), new String[]{}));
        checkContentResourceType = PropertiesUtil.toBoolean(configs.get(PROP_CHECK_CONTENT_RESOURCE_TYPE),false);

        log.info("ResourceHttpCacheConfigExtension activated/modified.");
    }
}
