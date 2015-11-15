package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Concrete implementation of cache config for http cache. Modelled as OSGi config factory.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache config",
           description = "Config for request URI patterns that have to be cached.",
           configurationFactory = true,
           metatype = true,
           immediate = true)
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
              options = {@PropertyOption(name = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST,
                                         value = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST),
                      @PropertyOption(name = AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST,
                                      value = AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST),
                      @PropertyOption(name = AuthenticationStatusConfigConstants.BOTH_ANONYMOUS_AUTHENTICATED_REQUESTS,
                                      value = AuthenticationStatusConfigConstants
                                              .BOTH_ANONYMOUS_AUTHENTICATED_REQUESTS)},
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


    @Activate
    @Modified
    protected void activate(Map<String, Object> configs) {
        //Read configs and populate variables after trimming whitespaces.

        // Request URIs - Whitelisted.
        requestUriPatterns = new ArrayList(Arrays.asList(PropertiesUtil.toStringArray(configs.get
                (PROP_REQUEST_URI_PATTERNS))));
        requestUriPatternsAsRegEx = new ArrayList<>();
        Iterator<String> listIterator = requestUriPatterns.listIterator();
        while (listIterator.hasNext()) {
            String value = listIterator.next();
            if (StringUtils.isNotBlank(value)) {
                requestUriPatternsAsRegEx.add(Pattern.compile(value));
            } else {
                listIterator.remove();
            }
        }

        // Request URIs - Blacklisted.
        blacklistedRequestUriPatterns = new ArrayList(Arrays.asList(PropertiesUtil.toStringArray(configs.get
                (PROP_BLACKLISTED_REQUEST_URI_PATTERNS))));
        blacklistedRequestUriPatternsAsRegEx = new ArrayList<>();
        listIterator = blacklistedRequestUriPatterns.listIterator();
        while (listIterator.hasNext()) {
            String value = listIterator.next();
            if (StringUtils.isNotBlank(value)) {
                blacklistedRequestUriPatternsAsRegEx.add(Pattern.compile(value));
            } else {
                listIterator.remove();
            }
        }

        // Authentication requirement.
        authenticationRequirement = PropertiesUtil.toString(configs.get(PROP_AUTHENTICATION_REQUIREMENT),
                DEFAULT_AUTHENTICATION_REQUIREMENT);

        // User groups.
        userGroups = new ArrayList(Arrays.asList(PropertiesUtil.toStringArray(configs.get(PROP_USER_GROUPS))));
        listIterator = userGroups.listIterator();
        while (listIterator.hasNext()) {
            String value = listIterator.next();
            if (StringUtils.isBlank(value)) {
                listIterator.remove();
            }
        }

        // Cache store
        cacheStore = PropertiesUtil.toString(configs.get(PROP_CACHE_STORE), DEFAULT_CACHE_STORE);

        // Cache invalidation paths.
        cacheInvalidationPathPatterns = new ArrayList(Arrays.asList(PropertiesUtil.toStringArray(configs.get
                (PROP_CACHE_INVALIDATION_PATH_PATTERNS))));
        cacheInvalidationPathPatternsAsRegEx = new ArrayList<>();
        listIterator = cacheInvalidationPathPatterns.listIterator();
        while (listIterator.hasNext()) {
            String value = listIterator.next();
            if (StringUtils.isNotBlank(value)) {
                cacheInvalidationPathPatternsAsRegEx.add(Pattern.compile(value));
            } else {
                listIterator.remove();
            }
        }

        log.info("HttpCacheConfigImpl activated /modified.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("HttpCacheConfigImpl deactivated.");
    }

    //------------------------< Interface specific methods >
    @Override
    public List<String> getRequestURIs() {
        return requestUriPatterns;
    }

    @Override
    public List<Pattern> getRequestURIsAsRegEx() {
        return requestUriPatternsAsRegEx;
    }

    @Override
    public List<String> getBlacklistedURIs() {
        return blacklistedRequestUriPatterns;
    }

    @Override
    public List<Pattern> getBlacklistedURIsAsRegEx() {
        return blacklistedRequestUriPatternsAsRegEx;
    }

    @Override
    public String getAuthenticationRequirement() {
        return authenticationRequirement;
    }

    @Override
    public List<String> getUserGroupNames() {
        return userGroups;
    }

    @Override
    public String getCacheStoreName() {
        return cacheStore;
    }

    @Override
    public List<String> getCacheInvalidationPaths() {
        return cacheInvalidationPathPatterns;
    }

    @Override
    public List<Pattern> getCacheInvalidationPathsAsRegEx() {
        return cacheInvalidationPathPatternsAsRegEx;
    }
}
