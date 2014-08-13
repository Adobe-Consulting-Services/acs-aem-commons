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

package com.adobe.acs.commons.workflow.bulk.removal;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

import java.util.Comparator;

public class WorkflowInstanceFolderComparator implements Comparator<Resource> {
    @Override
    public int compare(final Resource resource1, final Resource resource2) {

        final String[] segments1 = StringUtils.split(resource1.getName(), "_");
        final String[] segments2 = StringUtils.split(resource2.getName(), "_");

        int result = 0;
        for(int i = 0; i < segments1.length; i++) {
            result = this.compare(segments1[0], segments2[0]);
            if (result != 0) {
                return result;
            }
        }

        return result;
    }

    private int compare(final String intString1, final String intString2) {
        Integer integer1 = new Integer(intString1);
        Integer integer2 = new Integer(intString2);

        return integer2.compareTo(integer1);
    }

}
