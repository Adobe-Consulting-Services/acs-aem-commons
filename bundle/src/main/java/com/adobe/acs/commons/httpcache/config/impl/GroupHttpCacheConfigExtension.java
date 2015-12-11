package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.httpcache.util.UserUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Implementation for custom cache config extension and associated cache key creation based on aem groups. This cache
 * config extension accepts the http request only if at least one of the configured groups is present in the request
 * user's group membership list. Made it as config factory as it could move along 1-1 with HttpCacheConfig.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Group based extension for HttpCacheConfig and CacheKeyFactory.",
           description = "HttpCacheConfig custom extension for group based configuration and associated cache key " +
                   "creation.",
           metatype = true,
           configurationFactory = true,
           policy = ConfigurationPolicy.REQUIRE
)
@Service
public class GroupHttpCacheConfigExtension implements HttpCacheConfigExtension, CacheKeyFactory {
    private static final Logger log = LoggerFactory.getLogger(GroupHttpCacheConfigExtension.class);

    // Custom cache config attributes
    @Property(label = "Allowed user groups",
              description = "Users groups that are used to accept and create cache keys.",
              unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_USER_GROUPS = "httpcache.config.extension.user-groups.allowed";
    private List<String> userGroups;

    //-------------------------<HttpCacheConfigExtension methods>

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
            HttpCacheRepositoryAccessException {

        // Match groups.
        if (UserUtils.isAnonymous(request.getResourceResolver().getUserID())) {
            // If the user is anonymous, no matching with groups required.
            return true;
        } else {
            // Case of authenticated requests.
            if (userGroups.isEmpty()) {
                // In case custom attributes list is empty.
                if (log.isTraceEnabled()) {
                    log.trace("GroupHttpCacheConfigExtension accepts request [ {} ]", request.getRequestURI());
                }
                return true;
            }

            try {
                List<String> requestUserGroupNames = UserUtils.getUserGroupMembershipNames(request
                        .getResourceResolver().adaptTo(User.class));

                // At least one of the group in config should match.
                boolean isGroupMatchFound = CollectionUtils.containsAny(userGroups, requestUserGroupNames);
                if (!isGroupMatchFound) {
                    log.trace("Group didn't match and hence rejecting the cache config.");
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("GroupHttpCacheConfigExtension accepts request [ {} ]", request.getRequestURI());
                    }
                }
                return isGroupMatchFound;
            } catch (RepositoryException e) {
                throw new HttpCacheRepositoryAccessException("Unable to access group information of request user.", e);
            }
        }
    }

    //-------------------------<CacheKeyFactory methods>

    @Override
    public CacheKey build(final SlingHttpServletRequest slingHttpServletRequest, final HttpCacheConfig cacheConfig)
            throws HttpCacheKeyCreationException {

        return new GroupCacheKey(slingHttpServletRequest, cacheConfig);
    }

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {

        // Check if key is instance of GroupCacheKey.
        if (!(key instanceof GroupCacheKey)) {
            return false;
        }
        // Validate if key request uri can be constructed out of uri patterns in cache config.
        return new GroupCacheKey(key.getUri(), cacheConfig).equals(key);
    }

    /**
     * The GroupCacheKey is a custom CacheKey bound to this particular factory.
     */
    class GroupCacheKey implements CacheKey {

        /* This key is composed of uri, list of user groups and authentication requirement details */
        private String uri;
        private List<String> cacheKeyUserGroups;
        private String authenticationRequirement;

        public GroupCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
                HttpCacheKeyCreationException {

            this.uri = request.getRequestURI();
            this.cacheKeyUserGroups = userGroups;
            this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
        }

        public GroupCacheKey(String uri, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {

            this.uri = uri;
            // Note - Custom attribute in this case is user group names.
            this.cacheKeyUserGroups = userGroups;
            this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GroupCacheKey that = (GroupCacheKey) o;

            return new EqualsBuilder().append(uri, that.uri).append(cacheKeyUserGroups, that.cacheKeyUserGroups)
                    .append(authenticationRequirement, that.authenticationRequirement).isEquals();
        }

        @Override
        public int hashCode() {

            return new HashCodeBuilder(17, 37).append(uri).append(cacheKeyUserGroups).append
                    (authenticationRequirement).toHashCode();
        }

        @Override
        public String toString() {

            StringBuilder formattedString = new StringBuilder(this.uri.replace('/', '_')).append("_");
            for (String userGroup : cacheKeyUserGroups) {
                formattedString.append(userGroup).append("_");
            }
            formattedString.append(authenticationRequirement);
            return formattedString.toString();
        }

        @Override
        public String getUri() {
            return this.uri;
        }
    }

    //-------------------------<OSGi Component methods>

    @Activate
    @Modified
    protected void activate(Map<String, Object> configs) {

        // User groups after removing empty strings.
        userGroups = new ArrayList(Arrays.asList(PropertiesUtil.toStringArray(configs.get(PROP_USER_GROUPS), new
                String[]{})));
        ListIterator<String> listIterator = userGroups.listIterator();
        while (listIterator.hasNext()) {
            String value = listIterator.next();
            if (StringUtils.isBlank(value)) {
                listIterator.remove();
            }
        }

        log.info("GroupHttpCacheConfigExtension activated /modified.");
    }
}
