package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/**
 * WHAT IS IT ???
 * <p>
 * WHAT PURPOSE THAT IT HAS ???
 * </p>
 *
 * @author niek.raaijkmakers@external.cybercon.de
 * @since 2019-01-07
 */
@ObjectClassDefinition(name = "ACS AEM Commons - HTTP Cache - Cache config",
        description = "Config for request URI patterns that have to be cached.")
public @interface Config {

    String PROP_ORDER = "httpcache.config.order";

    String PROP_REQUEST_URI_PATTERNS = "httpcache.config.requesturi.patterns";

    String PROP_BLACKLISTED_REQUEST_URI_PATTERNS =
            "httpcache.config.requesturi.patterns.blacklisted";

    String PROP_AUTHENTICATION_REQUIREMENT = "httpcache.config.request.authentication";

    String PROP_CACHE_INVALIDATION_PATH_PATTERNS = "httpcache.config.invalidation.oak.paths";

    String PROP_CACHE_STORE = "httpcache.config.cachestore";

    String PROP_FILTER_SCOPE = "httpcache.config.filter.scope";

    String PROP_EXPIRY_ON_CREATE = "httpcache.config.expiry.on.create";

    String PROP_EXPIRY_ON_ACCESS = "httpcache.config.expiry.on.access";

    String PROP_EXPIRY_ON_UPDATE = "httpcache.config.expiry.on.update";

    String PROP_CACHE_HANDLING_RULES_PID = "httpcache.config.cache.handling.rules.pid";

    String FILTER_SCOPE_REQUEST = "REQUEST";

    String FILTER_SCOPE_INCLUDE = "INCLUDE";

    String DEFAULT_FILTER_SCOPE = FILTER_SCOPE_REQUEST; // Defaults to REQUEST scope

    String DEFAULT_KEY_FACTORY_TARGET = "(service.pid=com.adobe.acs.commons.httpcache.config.impl.GroupHttpCacheConfigExtension)";

    String DEFAULT_EXTENSION_TARGET = DEFAULT_KEY_FACTORY_TARGET;

    String DEFAULT_AUTHENTICATION_REQUIREMENT = AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST;

    String DEFAULT_CACHE_STORE = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE;

    long DEFAULT_EXPIRY_ON_CREATE = 0L;

    long DEFAULT_EXPIRY_ON_ACCESS = 0L;

    long DEFAULT_EXPIRY_ON_UPDATE = 0L;

    int DEFAULT_ORDER = 1000;

    @AttributeDefinition(name = "Priority order",
            description = "Order in which the HttpCacheEngine should evaluate the HttpCacheConfigs against the "
                    + "request. Evaluates smallest to largest (Integer.MIN_VALUE -> Integer.MAX_VALUE). Defaults to "
                    + "1000 ",
            defaultValue = "" + DEFAULT_ORDER)
    int httpcache_config_order() default DEFAULT_ORDER;

    @AttributeDefinition(name = "Request URI patterns",
            description = "Request URI patterns (REGEX) to be cached. Example - /content/mysite(.*).product-data"
                    + ".json. Mandatory parameter.",
            cardinality = Integer.MAX_VALUE)
    String[] httpcache_config_requesturi_patterns() default {};

    @AttributeDefinition(name = "Blacklisted request URI patterns",
            description = "Blacklisted request URI patterns (REGEX). Evaluated post applying the above request uri "
                    + "patterns (httpcache.config.requesturi.patterns). Optional parameter.",
            cardinality = Integer.MAX_VALUE)
    String[] httpcache_config_requesturi_patterns_blacklisted() default {};

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
    String httpcache_config_request_authentication() default AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST;

    @AttributeDefinition(name = "JCR path pattern (REGEX) for cache invalidation ",
            description = "Optional set of paths in JCR (Oak) repository for which this cache has to be invalidated"
                    + ". This accepts " + "REGEX. Example - /etc/my-products(.*)",
            cardinality = Integer.MAX_VALUE)
    String[] httpcache_config_invalidation_oak_paths() default {};

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
    String httpcache_config_cachestore() default HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE;

    @AttributeDefinition(name = "Filter scope",
            description = "Specify the scope of this HttpCacheConfig in the scope of the Sling Servlet Filter processing chain.",
            options = {
                    @Option(value = FILTER_SCOPE_REQUEST,
                            label = FILTER_SCOPE_REQUEST),
                    @Option(value = FILTER_SCOPE_INCLUDE,
                            label = FILTER_SCOPE_INCLUDE)
            },
            defaultValue = DEFAULT_FILTER_SCOPE)
    String httpcache_config_filter_scope() default DEFAULT_FILTER_SCOPE;

    @AttributeDefinition(
            name = "HttpCacheConfigExtension service pid",
            description = "Service pid of target implementation of HttpCacheConfigExtension to be used. Example - "
                    + "(service.pid=" + DEFAULT_EXTENSION_TARGET + ")."
                    + " Optional parameter.",
            defaultValue = DEFAULT_EXTENSION_TARGET)
    String cacheConfigExtension_target() default DEFAULT_EXTENSION_TARGET;

    @AttributeDefinition(name = "CacheKeyFactory service pid",
            description = "Service pid of target implementation of CacheKeyFactory to be used. Example - "
                    + "(service.pid=" + DEFAULT_KEY_FACTORY_TARGET + ")."
                    + " Mandatory parameter.",
            defaultValue = DEFAULT_KEY_FACTORY_TARGET)
    String cacheKeyFactory() default DEFAULT_KEY_FACTORY_TARGET;

    @AttributeDefinition(name = "Config-specific HttpCacheHandlingRules",
            description = "List of Service pid of HttpCacheHandlingRule applicable for this cache config. Optional "
                    + "parameter")
    String[] httpcache_config_cache_handling_rules_pid();

    @AttributeDefinition(name = "Expiry on create",
            description = "Specifies a custom expiry on create. Overrules the global expiry, unless the value is 0.")
    long httpcache_config_expiry_on_create();

    @AttributeDefinition(name = "Expiry on access",
            description = "Specifies a custom expiry on access. This refreshes the expiry of the entry if it's used. Lower then 0 means no expiry on access. ")
    long httpcache_config_expiry_on_access();

    @AttributeDefinition(name = "Expiry on update",
            description = "Specifies a custom expiry on update. This refreshes the expiry of the entry if it's updated. Lower then 0 means no expiry on update.")
    long httpcache_config_expiry_on_update();
}
