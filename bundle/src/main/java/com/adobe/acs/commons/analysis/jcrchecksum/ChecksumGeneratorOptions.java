/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */

package com.adobe.acs.commons.analysis.jcrchecksum;

import org.osgi.annotation.versioning.ProviderType;

import java.util.Collections;
import java.util.Set;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface ChecksumGeneratorOptions {
    String DATA = "data";

    String PATHS = "paths";

    String QUERY = "query";

    String QUERY_TYPE = "queryType";

    String NODES_TYPES = "nodeTypes";

    String NODE_TYPE_EXCLUDES = "excludeNodeTypes";

    String PROPERTY_EXCLUDES = "excludeProperties";

    String SORTED_PROPERTIES = "sortedProperties";

    String SUB_TREE_EXCLUDES = "excludeSubTrees";

    String NODE_NAME_EXCLUDES = "excludeNodeNames";

    /**
     * For a node to be checksumable, its primaryType must exists in getIncludedNodesTypes() and not exist in
     * getExcludedNodeTypes().
     * @return the nodes types which are candidates for aggregation.
     */
    Set<String> getIncludedNodeTypes();

    /**
     * @return the node types which are not candidates for aggregation and cannot be aggregated under aggregation
     * candidates.
     */
    Set<String> getExcludedNodeTypes();

    /**
     *
     * @return the property names that should not be included as part of the checksum hash
     */
    Set<String> getExcludedProperties();

    /**
     * @return the property names whose multi-value order as defined in the JCR should be respected.
     */
    Set<String> getSortedProperties();

    /**
     * @return the named node subTrees to exclude (the matching node and any sub-nodes will NOT be traversed/checksumed)
     */
    default Set<String> getExcludedSubTrees() {
        return  Collections.EMPTY_SET;
    }

    /**
     * @return the nodeNames to exclude (sub-nodes WILL be traverse/checksumed)
     */
    default Set<String> getExcludedNodeNames() {
        return  Collections.EMPTY_SET;
    }
}