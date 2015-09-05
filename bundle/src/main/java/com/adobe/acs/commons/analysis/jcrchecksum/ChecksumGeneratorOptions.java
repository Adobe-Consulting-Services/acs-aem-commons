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

package com.adobe.acs.commons.analysis.jcrchecksum;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * Provides options to configure how to generate checksums using {@link ChecksumGenerator}.
 * 
 */
@ProviderType
public class ChecksumGeneratorOptions {
    private HashSet<String> nodeTypeIncludes;
    private HashSet<String> nodeTypeExcludes;
    private HashSet<String> propertyExcludes;
    private HashSet<String> sortMultiValues;
    boolean doNotUseDefaultNodeTypeIncludes;
    boolean doNotUseDefaultNodeTypeExcludes;
    boolean doNotUseDefaultPropertyExcludes;
    boolean doNotUseDefaultSortedMultiValueProperties;

    /**
     * Default nodetypes to use for the hash calculation.
     */
    public static final List<String> DEFAULT_INCLUDED_NODETYPES =
        new ArrayList<String>();

    static {
        DEFAULT_INCLUDED_NODETYPES.add("cq:PageContent");
        DEFAULT_INCLUDED_NODETYPES.add("dam:AssetContent");
    }

    /**
     * Default nodetypes to exclude from the hash calculation.
     */
    public static final List<String> DEFAULT_EXCLUDED_NODETYPES =
        new ArrayList<String>();

    static {
        DEFAULT_EXCLUDED_NODETYPES.add("rep:ACL");
    }

    /**
     * These JCR property names are the default ones that are excluded when
     * generating a hash of a tree of nodes.
     */
    public static final List<String> DEFAULT_EXCLUDED_PROPERTIES =
        new ArrayList<String>();

    static {
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:created");
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:createdBy");
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:uuid");
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:lastModified");
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:lastModifiedBy");
        DEFAULT_EXCLUDED_PROPERTIES.add("cq:lastModified");
        DEFAULT_EXCLUDED_PROPERTIES.add("cq:lastModifiedBy");
        DEFAULT_EXCLUDED_PROPERTIES.add("cq:lastReplicated");
        DEFAULT_EXCLUDED_PROPERTIES.add("cq:lastReplicatedBy");
        DEFAULT_EXCLUDED_PROPERTIES.add("cq:lastReplicationAction");
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:versionHistory");
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:predecessors");
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:baseVersion");
        DEFAULT_EXCLUDED_PROPERTIES.add("jcr:lastModifiedBy");
        DEFAULT_EXCLUDED_PROPERTIES.add("cq:lastModified");
    }

    /**
     * These JCR multi-value property names are the default ones that will have
     * their values sorted before generating a checksum.
     */
    public static final List<String> DEFAULT_SORTED_MULTI_VALUES =
        new ArrayList<String>();

    static {
        DEFAULT_SORTED_MULTI_VALUES.add("cq:tags");
    }

    public void disableDefaultNodeTypeIncludes() {
        this.doNotUseDefaultNodeTypeIncludes = true;
    }

    public void setNodeTypeIncludes(HashSet<String> nodeTypeIncludes) {
        this.nodeTypeIncludes = new HashSet<String>();
        this.nodeTypeIncludes.addAll(nodeTypeIncludes);
    }

    /**
     * Sets the nodetypes to include in checksum generation.  An aggregate checksum would be generated
     * for the nodes having the matching nodetypes. 
     */
    public void setNodeTypeIncludes(String[] nodeTypeIncludes) {
        if (nodeTypeIncludes != null) {
            this.nodeTypeIncludes = new HashSet<String>();

            for (String nodeTypeExclude : nodeTypeIncludes)
                this.nodeTypeIncludes.add(nodeTypeExclude);
        }
    }

    public HashSet<String> getNodeTypeIncludes() {
        HashSet<String> copyOfNodeTypeIncludes = new HashSet<String>();
        if (this.nodeTypeIncludes != null)
            copyOfNodeTypeIncludes.addAll(this.nodeTypeIncludes);

        if (!this.doNotUseDefaultNodeTypeIncludes)
            copyOfNodeTypeIncludes.addAll(DEFAULT_INCLUDED_NODETYPES);
        return copyOfNodeTypeIncludes;
    }

    public void disableDefaultNodeTypeExcludes() {
        this.doNotUseDefaultNodeTypeExcludes = true;
    }

    public void setNodeTypeExcludes(HashSet<String> nodeTypeExcludes) {
        this.nodeTypeExcludes = new HashSet<String>();
        this.nodeTypeExcludes.addAll(nodeTypeExcludes);
    }

    /**
     * Sets the nodetypes to exclude from checksum generation
     * 
     * @param nodeTypeExcludess
     */
    public void setNodeTypeExcludes(String[] nodeTypeExcludes) {
        if (nodeTypeExcludes != null) {
            this.nodeTypeExcludes = new HashSet<String>();
            for (String nodeTypeExclude : nodeTypeExcludes)
                this.nodeTypeExcludes.add(nodeTypeExclude);
        }
    }

    public HashSet<String> getNodeTypeExcludes() {
        HashSet<String> copyOfNodeTypeExcludes = new HashSet<String>();
        if (this.nodeTypeExcludes != null)
            copyOfNodeTypeExcludes.addAll(this.nodeTypeExcludes);

        if (!this.doNotUseDefaultNodeTypeExcludes)
            copyOfNodeTypeExcludes.addAll(DEFAULT_EXCLUDED_NODETYPES);
        return copyOfNodeTypeExcludes;
    }

    public void disableDefaultPropertyExcludes() {
        this.doNotUseDefaultPropertyExcludes = true;
    }

    public void setPropertyExcludes(HashSet<String> propertyExcludes) {
        this.propertyExcludes = new HashSet<String>();
        this.propertyExcludes.addAll(propertyExcludes);
    }

    /**
     * Sets the names of properties to exclude from checksum generation.
     */
    public void setPropertyExcludes(String[] propertyExcludes) {
        if (propertyExcludes != null) {
            this.propertyExcludes = new HashSet<String>();
            for (String propertyExclude : propertyExcludes)
                this.propertyExcludes.add(propertyExclude);
        }
    }

    public HashSet<String> getPropertyExcludes() {
        HashSet<String> copyOfPropertyExcludes = new HashSet<String>();
        if (this.propertyExcludes != null)
            copyOfPropertyExcludes.addAll(this.propertyExcludes);

        if (!this.doNotUseDefaultPropertyExcludes)
            copyOfPropertyExcludes.addAll(DEFAULT_EXCLUDED_PROPERTIES);
        return copyOfPropertyExcludes;
    }

    public void disableDefaultSortedMultiValueProperties() {
        this.doNotUseDefaultSortedMultiValueProperties = true;
    }

    public void setSortedMultiValueProperties(HashSet<String> sortMultiValues) {
        this.sortMultiValues = new HashSet<String>();
        this.sortMultiValues.addAll(sortMultiValues);
    }

    /**
     * Sets the names of multi-value properties to sort values of during checksum generation.
     * 
     * @param sortMultiValues
     */
    public void setSortedMultiValueProperties(String[] sortMultiValues) {
        if (sortMultiValues != null) {
            this.sortMultiValues = new HashSet<String>();
            for (String nodeType : sortMultiValues)
                this.sortMultiValues.add(nodeType);
        }
    }

    public HashSet<String> getSortedMultiValueProperties() {
        HashSet<String> copyOfSortedMultiValueProperties =
            new HashSet<String>();
        if (this.sortMultiValues != null)
            copyOfSortedMultiValueProperties.addAll(this.sortMultiValues);

        if (!this.doNotUseDefaultSortedMultiValueProperties)
            copyOfSortedMultiValueProperties
                .addAll(DEFAULT_SORTED_MULTI_VALUES);
        return copyOfSortedMultiValueProperties;
    }

}
