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
package com.adobe.acs.commons.util.visitors;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tree visitor which allows special cases such as how to handle child nodes
 * which will not be traversed further.
 *
 * In short, all nodes identified as container/folder items will be visited
 * recursively, and any child which is not a folder will be visited via a
 * special case method. This allows a cleaner delineation of leaf nodes vs
 * structure nodes.
 */
public class TreeFilteringResourceVisitor extends SimpleFilteringResourceVisitor {

    protected static final String[] TREE_TYPES = {
        JcrConstants.NT_FOLDER,
        JcrResourceConstants.NT_SLING_FOLDER,
        JcrResourceConstants.NT_SLING_ORDERED_FOLDER
    };

    private String[] treeTypes;

    /**
     * Create a standard visitor for commonly used folder structures.
     */
    public TreeFilteringResourceVisitor() {
        this(TREE_TYPES);
    }

    /**
     * Create a visitor for a specific set of folder structures. This is useful
     * for including other things such as nt:unstructured or oak folder types.
     *
     * @param treeTypes List of all node types to consider as folder-level
     * containers.
     */
    public TreeFilteringResourceVisitor(String... treeTypes) {
        this.treeTypes = Optional.ofNullable(treeTypes)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(new String[0]);
        setTraversalFilter(this::isFolder);
    }

    public final boolean isFolder(Resource res) {
        String type = res.getResourceType();
        for (String treeType : treeTypes) {
            if (type.equalsIgnoreCase(treeType)) {
                return true;
            }
        }
        return false;
    }
}
