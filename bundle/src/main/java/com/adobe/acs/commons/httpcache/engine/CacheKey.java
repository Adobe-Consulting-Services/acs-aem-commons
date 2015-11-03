package com.adobe.acs.commons.httpcache.engine;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Represents the cache key used in cache stores. Modeled as value object.
 */

// TODO - Make userGroup optional so that it works for anonymous as well.
public class CacheKey {
    /** Request uri in http context */
    private String uri;
    /** Array of aem user groups **/
    private String[] userGroups;

    /**
     * Making constructor private forcing instances to be made through <code>build</code> method.
     */
    private CacheKey() {
    }

    /**
     * Get the url
     *
     * @return
     */
    public String getUri() {

        return uri;
    }

    /**
     * Get the user groups.
     *
     * @return Array of user groups.
     */
    public String[] getUserGroups() {
        return userGroups;
    }

    /**
     * Build the required state from the request and the applicable cache config..
     *
     * @param request
     * @param cacheConfig
     * @return
     */
    public CacheKey build(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
        CacheKey key = new CacheKey();
        key.uri = request.getRequestURI();
        key.userGroups = (String[]) cacheConfig.getMandatoryUserGroupNames().toArray();
        return this;
    }

    // TODO - Need to thoroughly test the equality / hashcode using unit test.
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        CacheKey cacheKey = (CacheKey) o;

        return new EqualsBuilder().append(uri, cacheKey.uri).append(userGroups, cacheKey.userGroups).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(uri).append(userGroups).toHashCode();
    }

}
