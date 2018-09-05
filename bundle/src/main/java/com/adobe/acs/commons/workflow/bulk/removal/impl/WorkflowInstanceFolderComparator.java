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

package com.adobe.acs.commons.workflow.bulk.removal.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

import java.util.Comparator;

public class WorkflowInstanceFolderComparator implements Comparator<Resource> {

    private static final int MAX_SEGMENTS = 4; // YYYY-MM-DD_N

    /**
     * Compares resource names that are in the format YYYY-MM-DD_N where _N is an optional positive integer.
     *
     * Sorts in an ascending order.
     *
     * @param workflowFolder1 workflow folder 1
     * @param workflowFolder2 workflow folder 2
     * @return sort descending
     */
    @Override
    public final int compare(final Resource workflowFolder1, final Resource workflowFolder2) {

        final String[] segments1 = StringUtils.split(workflowFolder1.getName(), "-_");
        final String[] segments2 = StringUtils.split(workflowFolder2.getName(), "-_");

        int result = 0;

        for (int i = 0; i < MAX_SEGMENTS; i++) {
            String seg1 = "-1";
            String seg2 = "-1";

            if (i < segments1.length) {
                seg1 = segments1[i];
            }

            if (i < segments2.length) {
                seg2 = segments2[i];
            }

            result = this.compare(seg1, seg2);

            if (result != 0) {
                return result;
            }
        }

        return result;
    }

    private int compare(final String intString1, final String intString2) {
        Integer integer1 = Integer.valueOf(intString1);
        Integer integer2 = Integer.valueOf(intString2);

        return integer1.compareTo(integer2);
    }
}
