package com.adobe.acs.commons.httpcache.keys.impl;

import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.httpcache.store.mem.MemCacheKey;
import org.apache.commons.collections.list.TreeList;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.sling.api.SlingHttpServletRequest;

import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@Service
public class GroupCacheKeyFactory implements CacheKeyFactory {

    private static final String[] DEFAULT_ALLOWED_GROUPS = new String[]{};
    private String[] allowedGroups = new String[]{};
    @Property(label = "Allows Groups",
            description = "Groups that are allowed to be used to construct the Cache Key. "
                    + "If no groups are defined, all groups are allowed.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_ALLOWED_GROUPS = "groups.whitelist";

    @Override
    public CacheKey build(final SlingHttpServletRequest slingHttpServletRequest) throws HttpCacheKeyCreationException {
        return new GroupCacheKey(slingHttpServletRequest);
    }

    @Activate
    public void activate(Map<String, Object> config) {
        this.allowedGroups = PropertiesUtil.toStringArray(config.get(PROP_ALLOWED_GROUPS), new String[]{});
    }

    /**
     * The GroupCacheKey is a custom CacheKey bound to this particular factory.
     *
     * This implementation should implement all persistence key types (ie. MemCacheKey, DiskCacheKey, JcrCacheKey)
     * based on what it supports.
     */
    public class GroupCacheKey implements MemCacheKey {
        private String uri;
        private List<String> userGroups;

        public GroupCacheKey(SlingHttpServletRequest request) throws HttpCacheKeyCreationException {
            try {
                this.uri = new String(request.getPathInfo());
                this.userGroups = new TreeList();

                final Authorizable authorizable = request.adaptTo(Authorizable.class);
                final Iterator<Group> groups = authorizable.memberOf();

                while (groups.hasNext()) {
                    final Group group = groups.next();

                    if (ArrayUtils.isEmpty(allowedGroups) || ArrayUtils.contains(allowedGroups, group)) {
                        this.userGroups.add(new String(group.getPrincipal().getName()));
                    }
                }
            } catch(RepositoryException e) {
                throw new HttpCacheKeyCreationException(e);
            }
        }

        /**
         * Getter used to expose the URI to allow comparison of two objects of this same Class type.
         *
         * @return the uri
         */
        public String getUri() {
            return this.uri;
        }

        /**
         * Getter used to expose the Groups to allow comparison of two objects of this same Class type.
         *
         * @return the groups
         */
        public List<String> getGroups() {
            return this.userGroups;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GroupCacheKey that = (GroupCacheKey) o;

            return new EqualsBuilder()
                    .append(this.getUri(), that.getUri())
                    .append(this.getGroups(), that.getGroups())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(this.getUri())
                    .append(this.getGroups())
                    .toHashCode();
        }

        /**
         * Provides casting of the object to the persistence-specific CacheKey type.
         *
         * @return this object as a MemCacheKey
         */
        @Override
        public MemCacheKey asMemCacheKey() {
            return this;
        }

        @Override
        public boolean isInvalidatedBy(final String path) {
            // TODO we can get more fancy with this as needed
            return StringUtils.startsWith(this.uri, path);
        }
    }
}
