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
package com.adobe.acs.commons.sorter.impl;

import org.apache.jackrabbit.JcrConstants;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Comparator;

class HierarchyNodeComparator implements Comparator<Node> {
    public static final String RP_NOT_HIERARCHY_FIRST = ":nonHierarchyFirst";

    public static final HierarchyNodeComparator INSTANCE = new HierarchyNodeComparator();

    private HierarchyNodeComparator(){
        // ensure singleton
    }

    @Override
    public int compare(Node n1, Node n2) {
        try {
            return Boolean.compare(
                    n1.isNodeType(JcrConstants.NT_HIERARCHYNODE),
                    n2.isNodeType(JcrConstants.NT_HIERARCHYNODE));
        } catch (RepositoryException e) {
            return 0;
        }
    }
}
