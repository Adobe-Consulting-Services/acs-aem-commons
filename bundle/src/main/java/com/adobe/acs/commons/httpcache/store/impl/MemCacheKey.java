package com.adobe.acs.commons.httpcache.store.impl;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Key for the cache entry in mem store.
 */
class MemCacheKey {
    /** Request uri in http context */
    private String uri;
    /** Array of aem user groups **/
    private String[] userGroups;

    /**
     * Creates <code>MemCacheKey</code>. Use <code>build*</code> methods to supply parameters.
     */
    MemCacheKey() {
    }

    /**
     * Construct a Mem cache key that can be used for caching. This constructor takes deep copy of parameters making the
     * object suitable for caching avoiding any memory leaks.
     *
     * @param uri
     * @param userGroups
     */
    public MemCacheKey buildForCaching(String uri, String[] userGroups) {
        // Taken copy of arguments before caching them to avoid chances of memory leak.
        // Take copy of original uri.
        this.uri = new String(uri);

        // Take copy of the original array
        this.userGroups = new String[userGroups.length];
        for (String userGroup : userGroups) {
            ArrayUtils.add(this.userGroups, userGroup);
        }
        return this;
    }

    /**
     * Construct a Mem cache key that can be used for lookups. This doesn't take a copy of parameters and hence suitable
     * for lookups.
     *
     * @param uri
     * @param userGroups
     */
    public MemCacheKey buildForLookups(String uri, String[] userGroups) {
        this.uri = uri;
        this.userGroups = userGroups;
        return this;
    }

    /**
     * Get Uri.
     *
     * @return
     */
    public String getUri() {
        return uri;
    }

    /**
     * Get user groups.
     *
     * @return
     */
    public String[] getUserGroups() {
        return userGroups;
    }

    // TODO - Need to thoroughly test the equality / hashcode using unit test.

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        MemCacheKey that = (MemCacheKey) o;

        return new EqualsBuilder().append(uri, that.uri).append(userGroups, that.userGroups).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(uri).append(userGroups).toHashCode();
    }
}
