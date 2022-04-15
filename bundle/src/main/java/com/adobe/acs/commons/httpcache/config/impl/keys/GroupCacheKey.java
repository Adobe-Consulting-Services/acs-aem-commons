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
package com.adobe.acs.commons.httpcache.config.impl.keys;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.AbstractCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.sling.api.SlingHttpServletRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The GroupCacheKey
 * CacheKey that differentiates on user group authentication.
 */
public
class GroupCacheKey extends AbstractCacheKey implements CacheKey, Serializable {

    /* This key is composed of uri, list of user groups and authentication requirement details */
    private List<String> cacheKeyUserGroups;

    public GroupCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, List<String> userGroups) throws
            HttpCacheKeyCreationException {

        super(request, cacheConfig);
        this.cacheKeyUserGroups = Optional.ofNullable(userGroups)
                .map(list -> (List<String>) new ArrayList<>(list))
                .orElse(Collections.emptyList());
    }

    public GroupCacheKey(String uri, HttpCacheConfig cacheConfig, List<String> userGroups) throws HttpCacheKeyCreationException {
        super(uri, cacheConfig);
        this.cacheKeyUserGroups = Optional.ofNullable(userGroups)
                .map(list -> (List<String>) new ArrayList<>(list))
                .orElse(Collections.emptyList());
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
