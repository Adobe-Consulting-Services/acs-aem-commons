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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.util.UserUtils;

/**
 * Concrete implementation of cache config for http cache. Modelled as OSGi config factory.
 */
@Component(service=HttpCacheConfig.class,
configurationPolicy=ConfigurationPolicy.REQUIRE,
property= {
      "webconsole.configurationFactory.nameHint" + "=" +
             "Order: {httpcache.config.order}, "
                 + "Request URIs: {httpcache.config.requesturi.patterns}, "
                 + "Request URIs blacklist: {httpcache.config.requesturi.patterns.blacklisted}, "
                 + "Authentication: {httpcache.config.request.authentication}, "
                 + "Invalidation paths: {httpcache.config.invalidation.oak.paths}, "
                 + "Cache type: {httpcache.config.cachestore}"})
@Designate(ocd=HttpCacheConfigImpl.Config.class, factory=true)
public class HttpCacheConfigImpl implements HttpCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);
    private static final String FILTER_SCOPE_REQUEST = "REQUEST";
    private static final String FILTER_SCOPE_INCLUDE = "INCLUDE";
    
    @ObjectClassDefinition(name= "ACS AEM Commons - HTTP Cache - Cache config",
           description = "Config for request URI patterns that have to be cached.")
    public @interface Config {
        @AttributeDefinition(name = "Priority order",
                description = "Order in which the HttpCacheEngine should evaluate the HttpCacheConfigs against the "
                        + "request. Evaluates smallest to largest (Integer.MIN_VALUE -> Integer.MAX_VALUE). Defaults to "
                        + "1000 ",
                defaultValue = ""+DEFAULT_ORDER)
        int httpcache_config_order();
        
        @AttributeDefinition(name = "Request URI patterns",
                description = "Request URI patterns (REGEX) to be cached. Example - /content/mysite(.*).product-data"
                        + ".json. Mandatory parameter.",
                cardinality = Integer.MAX_VALUE)
      String[] httpcache_config_requesturi_patterns();
        
        @AttributeDefinition(name = "Blacklisted request URI patterns",
                description = "Blacklisted request URI patterns (REGEX). Evaluated post applying the above request uri "
                        + "patterns (httpcache.config.requesturi.patterns). Optional parameter.",
                cardinality = Integer.MAX_VALUE)
      String[] httpcache_config_requesturi_patterns_blacklisted();

        @AttributeDefinition(name = "Authentication",
                description = "Authentication requirement.",
                options = {
                        @Option(value = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST,
                                           label = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST),
                        @Option(value = AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST,
                              label = AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST),
                        @Option(value = AuthenticationStatusConfigConstants.BOTH_ANONYMOUS_AUTHENTICATED_REQUESTS,
                              label = AuthenticationStatusConfigConstants.BOTH_ANONYMOUS_AUTHENTICATED_REQUESTS)
                },
                defaultValue = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST)
      String httpcache_config_request_authentication();
        
        @AttributeDefinition(name = "JCR path pattern (REGEX) for cache invalidation ",
                description = "Optional set of paths in JCR (Oak) repository for which this cache has to be invalidated"
                        + ". This accepts " + "REGEX. Example - /etc/my-products(.*)",
                cardinality = Integer.MAX_VALUE)
       String[] httpcache_config_invalidation_oak_paths()
       ;
        @AttributeDefinition(name = "Cache store",
                description = "Cache store for caching the response for this request URI. Example - MEM. This should "
                        + "be one of the cache stores active in this installation. Mandatory parameter.",
                options = {
                        @Option(value = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE,
                                           label = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE),
                        @Option(value = HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE,
                              label = HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE)
                },
              defaultValue = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE)
      String httpcache_config_cachestore();
        
        @AttributeDefinition(name = "Filter scope",
                description = "Specify the scope of this HttpCacheConfig in the scope of the Sling Servlet Filter processing chain.",
                options = {
                      @Option(value = FILTER_SCOPE_REQUEST,
                                label = FILTER_SCOPE_REQUEST),
                      @Option(value = FILTER_SCOPE_INCLUDE,
                            label = FILTER_SCOPE_INCLUDE)
                },
                defaultValue = DEFAULT_FILTER_SCOPE)
        String httpcache_config_filter_scope();

        @AttributeDefinition(
                name = "HttpCacheConfigExtension service pid",
                description = "Service pid of target implementation of HttpCacheConfigExtension to be used. Example - "
                        + "(service.pid=com.adobe.acs.commons.httpcache.config.impl.GroupHttpCacheConfigExtension)."
                        + " Optional parameter.",
                defaultValue = "(service.pid=com.adobe.acs.commons.httpcache.config.impl.GroupHttpCacheConfigExtension)")
   String cacheConfigExtension_target();
        
        @AttributeDefinition( name = "CacheKeyFactory service pid",
              description = "Service pid of target implementation of CacheKeyFactory to be used. Example - "
                        + "(service.pid=com.adobe.acs.commons.httpcac`he.config.impl.GroupHttpCacheConfigExtension)."
                        + " Mandatory parameter.",
                defaultValue = "(service.pid=com.adobe.acs.commons.httpcache.config.impl.GroupHttpCacheConfigExtension)")
        String cacheKeyFactory();
        
        @AttributeDefinition(name = "Config-specific HttpCacheHandlingRules",
                description = "List of Service pid of HttpCacheHandlingRule applicable for this cache config. Optional "
                        + "parameter")
        String[] httpcache_config_cache_handling_rules_pid();

    }

    // Order
    public static final int DEFAULT_ORDER = 1000;
    private int order = DEFAULT_ORDER;
    public static final String PROP_ORDER = "httpcache.config.order";

    // Request URIs - Whitelisted.
    private static final String PROP_REQUEST_URI_PATTERNS = "httpcache.config.requesturi.patterns";
    private List<String> requestUriPatterns;
    private List<Pattern> requestUriPatternsAsRegEx;

    // Request URIs - Blacklisted.

    private static final String PROP_BLACKLISTED_REQUEST_URI_PATTERNS =
            "httpcache.config.requesturi.patterns.blacklisted";
    private List<String> blacklistedRequestUriPatterns;
    private List<Pattern> blacklistedRequestUriPatternsAsRegEx;

    // Authentication requirement
    private static final String PROP_AUTHENTICATION_REQUIREMENT = "httpcache.config.request.authentication";
    private static final String DEFAULT_AUTHENTICATION_REQUIREMENT = AuthenticationStatusConfigConstants
            .ANONYMOUS_REQUEST;
    private String authenticationRequirement;

    // Invalidation paths
    private static final String PROP_CACHE_INVALIDATION_PATH_PATTERNS = "httpcache.config.invalidation.oak.paths";
    private List<String> cacheInvalidationPathPatterns;
    private List<Pattern> cacheInvalidationPathPatternsAsRegEx;

    // Cache store
    private static final String PROP_CACHE_STORE = "httpcache.config.cachestore";
    private static final String DEFAULT_CACHE_STORE = "MEM"; // Defaults to memory cache store
    private String cacheStore;


    // Cache store
    private static final String DEFAULT_FILTER_SCOPE = FILTER_SCOPE_REQUEST; // Defaults to REQUEST scope
    private static final String PROP_FILTER_SCOPE = "httpcache.config.filter.scope";
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

    private static final String PROP_CACHE_HANDLING_RULES_PID = "httpcache.config.cache-handling-rules.pid";
    private List<String> cacheHandlingRulesPid;

    @Activate
    protected void activate(Map<String, Object> configs) {

        // Request URIs - Whitelisted.
        requestUriPatterns = Arrays.asList(PropertiesUtil.toStringArray(configs.get(PROP_REQUEST_URI_PATTERNS), new
                String[]{}));
        requestUriPatternsAsRegEx = compileToPatterns(requestUriPatterns);

        // Request URIs - Blacklisted.
        blacklistedRequestUriPatterns = Arrays.asList(PropertiesUtil.toStringArray(configs
                .get(PROP_BLACKLISTED_REQUEST_URI_PATTERNS), new String[]{}));
        blacklistedRequestUriPatternsAsRegEx = compileToPatterns(blacklistedRequestUriPatterns);

        // Authentication requirement.
        authenticationRequirement = PropertiesUtil.toString(configs.get(PROP_AUTHENTICATION_REQUIREMENT),
                DEFAULT_AUTHENTICATION_REQUIREMENT);

        // Cache store
        cacheStore = PropertiesUtil.toString(configs.get(PROP_CACHE_STORE), DEFAULT_CACHE_STORE);

        // Cache invalidation paths.
        cacheInvalidationPathPatterns = Arrays.asList(PropertiesUtil.toStringArray(configs
                .get(PROP_CACHE_INVALIDATION_PATH_PATTERNS), new String[]{}));
        cacheInvalidationPathPatternsAsRegEx = compileToPatterns(cacheInvalidationPathPatterns);

        order = PropertiesUtil.toInteger(configs.get(PROP_ORDER), DEFAULT_ORDER);

        filterScope = FilterScope.valueOf(PropertiesUtil.toString(configs.get(PROP_FILTER_SCOPE), DEFAULT_FILTER_SCOPE).toUpperCase());

        // PIDs of cache handling rules.
        cacheHandlingRulesPid = new ArrayList<String>(Arrays.asList(PropertiesUtil.toStringArray(configs
                .get(PROP_CACHE_HANDLING_RULES_PID), new String[]{})));
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
