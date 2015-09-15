/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Provides options to configure how to generate checksums using {@link ChecksumGenerator}.
 */
@ProviderType
public abstract class AbstractChecksumGeneratorOptions implements ChecksumGeneratorOptions {

    protected Set<String> paths = new HashSet<String>();

    protected Set<String> includedNodeTypes = new HashSet<String>();

    protected Set<String> excludedNodeTypes = new HashSet<String>();

    protected Set<String> excludedProperties = new HashSet<String>();

    protected Set<String> sortedProperties = new HashSet<String>();

    public void addPaths(String... paths) {
        if (paths != null) {
            this.paths.addAll(Arrays.asList(paths));
        }
    }

    public Set<String> getPaths() {
        return this.paths;
    }

    public void addIncludedNodeTypes(String... includedNodeTypes) {
        if (includedNodeTypes != null) {
            this.includedNodeTypes.addAll(Arrays.asList(includedNodeTypes));
        }
    }

    public Set<String> getIncludedNodeTypes() {
        return this.includedNodeTypes;
    }

    public void addExcludedNodeTypes(String... excludedNodeTypes) {
        if (excludedNodeTypes != null) {
            this.excludedNodeTypes.addAll(Arrays.asList(excludedNodeTypes));
        }
    }

    public Set<String> getExcludedNodeTypes() {
        return this.excludedNodeTypes;
    }

    public void addExcludedProperties(String... excludedProperties) {
        if (excludedProperties != null) {
            this.excludedProperties.addAll(Arrays.asList(excludedProperties));
        }
    }

    public Set<String> getExcludedProperties() {
        return this.excludedProperties;
    }

    public void addSortedProperties(String... sortedProperties) {
        if (sortedProperties != null) {
            this.sortedProperties.addAll(Arrays.asList(sortedProperties));
        }
    }

    public Set<String> getSortedProperties() {
        return this.sortedProperties;
    }

    protected Set<String> getPathsFromQuery(ResourceResolver resourceResolver, String language, String query) {
        if (StringUtils.isBlank(query)) {
            return Collections.EMPTY_SET;
        }

        Set<String> paths = new HashSet<String>();
        language = StringUtils.defaultIfEmpty(language, "xpath");
        Iterator<Resource> resources = resourceResolver.findResources(query, language);

        while (resources.hasNext()) {
            paths.add(resources.next().getPath());
        }

        return paths;
    }


    protected Set<String> getPathsFromInputstream(InputStream is) throws IOException {
        if (is == null) {
            return Collections.EMPTY_SET;
        }

        Set<String> paths = new HashSet<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        try {
            String path;
            while ((path = br.readLine()) != null) {
                paths.add(path);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return paths;
    }


    public String toString() {
        InfoWriter iw = new InfoWriter();

        iw.title("Checksum Generator Options");
        iw.message("Paths: {}", this.getPaths());
        iw.message("Node Type Includes: {}", this.getIncludedNodeTypes());
        iw.message("Node Type Excludes: {}", this.getExcludedNodeTypes());
        iw.message("Property Excludes: {}", this.getExcludedProperties());
        iw.message("Sorted Properties: {}", this.getSortedProperties());

        return iw.toString();
    }
}
