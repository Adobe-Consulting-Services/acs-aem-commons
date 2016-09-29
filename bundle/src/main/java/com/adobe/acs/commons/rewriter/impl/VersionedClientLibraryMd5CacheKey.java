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
package com.adobe.acs.commons.rewriter.impl;

import com.adobe.granite.ui.clientlibs.HtmlLibrary;
import com.adobe.granite.ui.clientlibs.LibraryType;
import com.google.common.base.Objects;

class VersionedClientLibraryMd5CacheKey {
    private final String path;
    private final LibraryType type;

    VersionedClientLibraryMd5CacheKey(HtmlLibrary htmlLibrary) {
        this.path = htmlLibrary.getLibraryPath();
        this.type = htmlLibrary.getType();
    }

    VersionedClientLibraryMd5CacheKey(String path, LibraryType type) {
        this.path = path;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VersionedClientLibraryMd5CacheKey other = (VersionedClientLibraryMd5CacheKey) obj;

        return Objects.equal(this.path, other.path) && Objects.equal(this.type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.path, this.type);
    }

    @Override
    public String toString() {
        return new StringBuilder(path).append('.').append(type.name().toLowerCase()).toString();
    }

}