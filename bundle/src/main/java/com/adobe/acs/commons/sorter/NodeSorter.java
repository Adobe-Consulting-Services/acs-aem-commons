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
package com.adobe.acs.commons.sorter;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import java.util.Comparator;

public interface NodeSorter {

    /**
     * The name of a sorter, e.g. 'byTitle' or 'byMyCustomProperty'
     * It will be used to find the actual sorter from the {@link SortNodesOperation#RP_SORTER_NAME}
     * request parameter
     */
    String getName();

    /**
     * Optional label to show in the UI drop-down to select a sorter.
    */
    default String getLabel(){
        return getName();
    }

    /**
     * Create a comparator to sort nodes.
     * Implementations can read additional parameters from request, e.g.
     * whether search should be case-sensitive, ascending/descending, etc.
     */
    Comparator<Node> createComparator(HttpServletRequest request);

 }
