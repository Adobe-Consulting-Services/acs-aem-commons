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

import com.adobe.acs.commons.sorter.NodeSorter;
import com.day.cq.commons.JcrLabeledResource;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.Comparator;

import static com.adobe.acs.commons.sorter.impl.HierarchyNodeComparator.RP_NOT_HIERARCHY_FIRST;

@Component
public class NodeTitleSorter implements NodeSorter {

    public static final String SORTER_NAME = "byTitle";
    public static final String RP_CASE_SENSITIVE = ":caseSensitive";

    public String getName(){
        return SORTER_NAME;
    }

    public String getLabel(){
        return "By Node Title";
    }

    @Override
    public Comparator<Node> createComparator(HttpServletRequest request) {
        boolean caseSensitive = Boolean.parseBoolean(request.getParameter(RP_CASE_SENSITIVE));
        boolean nonHierarchyFirst = request.getParameter(RP_NOT_HIERARCHY_FIRST) == null
                || Boolean.parseBoolean(request.getParameter(RP_NOT_HIERARCHY_FIRST));
        Comparator<Node> parentComparator = nonHierarchyFirst ? HierarchyNodeComparator.INSTANCE : (n1, n2) -> 0;
        return parentComparator.thenComparing((n1, n2) -> {
            try {
                String title1 = new JcrLabeledResource(n1).getTitle();
                if (title1 == null) {
                    title1 = n1.getName();
                }
                String title2 = new JcrLabeledResource(n2).getTitle();
                if (title2 == null) {
                    title2 = n2.getName();
                }
                return caseSensitive ? title1.compareTo(title2) :
                        title1.compareToIgnoreCase(title2);
            } catch (RepositoryException e) {
                return 0;
            }
        });
    }
}
