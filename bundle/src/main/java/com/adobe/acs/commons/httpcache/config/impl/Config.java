package com.adobe.acs.commons.httpcache.config.impl;


import com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name= "ACS AEM Commons - HTTP Cache - Cache config",
        description = "Config for request URI patterns that have to be cached.")
public @interface Config {

    int DEFAULT_ORDER = 1000;

    String FILTER_SCOPE_REQUEST = "REQUEST";
    String FILTER_SCOPE_INCLUDE = "INCLUDE";

    // Authentication requirement
    String PROP_AUTHENTICATION_REQUIREMENT = "httpcache.config.request.authentication";
    String DEFAULT_AUTHENTICATION_REQUIREMENT = AuthenticationStatusConfigConstants
            .ANONYMOUS_REQUEST;
    // Invalidation paths
    String PROP_CACHE_INVALIDATION_PATH_PATTERNS = "httpcache.config.invalidation.oak.paths";

    // Cache store
    String PROP_CACHE_STORE = "httpcache.config.cachestore";
    String DEFAULT_CACHE_STORE = "MEM"; // Defaults to memory cache store

    // Cache store
    String DEFAULT_FILTER_SCOPE = FILTER_SCOPE_REQUEST; // Defaults to REQUEST scope
    String PROP_FILTER_SCOPE = "httpcache.config.filter.scope";

    // Request URIs - Whitelisted.
    String PROP_REQUEST_URI_PATTERNS = "httpcache.config.requesturi.patterns";

    // Request URIs - Blacklisted.
    String PROP_BLACKLISTED_REQUEST_URI_PATTERNS =
            "httpcache.config.requesturi.patterns.blacklisted";
    // Order
    String PROP_ORDER = "httpcache.config.order";

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
