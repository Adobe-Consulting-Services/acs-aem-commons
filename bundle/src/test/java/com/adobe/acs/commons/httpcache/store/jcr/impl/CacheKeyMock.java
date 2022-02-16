/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.httpcache.store.jcr.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.adobe.acs.commons.httpcache.keys.CacheKey;

public class CacheKeyMock implements CacheKey {
    private String uri;
    private String hierarchyResourcePath;
    private int hashCode;
    private String toString;

    public CacheKeyMock(String uri, String hierarchyResourcePath, int hashCode, String toString) {

        this.uri = uri;
        this.hierarchyResourcePath = hierarchyResourcePath;
        this.hashCode = hashCode;
        this.toString = toString;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getHierarchyResourcePath() {
        return hierarchyResourcePath;
    }

    @Override
    public long getExpiryForCreation() {
        return -1;
    }

    @Override
    public long getExpiryForAccess() {
        return -1;
    }

    @Override
    public long getExpiryForUpdate() {
        return -1;
    }

    @Override
    public boolean isInvalidatedBy(CacheKey cacheKey) {
        return false;
    }

    protected void writeObject(ObjectOutputStream o) throws IOException {
        o.writeObject(toString);
        o.writeObject(uri);
        o.writeInt(hashCode);
        o.writeObject(hierarchyResourcePath);
    }

    protected void readObject(ObjectInputStream o) throws IOException, ClassNotFoundException {

        toString = (String) o.readObject();
        uri = (String) o.readObject();
        hashCode = o.readInt();
        hierarchyResourcePath = (String) o.readObject();
    }

    public int hashCode() {
        return hashCode;
    }

    public String toString() {
        return toString;
    }

    public boolean equals(Object o) {
        if (o instanceof CacheKeyMock) {
            CacheKeyMock other = (CacheKeyMock) o;
            return ((this.hashCode == other.hashCode)
                    && (this.hierarchyResourcePath.equals(other.hierarchyResourcePath)) 
                    && (this.uri.equals(other.uri))
                    && (this.toString.equals(other.toString)));
        } else {
            return false;
        }
    }
}
