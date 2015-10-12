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

package com.adobe.acs.commons.analysis.jcrchecksum.impl.options;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.util.InfoWriter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides options to configure how to generate checksums using {@link ChecksumGenerator}.
 */
@ProviderType
public abstract class AbstractChecksumGeneratorOptions implements ChecksumGeneratorOptions {

    protected Set<String> includedNodeTypes = new HashSet<String>();

    protected Set<String> excludedNodeTypes = new HashSet<String>();

    protected Set<String> excludedProperties = new HashSet<String>();

    protected Set<String> sortedProperties = new HashSet<String>();

    public void addIncludedNodeTypes(String... data) {
        if (data != null) {
            this.includedNodeTypes.addAll(Arrays.asList(data));
        }
    }

    public Set<String> getIncludedNodeTypes() {
        return this.includedNodeTypes;
    }

    public void addExcludedNodeTypes(String... data) {
        if (data != null) {
            this.excludedNodeTypes.addAll(Arrays.asList(data));
        }
    }

    public Set<String> getExcludedNodeTypes() {
        return this.excludedNodeTypes;
    }

    public void addExcludedProperties(String... data) {
        if (data != null) {
            this.excludedProperties.addAll(Arrays.asList(data));
        }
    }

    public Set<String> getExcludedProperties() {
        return this.excludedProperties;
    }

    public void addSortedProperties(String... data) {
        if (data != null) {
            this.sortedProperties.addAll(Arrays.asList(data));
        }
    }

    public Set<String> getSortedProperties() {
        return this.sortedProperties;
    }

    public String toString() {
        InfoWriter iw = new InfoWriter();

        iw.title("Checksum Generator Options");
        iw.message("Node Type Includes: {}", this.getIncludedNodeTypes());
        iw.message("Node Type Excludes: {}", this.getExcludedNodeTypes());
        iw.message("Property Excludes: {}", this.getExcludedProperties());
        iw.message("Sorted Properties: {}", this.getSortedProperties());

        return iw.toString();
    }
}
