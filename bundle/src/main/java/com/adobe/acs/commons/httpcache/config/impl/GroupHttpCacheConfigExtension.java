/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
    package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.AbstractCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.httpcache.util.UserUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Implementation for custom cache config extension and associated cache key creation based on aem groups. This cache
 * config extension accepts the http request only if at least one of the configured groups is present in the request
 * user's group membership list. Made it as config factory as it could move along 1-1 with HttpCacheConfig.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Group based extension for HttpCacheConfig and CacheKeyFactory.",
           description = "HttpCacheConfig custom extension for group based configuration and associated cache key "
                   + "creation.",
           metatype = true,
           configurationFactory = true,
           policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(name = "webconsole.configurationFactory.nameHint",
                value = "Allowed user groups: {httpcache.config.extension.user-groups.allowed}",
                propertyPrivate = true)
})
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
    public CacheKey build(final String resourcePath, final HttpCacheConfig cacheConfig)
            throws HttpCacheKeyCreationException {
        return new GroupCacheKey(resourcePath, cacheConfig);
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
    class GroupCacheKey extends AbstractCacheKey implements CacheKey, Serializable {

        /* This key is composed of uri, list of user groups and authentication requirement details */
        private List<String> cacheKeyUserGroups;

        public GroupCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
                HttpCacheKeyCreationException {

            super(request, cacheConfig);
            this.cacheKeyUserGroups = userGroups;
        }

        public GroupCacheKey(String uri, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {
            super(uri, cacheConfig);
            this.cacheKeyUserGroups = userGroups;
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }

            if (o == null) {
                return false;
            }

            GroupCacheKey that = (GroupCacheKey) o;

            return new EqualsBuilder()
                    .append(getUri(), that.getUri())
                    .append(cacheKeyUserGroups, that.cacheKeyUserGroups)
                    .append(getAuthenticationRequirement(), that.getAuthenticationRequirement())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(getUri())
                    .append(cacheKeyUserGroups)
                    .append(getAuthenticationRequirement()).toHashCode();
        }

        @Override
        public String toString() {
            StringBuilder formattedString = new StringBuilder(this.uri).append(" [GROUPS:");
            formattedString.append(StringUtils.join(cacheKeyUserGroups, "|"));
            formattedString.append("] [AUTH_REQ:" + getAuthenticationRequirement() + "]");
            return formattedString.toString();
        }

        /** For Serialization **/
        private void writeObject(ObjectOutputStream o) throws IOException
        {
            parentWriteObject(o);
            final Object[] userGroupArray = cacheKeyUserGroups.toArray();
            o.writeObject(StringUtils.join(userGroupArray, ","));
        }

        /** For De serialization **/
        private void readObject(ObjectInputStream o)
                throws IOException, ClassNotFoundException {

            parentReadObject(o);
            final String userGroupsStr = (String) o.readObject();
            final String[] userGroupStrArray = userGroupsStr.split(",");
            cacheKeyUserGroups = Arrays.asList(userGroupStrArray);
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

        log.info("GroupHttpCacheConfigExtension activated/modified.");
    }
}
