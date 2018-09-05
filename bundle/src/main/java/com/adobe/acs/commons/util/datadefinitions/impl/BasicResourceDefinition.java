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
package com.adobe.acs.commons.util.datadefinitions.impl;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

public class BasicResourceDefinition implements ResourceDefinition {
    protected String id = null;
    protected String name = null;
    protected String title = null;
    protected String description = null;
    protected  String path = null;
    protected Map<String, String> localizedTitles = new HashMap<>();
    private boolean ordered = false;

    public BasicResourceDefinition(String name) {
        this.setName(name);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Map<String, String> getLocalizedTitles() {
        return localizedTitles;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isOrdered() {
        return ordered;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocalizedTitles(Map<String,String> localizedTitles) {
        this.localizedTitles = localizedTitles;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }

        BasicResourceDefinition rhs = (BasicResourceDefinition) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(getId(), rhs.getId())
                .isEquals();
    }

    public int hashCode() {
        // you pick a hard-coded, randomly chosen, non-zero, odd number
        // ideally different for each class
        return new HashCodeBuilder(3748317, 3479337)
                .append(getId())
                .toHashCode();
    }
}