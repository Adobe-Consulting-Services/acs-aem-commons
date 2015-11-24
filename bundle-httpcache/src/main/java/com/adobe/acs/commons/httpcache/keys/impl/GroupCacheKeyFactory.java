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
    public class GroupCacheKey implements CacheKey {
        
        /* This key is composed of uri, list of user groups and authentication requirement details */
        private String uri;
        private List<String> userGroups;
        private String authenticationRequirement;

        public GroupCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
                HttpCacheKeyCreationException {

            this.uri = request.getRequestURI();
            // Note - Custom attribute in this case is user group names.
            this.userGroups = cacheConfig.getCustomConfigAttributes();
            this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
        }

        public GroupCacheKey(String uri, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {

            this.uri = uri;
            // Note - Custom attribute in this case is user group names.
            this.userGroups = cacheConfig.getCustomConfigAttributes();
            this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
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

        @Override
        public String toString() {

            StringBuilder formattedString = new StringBuilder(this.uri.replace('/', '_')).append("_");
            for (String userGroup : userGroups) {
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
}
