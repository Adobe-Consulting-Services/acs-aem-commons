package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete implementation of cache config for http cache. Modelled as OSGi config factory.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache config",
           description = "Config for request URI patterns that have to be cached.",
           configurationFactory = true,
           metatype = true)
@Service
public class HttpCacheConfigImpl implements HttpCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    // Request URIs - Whitelisted.
    @Property(label = "Request URI patterns",
              description = "Request URI patterns (REGEX) to be cached. Example - /content/mysite(.*).product-data" +
                      ".json. Mandatory parameter.",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_REQUEST_URI_PATTERNS = "httpcache.config.requesturi.patterns";
    private List<String> requestUriPatterns;
    private List<Pattern> requestUriPatternsAsRegEx;

    // Request URIs - Blacklisted.
    @Property(label = "Blacklisted request URI patterns",
              description = "Blacklisted request URI patterns (REGEX). Evaluated post applying the above request uri " +
                      "" + "patterns (httpcache.config.requesturi.patterns). Optional parameter.",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_BLACKLISTED_REQUEST_URI_PATTERNS = "httpcache.config.requesturi.patterns" + "" +
            ".blacklisted";
    private List<String> blacklistedRequestUriPatterns;
    private List<Pattern> blacklistedRequestUriPatternsAsRegEx;

    // Authentication requirment
    // @formatter:off
    @Property(label = "Authentication",
              description = "Authentication requirement.",
              options = {
                      @PropertyOption(name = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST,
                                         value = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST),
                      @PropertyOption(name = AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST,
                                      value = AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST),
                      @PropertyOption(name = AuthenticationStatusConfigConstants.BOTH_ANONYMOUS_AUTHENTICATED_REQUESTS,
                                      value = AuthenticationStatusConfigConstants
                                              .BOTH_ANONYMOUS_AUTHENTICATED_REQUESTS)
              },
              value = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST)
    // @formatter:on
    private static final String PROP_AUTHENTICATION_REQUIREMENT = "httpcache.config.request.authentication";
    private static final String DEFAULT_AUTHENTICATION_REQUIREMENT = AuthenticationStatusConfigConstants
            .ANONYMOUS_REQUEST;
    private String authenticationRequirement;

    // User groups
    @Property(label = "AEM user groups",
              description = "Set of AEM user groups for which this config is applicable. User of the " +
                      "request has to have at least one of these groups to be present to have this config applicable." +
                      " This parameter is effective only when the above " +
                      "'httpcache.config.request.authentication' is anonymous. This parameter is optional.",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_USER_GROUPS = "httpcache.config.user.groups";
    private List<String> userGroups;


    // Cache store
    // @formatter:off
    @Property(label = "Cache store",
              description = "Cache store for caching the response for this request URI. Example - MEM. This should "
                      + "be one of the cache stores active in this installation. Mandatory parameter.",
              options = {
                      @PropertyOption(name = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE,
                                         value = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE),
                      @PropertyOption(name = HttpCacheStore.VALUE_DISK_CACHE_STORE_TYPE,
                                      value = HttpCacheStore.VALUE_DISK_CACHE_STORE_TYPE),
                      @PropertyOption(name = HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE,
                                      value = HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE)},
              value = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE)
    // @formatter:on
    private static final String PROP_CACHE_STORE = "httpcache.config.cachestore";
    private static final String DEFAULT_CACHE_STORE = "MEM"; // Defaults to memory cache store
    private String cacheStore;

    // Invalidation paths
    @Property(label = "JCR path pattern (REGEX) for cache invalidation ",
              description = "Optional set of paths in JCR (Oak) repository for which this cache has to be invalidated" +
                      ". This accepts " + "REGEX. Example - /etc/my-products(.*)",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_CACHE_INVALIDATION_PATH_PATTERNS = "httpcache.config.invalidation.oak.paths";
    private List<String> cacheInvalidationPathPatterns;
    private List<Pattern> cacheInvalidationPathPatternsAsRegEx;

    // TODO make this target configurable via OSGi so other can specific custom Key factories
    @Reference(target = "(component.pid=com.adobe.acs.commons.httpcache.keys.impl.GroupCacheKeyFactory)")
    private CacheKeyFactory cacheKeyFactory;

    @Activate
    @Modified
    protected void activate(Map<String, Object> configs) {
        //Read configs and populate variables after trimming whitespaces.

        // Request URIs - Whitelisted.
        requestUriPatterns = Arrays.asList(
                PropertiesUtil.toStringArray(configs.get(PROP_REQUEST_URI_PATTERNS), new String[]{}));
        requestUriPatternsAsRegEx = compileToPatterns(requestUriPatterns);

        // Request URIs - Blacklisted.
        blacklistedRequestUriPatterns = Arrays.asList(
                PropertiesUtil.toStringArray(configs.get(PROP_BLACKLISTED_REQUEST_URI_PATTERNS), new String[]{}));
        blacklistedRequestUriPatternsAsRegEx = compileToPatterns(blacklistedRequestUriPatterns);

        // Authentication requirement.
        authenticationRequirement = PropertiesUtil.toString(configs.get(PROP_AUTHENTICATION_REQUIREMENT),
                DEFAULT_AUTHENTICATION_REQUIREMENT);

        // Cache store
        cacheStore = PropertiesUtil.toString(configs.get(PROP_CACHE_STORE), DEFAULT_CACHE_STORE);

        // Cache invalidation paths.
        cacheInvalidationPathPatterns = Arrays.asList(
                PropertiesUtil.toStringArray(configs.get(PROP_CACHE_INVALIDATION_PATH_PATTERNS), new String[]{}));
        cacheInvalidationPathPatternsAsRegEx = compileToPatterns(cacheInvalidationPathPatterns);

        log.info("HttpCacheConfigImpl activated /modified.");
    }

    /**
     * Converts an array of Regex strings into compiled Patterns.
     *
     * @param regexes the regex strings to compile into Patterns
     * @return the list of compiled Patterns
     */
    private List<Pattern> compileToPatterns(final List<String> regexes) {
        final List<Pattern> patterns = new ArrayList<>();

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

    private boolean matches(List<Pattern> patterns, String data) {
        for (Pattern pattern : patterns) {
            final Matcher matcher = pattern.matcher(data);
            if (matcher.matches()) {
                return true;
            }
        }

        return false;
    }

    //------------------------< Interface specific methods >
    @Override
    public boolean accepts(SlingHttpServletRequest request) {
        if ("anonymous".equals(request.getResourceResolver().getUserID())
                && AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST.equals(this.authenticationRequirement)) {
            // Only supports authenticated requests, but request is anonymous so reject
            return false;
        }

        final String uri = request.getRequestURI();

        if (!this.matches(this.requestUriPatternsAsRegEx, uri)) {
            // Does not match URI Whitelist
            return false;
        }

        if (this.matches(this.blacklistedRequestUriPatternsAsRegEx, uri)) {
            // Matches URI Blacklist; reject
            return false;
        }

        return true;
    }

    @Override
    public CacheKey buildCacheKey(SlingHttpServletRequest request) {
        try {
            return this.cacheKeyFactory.build(request);
        } catch (HttpCacheKeyCreationException e) {
            // TODO handle error
            return null;
        }
    }

    @Override
    public boolean isValid() {
        return CollectionUtils.isNotEmpty(this.requestUriPatterns);
    }

    @Override
    public boolean isInvalidateAll() {
        // TODO get this from OSGi Config
        return false;
    }

    @Override
    public boolean canInvalidate(final String path) {
        return matches(cacheInvalidationPathPatternsAsRegEx, path);
    }

    @Override
    public String getCacheStoreName() {
        return cacheStore;
    }
}
