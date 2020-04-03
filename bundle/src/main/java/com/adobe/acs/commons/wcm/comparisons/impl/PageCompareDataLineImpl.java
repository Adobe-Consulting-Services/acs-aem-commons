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
package com.adobe.acs.commons.wcm.comparisons.impl;

import com.adobe.acs.commons.version.impl.EvolutionConfig;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLine;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

class PageCompareDataLineImpl implements PageCompareDataLine {

    private static final int LEN = 40;
    private final String path;
    private final String name;
    private final String value;
    private final int depth;

    PageCompareDataLineImpl(Property property, String basePath, int depth) throws RepositoryException {
        this.path = property.getPath().replace(basePath, "");
        this.name = property.getName();
        this.value = EvolutionConfig.printProperty(property);
        this.depth = depth;
    }

    PageCompareDataLineImpl(Resource resource, String basePath, int depth) {
        this.path = resource.getPath().replace(basePath, "");
        this.name = resource.getName();
        this.value = null;
        this.depth = depth;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PageCompareDataLineImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        PageCompareDataLineImpl other = (PageCompareDataLineImpl) obj;
        return new EqualsBuilder()
                .append(path, other.getPath())
                .append(name, other.getName())
                .append(value, other.getValueString())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(79, 11)
                .append(path)
                .append(name)
                .append(value).toHashCode();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getUniqueName() {
        return path.replace("/jcr:content", "").replaceAll("/","").replaceAll(":","-");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValueString() {
        return value;
    }

    @Override
    public String getValueStringShort() {
        return StringUtils.left(value, LEN);
    }

    @Override
    public int getDepth() {
        return depth;
    }

}
