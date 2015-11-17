package com.adobe.acs.commons.httpcache.keys.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.List;

/**
 * Key factory which generated keys based on user groups.
 */
@Component
@Service
public class GroupCacheKeyFactory implements CacheKeyFactory {

    @Override
    public CacheKey build(final SlingHttpServletRequest slingHttpServletRequest, final HttpCacheConfig cacheConfig)
            throws HttpCacheKeyCreationException {
        return new GroupCacheKey(slingHttpServletRequest, cacheConfig);
    }

    /**
     * The GroupCacheKey is a custom CacheKey bound to this particular factory.
     */
    public class GroupCacheKey implements CacheKey {
        /* This key is composed of uri, list of user groups and authenticatin details */
        private String uri;
        private List<String> userGroups;
        private String authenticationRequirement;

        public GroupCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
                HttpCacheKeyCreationException {
            this.uri = request.getRequestURI();
            this.userGroups = cacheConfig.getUserGroups();
            this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            GroupCacheKey that = (GroupCacheKey) o;

            return new EqualsBuilder().append(uri, that.uri).append(userGroups, that.userGroups).append
                    (authenticationRequirement, that.authenticationRequirement).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(uri).append(userGroups).append(authenticationRequirement)
                    .toHashCode();
        }
    }
}
