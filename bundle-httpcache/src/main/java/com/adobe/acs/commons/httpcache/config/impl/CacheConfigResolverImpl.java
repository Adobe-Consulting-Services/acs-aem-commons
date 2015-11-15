package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants;
import com.adobe.acs.commons.httpcache.config.CacheConfigResolver;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheConfigConflictException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheReposityAccessException;
import com.adobe.acs.commons.httpcache.util.UserUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Concrete implementation for the cache config resolver. Has aggregation relationship with Cache configs.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache config resolver",
           description = "Resolves cache config for the http request.",
           immediate = true)
@Service
@Reference(name = CacheConfigResolverImpl.METHOD_NAME_TO_BIND_CONFIG,
           referenceInterface = HttpCacheConfig.class,
           policy = ReferencePolicy.DYNAMIC,
           cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class CacheConfigResolverImpl implements CacheConfigResolver {
    private static final Logger log = LoggerFactory.getLogger(CacheConfigResolverImpl.class);

    /** Method name that binds cache configs */
    protected static final String METHOD_NAME_TO_BIND_CONFIG = "httpCacheConfig";

    /** Thread safe list to contain the registered HttpCacheConfig references. */
    private static CopyOnWriteArrayList<HttpCacheConfig> cacheConfigs = new CopyOnWriteArrayList<>();

    @Activate
    protected void activate(Map<String, Object> configs) {
        log.info("CacheConfigResolverImpl activated.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("CacheConfigResolverImpl deactivated.");
    }

    /**
     * Binds http cache config.
     *
     * @param cacheConfig
     * @param config
     */
    protected void bindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> config) {
        // Validate cache config object
        // Check if the request uri is present.
        if (cacheConfig.getRequestURIs().isEmpty()) {
            log.info("Http cache config rejected as the request uri is absent.");
            return;
        }
        // Remove the user groups array if the config is tied to anonymous requests.
        if (!AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST.equals(cacheConfig.getAuthenticationRequirement())
                && !cacheConfig.getUserGroupNames().isEmpty()) {
            cacheConfig.getUserGroupNames().clear();
            log.debug("Config is for unauthenticated requests and hence list of groups configured are rejected.");
        }

        // Check if the same object is already there in the map.
        if (cacheConfigs.contains(cacheConfig)) {
            log.trace("Http cache config object already exists in the cacheConfigs map and hence ignored.");
            return;
        }

        // Add it to the map.
        cacheConfigs.add(cacheConfig);
        log.info("Cache config for request URIs {} added.", cacheConfig.getRequestURIs().toString());
        log.debug("Total number of cache configs added - {}", cacheConfigs.size());
    }

    /**
     * Unbinds cache config.
     *
     * @param cacheConfig
     * @param config
     */
    protected void unbindHttpCacheConfig(final HttpCacheConfig cacheConfig, final Map<String, Object> config) {
        if (cacheConfigs.contains(cacheConfig)) {
            cacheConfigs.remove(cacheConfig);
            // TODO - When a cache config is unbound, associated cached items should be removed from the cache store.
            log.info("Cache config for request URI {} removed.", cacheConfig.getRequestURIs().toString());
            log.debug("Total number of cache configs after removal - {}", cacheConfigs.size());
            return;
        }
        log.debug("This cache config entry was not bound and hence nothing to unbind.");
    }

    //------------< Interface specific methods >
    @Override
    public boolean isConfigFound(SlingHttpServletRequest request) throws HttpCacheReposityAccessException {
        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (doesThisConfigMatch(cacheConfig, request)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given cache config matches for the request.
     *
     * @param cacheConfig
     * @param request
     * @return
     */
    private boolean doesThisConfigMatch(HttpCacheConfig cacheConfig, SlingHttpServletRequest request) throws
            HttpCacheReposityAccessException {
        // Match authentication requirement.
        if (UserUtils.isAnonymous(request.getResourceResolver().getUserID())) {
            if (AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST.equals(cacheConfig
                    .getAuthenticationRequirement())) {
                return false;
            }
        } else {
            if (AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST.equals(cacheConfig.getAuthenticationRequirement
                    ())) {
                return false;
            }
        }

        // Match request URI.
        boolean isUriMatchFound = false;
        for (Pattern pattern : cacheConfig.getRequestURIsAsRegEx()) {
            if (pattern.matcher(request.getRequestURI()).matches()) {
                isUriMatchFound = true;
                break;
            }
        }
        if (!isUriMatchFound) {
            log.trace("Request matches with the cache config request uri pattern.");
            return false;
        }

        // Match blacklisted URI.
        for (Pattern pattern : cacheConfig.getBlacklistedURIsAsRegEx()) {
            if (pattern.matcher(request.getRequestURI()).matches()) {
                // Flip the flag when there is a match with blacklisted uri.
                isUriMatchFound = false;
                break;
            }
        }
        if (!isUriMatchFound) {
            log.trace("Request matches with the blacklist and hence match cancelled.");
            return false;
        }

        // Match groups.
        if (UserUtils.isAnonymous(request.getResourceResolver().getUserID())) {
            // If the user is anonymous, no matching with groups required.
            return true;
        } else {
            // Case of authenticated requests.
            if (cacheConfig.getUserGroupNames().size() > 0) {
                try {
                    List<String> requestUserGroupNames = UserUtils.getUserGroupMembershipNames(request
                            .getResourceResolver().adaptTo(User.class));

                    // At least one of the group in config should match.
                    boolean isGroupMatchFound = CollectionUtils.containsAny(cacheConfig.getUserGroupNames(),
                            requestUserGroupNames);
                    if (!isGroupMatchFound) {
                        log.debug("Group didn't match and hence rejecting the cache config.");
                    }
                    return isGroupMatchFound;
                } catch (RepositoryException e) {
                    throw new HttpCacheReposityAccessException("Unable to access group information of request user.",
                            e);
                }
            }
        }
        return true;
    }

    @Override
    public HttpCacheConfig resolveConfig(SlingHttpServletRequest request) throws HttpCacheConfigConflictException,
            HttpCacheReposityAccessException {
        List<HttpCacheConfig> matchingConfigs = new ArrayList<>();

        // Collect all the matching cache configs.
        for (HttpCacheConfig cacheConfig : cacheConfigs) {
            if (doesThisConfigMatch(cacheConfig, request)) {
                matchingConfigs.add(cacheConfig);
            }
        }

        // If there is more than one matching cache config, throw Cache Conflict exception.
        // Ideally, when there are multiple configs matching, the one with closest match has to be chosen based on
        // certain ranking mechanism. For the sake of simplicity, it' been reserved for future implementation.
        if (matchingConfigs.size() == 1) {
            return matchingConfigs.get(0);
        } else if (matchingConfigs.size() > 1) {
            throw new HttpCacheConfigConflictException("Multiple matching cache configs found and unable to " +
                    "determine" + " the closest match");
        } else {
            return null;
        }
    }
}
