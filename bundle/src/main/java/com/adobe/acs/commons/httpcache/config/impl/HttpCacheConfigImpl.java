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

import com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.httpcache.util.UserUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete implementation of cache config for http cache. Modelled as OSGi config factory.
 */
@Component(service=HttpCacheConfig.class,
configurationPolicy=ConfigurationPolicy.REQUIRE,
property= {
      "webconsole.configurationFactory.nameHint" + "="
                 + "Order: {httpcache.config.order}, "
                 + "Request URIs: {httpcache.config.requesturi.patterns}, "
                 + "Request URIs blacklist: {httpcache.config.requesturi.patterns.blacklisted}, "
                 + "Authentication: {httpcache.config.request.authentication}, "
                 + "Invalidation paths: {httpcache.config.invalidation.oak.paths}, "
                 + "Cache type: {httpcache.config.cachestore}"})
@Designate(ocd=Config.class, factory=true)
public class HttpCacheConfigImpl implements HttpCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    private int order = Config.DEFAULT_ORDER;

    // Request URIs - Whitelisted.
    private List<String> requestUriPatterns;
    private List<Pattern> requestUriPatternsAsRegEx;

    // Request URIs - Blacklisted.
    private List<String> blacklistedRequestUriPatterns;
    private List<Pattern> blacklistedRequestUriPatternsAsRegEx;

    // Authentication requirement
    private String authenticationRequirement;

    // Invalidation paths
    private List<String> cacheInvalidationPathPatterns;
    private List<Pattern> cacheInvalidationPathPatternsAsRegEx;

    // Cache store
    private String cacheStore;

    // Cache store
    private FilterScope filterScope;

    // Making the cache config extension configurable.
    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               name = "cacheConfigExtension")
    private volatile HttpCacheConfigExtension cacheConfigExtension;

    // Making the cache key factory configurable.

    @Reference(cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               name = "cacheKeyFactory")
    private volatile CacheKeyFactory cacheKeyFactory;

    private List<String> cacheHandlingRulesPid;


    private long expiryOnCreate;
    private long expiryOnAccess;
    private long expiryOnUpdate;

    @Activate
    protected void activate(Map<String,Object> configs) {

        // Request URIs - Whitelisted.
        requestUriPatterns = Arrays.asList(PropertiesUtil.toStringArray(configs.get(Config.PROP_REQUEST_URI_PATTERNS), new
                String[]{}));
        requestUriPatternsAsRegEx = compileToPatterns(requestUriPatterns);

        // Request URIs - Blacklisted.
        blacklistedRequestUriPatterns = Arrays.asList(PropertiesUtil.toStringArray(configs
                .get(Config.PROP_BLACKLISTED_REQUEST_URI_PATTERNS), new String[]{}));
        blacklistedRequestUriPatternsAsRegEx = compileToPatterns(blacklistedRequestUriPatterns);

        // Authentication requirement.
        authenticationRequirement = PropertiesUtil.toString(configs.get(Config.PROP_AUTHENTICATION_REQUIREMENT),
                Config.DEFAULT_AUTHENTICATION_REQUIREMENT);

        // Cache store
        cacheStore = PropertiesUtil.toString(configs.get(Config.PROP_CACHE_STORE), Config.DEFAULT_CACHE_STORE);

        // Custom expiry
        expiryOnCreate = PropertiesUtil.toLong(configs.get(Config.PROP_EXPIRY_ON_CREATE), Config.DEFAULT_EXPIRY_ON_CREATE);
        expiryOnAccess = PropertiesUtil.toLong(configs.get(Config.PROP_EXPIRY_ON_ACCESS), Config.DEFAULT_EXPIRY_ON_ACCESS);
        expiryOnUpdate = PropertiesUtil.toLong(configs.get(Config.PROP_EXPIRY_ON_UPDATE), Config.DEFAULT_EXPIRY_ON_UPDATE);

        // Cache invalidation paths.
        cacheInvalidationPathPatterns = Arrays.asList(PropertiesUtil.toStringArray(configs
                .get(Config.PROP_CACHE_INVALIDATION_PATH_PATTERNS), new String[]{}));
        cacheInvalidationPathPatternsAsRegEx = compileToPatterns(cacheInvalidationPathPatterns);

        order = PropertiesUtil.toInteger(configs.get(Config.PROP_ORDER), Config.DEFAULT_ORDER);

        filterScope = FilterScope.valueOf(PropertiesUtil.toString(configs.get(Config.PROP_FILTER_SCOPE), Config.DEFAULT_FILTER_SCOPE).toUpperCase());
        // PIDs of cache handling rules.
        cacheHandlingRulesPid = new ArrayList<String>(Arrays.asList(PropertiesUtil.toStringArray(configs
                .get(Config.PROP_CACHE_HANDLING_RULES_PID), new String[]{})));
        ListIterator<String> listIterator = cacheHandlingRulesPid.listIterator();
        while (listIterator.hasNext()) {
            String value = listIterator.next();
            if (StringUtils.isBlank(value)) {
                listIterator.remove();
            }
        }

        log.info("HttpCacheConfigImpl activated.");
    }

    /**
     * Converts an array of Regex strings into compiled Patterns.
     *
     * @param regexes the regex strings to compile into Patterns
     * @return the list of compiled Patterns
     */
    private List<Pattern> compileToPatterns(final List<String> regexes) {
        final List<Pattern> patterns = new ArrayList<Pattern>();

        for (String regex : regexes) {
            if (StringUtils.isNotBlank(regex)) {
                patterns.add(Pattern.compile(regex));
            }
        }

        return patterns;
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("HttpCacheConfigImpl deactivated.");
    }

    //------------------------< Interface specific implementation >

    @Override
    public String getCacheStoreName() {
        return cacheStore;
    }

    @Override
    public boolean accepts(SlingHttpServletRequest request) throws HttpCacheRepositoryAccessException {

        // Match authentication requirement.
        if (UserUtils.isAnonymous(request.getResourceResolver().getUserID())) {
            if (AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST.equals(this.authenticationRequirement)) {
                log.trace("Rejected: Request is anonymous but the config accepts only authenticated request and hence"
                        + " reject");
                return false;
            }
        } else {
            if (AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST.equals(this.authenticationRequirement)) {
                log.trace("Rejected: Request is authenticated but config is for anonymous and hence reject.");
                return false;
            }
        }

        // Match request URI.
        final String uri = request.getRequestURI();
        if (!this.matches(this.requestUriPatternsAsRegEx, uri)) {
            // Does not match URI Whitelist
            log.trace("Rejected: Request URI does not match the white-listed URI patterns");
            return false;
        }

        // Match blacklisted URI.
        if (this.matches(this.blacklistedRequestUriPatternsAsRegEx, uri)) {
            // Matches URI Blacklist; reject
            log.trace("Rejected: Request URI does match a black-listed URI pattern");
            return false;
        }

        // Passing on the control to the extension point.
        if (null != cacheConfigExtension) {
            return cacheConfigExtension.accepts(request, this);
        }

        return true;
    }

    /**
     * Matching the given data with the set of compiled patterns.
     *
     * @param patterns
     * @param data
     * @return
     */
    private boolean matches(List<Pattern> patterns, String data) {
        for (Pattern pattern : patterns) {
            final Matcher matcher = pattern.matcher(data);
            if (matcher.matches()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public CacheKey buildCacheKey(SlingHttpServletRequest request) throws HttpCacheKeyCreationException {
        return this.cacheKeyFactory.build(request, this);
    }

    @Override
    public CacheKey buildCacheKey(String resourcePath) throws HttpCacheKeyCreationException {
        return this.cacheKeyFactory.build(resourcePath, this);
    }

    @Override
    public boolean isValid() {
        return CollectionUtils.isNotEmpty(this.requestUriPatterns);
    }

    @Override
    public boolean canInvalidate(final String path) {
        return matches(cacheInvalidationPathPatternsAsRegEx, path);
    }

    @Override
    public String getAuthenticationRequirement() {
        return this.authenticationRequirement;
    }

    @Override
    public List<Pattern> getRequestUriPatterns() {
        return this.requestUriPatternsAsRegEx;
    }

    @Override
    public List<Pattern> getBlacklistedRequestUriPatterns() {
        return this.blacklistedRequestUriPatternsAsRegEx;
    }

    @Override
    public List<Pattern> getJCRInvalidationPathPatterns() {
        return this.cacheInvalidationPathPatternsAsRegEx;
    }

    @Override
    public boolean knows(CacheKey key) throws HttpCacheKeyCreationException {
        return this.cacheKeyFactory.doesKeyMatchConfig(key, this);
    }

    @Override
    public long getExpiryOnCreate() {
        return expiryOnCreate;
    }

    @Override
    public long getExpiryForAccess() {
        return expiryOnAccess;
    }

    @Override
    public long getExpiryForUpdate() {
        return expiryOnUpdate;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public boolean acceptsRule(String servicePid) {
        return cacheHandlingRulesPid.contains(servicePid);
    }

    @Override
    public FilterScope getFilterScope() {
        return this.filterScope;
    }
}
