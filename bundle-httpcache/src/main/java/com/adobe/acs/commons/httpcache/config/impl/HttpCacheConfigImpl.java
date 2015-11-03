package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
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
                   "request URI pattern. Allows multiple configurations.",
           configurationFactory = true,
           metatype = true)
@Service
public class HttpCacheConfigImpl implements HttpCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    @Property(label = "Request URI pattern",
              description = "Request URI pattern (REGEX) to be cached. Example - /content/mysite(.*).product-data.json. Mandatory parameter.")
    private static final String PROP_REQUEST_URI_PATTERN = "httpcache.config.requesturi.pattern";
    private String requestUriPattern;

    @Property(label = "Response MIME type",
              description = "Example - application/json. Mandatory parameter.")
    private static final String PROP_RESPONSE_MIME_TYPE = "httpcache.config.response.mime";
    private String responseMimeType;

    @Property(label = "AEM user groups - Mandatory",
              description = "Mandatory set of AEM user groups for which this config is applicable. User of the " +
                      "request has to have all these groups for the request to be cache. Represents AND condition.",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_USER_GROUPS_MANDATORY = "httpcache.config.users.group.mandatory";
    private String[] userGroupsMandatory;

    @Property(label = "AEM user groups - Optional",
              description = "Optional set of AEM user groups for which this config is applicable. Represents OR " +
                      "condition.",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_USER_GROUPS_OPTIONAL = "httpcache.config.users.group.optional";
    private String[] userGroupsOptional;

    @Property(label = "Cache store",
              description = "Cache store for caching the response for this request URI. Example - MEM. This should " +
                      "be" + " one of the cache stores active in this installation. Mandatory parameter.",
              value = HttpCacheConfigImpl.DEFAULT_CACHE_STORE)
    private static final String PROP_CACHE_STORE = "httpcache.config.cachestore";
    private static final String DEFAULT_CACHE_STORE = "MEM"; // Defaults to memory cache store
    private String cacheStore;

    @Property(label = "JCR path pattern (REGEX) for cache invalidation ",
              description = "Optional set of paths in JCR (Oak) repository for which this cache has to be invalidated. This accepts "
                      + "REGEX. Example - /etc/my-products(.*)",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_CACHE_INVALIDATION_PATH_PATTERN = "httpcache.config.invalidation.oakpath";
    private String[] cacheInvalidationPathPattern;

    @Override
    public String getRequestUri() {
        return requestUriPattern;
    }

    @Override
    public String getResponseMimeType() {
        return responseMimeType;
    }

    @Override
    public List<String> getMandatoryUserGroupNames() {
        return Arrays.asList(userGroupsMandatory);
    }

    @Override
    public List<String> getOptionalUserGroupNames() {
        return Arrays.asList(userGroupsOptional);
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
        responseMimeType = PropertiesUtil.toString(configs.get(PROP_RESPONSE_MIME_TYPE), StringUtils.EMPTY).trim();
        userGroupsMandatory = PropertiesUtil.toStringArray(PROP_USER_GROUPS_MANDATORY, ArrayUtils.EMPTY_STRING_ARRAY);
        for (int i = 0; i < userGroupsMandatory.length; i++) {
            userGroupsMandatory[i] = userGroupsMandatory[i].trim();
        }
        userGroupsOptional = PropertiesUtil.toStringArray(PROP_USER_GROUPS_OPTIONAL, ArrayUtils.EMPTY_STRING_ARRAY);
        for (int i = 0; i < userGroupsOptional.length; i++) {
            userGroupsOptional[i] = userGroupsOptional[i].trim();
        }
        cacheStore = PropertiesUtil.toString(configs.get(PROP_CACHE_STORE), StringUtils.EMPTY).trim();
        cacheInvalidationPathPattern = PropertiesUtil.toStringArray(PROP_CACHE_INVALIDATION_PATH_PATTERN, ArrayUtils
                .EMPTY_STRING_ARRAY);
        for (int i = 0; i < cacheInvalidationPathPattern.length; i++) {
            userGroupsOptional[i] = cacheInvalidationPathPattern[i].trim();
        }
        log.info("HttpCacheConfigImpl activated /modified.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("HttpCacheConfigImpl deactivated.");
    }
}
