package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Config class for http cache. Modelled as OSGi config factory. Every factory instance is tied to a request URI.
 * Parameters such as selection of cache store, user groups, invalidation details shall also be configured.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache config",
           description = "Config for request URI pattern that has to be cached. Each config is tied to a single " +
                   "request URI pattern.",
           configurationFactory = true,
           metatype = true)
@Service
public class HttpCacheConfigImpl implements HttpCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    @Property(label = "Request URI pattern",
              description = "Request URI pattern (REGEX) to be cached. Example - /content/mysite(.*).product-data" +
                      ".json. Mandatory parameter.")
    private static final String PROP_REQUEST_URI_PATTERN = "httpcache.config.requesturi.pattern";
    private String requestUriPattern;

    @Property(label = "Is request authenticated",
              description = "Select if the request is authenticated. If not selected, this cache will be applicable "
                      + "only for anonymous (public user) requests.",
              boolValue = HttpCacheConfigImpl.DEFAULT_IS_REQUEST_AUTHENTICATION_REQUIRED)
    private static final String PROP_IS_REQUEST_AUTHENTICATION_REQUIRED = "httpcache.config.request.authentication" +
            ".isrequired";
    private static final boolean DEFAULT_IS_REQUEST_AUTHENTICATION_REQUIRED = false;
    private boolean isRequestAuthenticationRequired;

    @Property(label = "AEM user groups",
              description = "Set of AEM user groups for which this config is applicable. User of the " +
                      "request has to have at least one of these groups to be present to have this config applicable." +
                      " This parameter is effective only when the above " +
                      "'httpcache.config.request.authentication.isrequired' is true. This parameter is optional.",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_USER_GROUPS = "httpcache.config.users.group.mandatory";
    private String[] userGroups;


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

    @Property(label = "JCR path pattern (REGEX) for cache invalidation ",
              description = "Optional set of paths in JCR (Oak) repository for which this cache has to be invalidated" +
                      ". This accepts " + "REGEX. Example - /etc/my-products(.*)",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_CACHE_INVALIDATION_PATH_PATTERN = "httpcache.config.invalidation.oakpath";
    private String[] cacheInvalidationPathPattern;

    @Override
    public String getRequestUri() {
        return requestUriPattern;
    }

    @Override
    public boolean isRequestAuthenticationRequired() {
        return isRequestAuthenticationRequired;
    }

    @Override
    public List<String> getUserGroupNames() {
        return Arrays.asList(userGroups);
    }

    @Override
    public String getCacheStoreName() {
        return cacheStore;
    }

    @Override
    public List<String> getCacheInvalidationPaths() {
        return Arrays.asList(cacheInvalidationPathPattern);
    }

    @Activate
    @Modified
    protected void activate(Map<String, Object> configs) {
        //Read configs and populate variables after trimming whitespaces.
        requestUriPattern = PropertiesUtil.toString(configs.get(PROP_REQUEST_URI_PATTERN), StringUtils.EMPTY).trim();

        isRequestAuthenticationRequired = PropertiesUtil.toBoolean(configs.get
                (PROP_IS_REQUEST_AUTHENTICATION_REQUIRED), DEFAULT_IS_REQUEST_AUTHENTICATION_REQUIRED);

        userGroups = PropertiesUtil.toStringArray(PROP_USER_GROUPS, ArrayUtils.EMPTY_STRING_ARRAY);
        for (int i = 0; i < userGroups.length; i++) {
            userGroups[i] = userGroups[i].trim();
        }

        cacheStore = PropertiesUtil.toString(configs.get(PROP_CACHE_STORE), StringUtils.EMPTY).trim();

        cacheInvalidationPathPattern = PropertiesUtil.toStringArray(PROP_CACHE_INVALIDATION_PATH_PATTERN, ArrayUtils
                .EMPTY_STRING_ARRAY);
        for (int i = 0; i < cacheInvalidationPathPattern.length; i++) {
            cacheInvalidationPathPattern[i] = cacheInvalidationPathPattern[i].trim();
        }
        log.info("HttpCacheConfigImpl activated /modified.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("HttpCacheConfigImpl deactivated.");
    }
}
