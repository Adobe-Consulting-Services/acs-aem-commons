/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.httpcache.keys;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.day.cq.commons.PathInfo;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;

public abstract class AbstractCacheKey implements Serializable{

    protected String authenticationRequirement;
    protected String uri;
    protected String resourcePath;
    protected String hierarchyResourcePath;

    public AbstractCacheKey(){

    }

    public AbstractCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
        this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
        this.uri = request.getRequestURI();
        this.resourcePath = unmangle(request.getResource().getPath());
        this.hierarchyResourcePath = makeHierarchyResourcePath(this.resourcePath);
    }

    public AbstractCacheKey(String uri, HttpCacheConfig cacheConfig) {
        this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
        this.uri = uri;
        this.resourcePath = unmangle(new PathInfo(uri).getResourcePath());
        this.hierarchyResourcePath = makeHierarchyResourcePath(this.resourcePath);
    }

    protected void parentWriteObject(ObjectOutputStream o) throws IOException
    {
        o.writeObject(authenticationRequirement);
        o.writeObject(uri);
        o.writeObject(resourcePath);
        o.writeObject(hierarchyResourcePath);
    }

    protected void parentReadObject(ObjectInputStream o)
            throws IOException, ClassNotFoundException {

        authenticationRequirement = (String) o.readObject();
        uri = (String) o.readObject();
        resourcePath = (String) o.readObject();
        hierarchyResourcePath = (String) o.readObject();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getUri())
                .append(getAuthenticationRequirement()).toHashCode();
    }

    public String getAuthenticationRequirement() {
        return authenticationRequirement;
    }

    public String getUri() {
        return uri;
    }

    public String getHierarchyResourcePath() {
        return hierarchyResourcePath;
    }

    public String getResourcePath(){
        return resourcePath;
    }

    public boolean isInvalidatedBy(CacheKey cacheKey) {
        return StringUtils.equals(hierarchyResourcePath, cacheKey.getHierarchyResourcePath());
    }

    protected String makeHierarchyResourcePath(String resourcePath) {
        return StringUtils.substringBefore(resourcePath,"/" + JcrConstants.JCR_CONTENT);
    }

    private String unmangle(String str) {
        str = StringUtils.replace(str, "jcr%3acontent", JcrConstants.JCR_CONTENT);
        return StringUtils.replace(str, "_jcr_content", JcrConstants.JCR_CONTENT);
    }


}
