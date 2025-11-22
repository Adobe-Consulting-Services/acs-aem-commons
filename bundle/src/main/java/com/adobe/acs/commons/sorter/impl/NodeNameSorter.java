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
package com.adobe.acs.commons.sorter.impl;

import com.adobe.acs.commons.sorter.NodeSorter;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.Comparator;

import static com.adobe.acs.commons.sorter.impl.HierarchyNodeComparator.RP_NOT_HIERARCHY_FIRST;
import static org.apache.commons.lang3.StringUtils.isNumeric;

@Component(service = NodeSorter.class)
public class NodeNameSorter implements NodeSorter {
    public static final String SORTER_NAME = "byName";
    public static final String RP_CASE_SENSITIVE = ":caseSensitive";
    public static final String RP_RESPECT_NUMBERS = ":respectNumbers";

    public String getName() {
        return SORTER_NAME;
    }

    public String getLabel() {
        return "By Node Name";
    }


    private Comparator<Node> numbersFirstComparator = (n1, n2) -> {
        try {
            String name1 = n1.getName();
            String name2 = n2.getName();
            return Boolean.compare(
                    isNumeric(name2),
                    isNumeric(name1)
            );
        } catch (RepositoryException e) {
            return 0;
        }
    };

    @Override
    public Comparator<Node> createComparator(HttpServletRequest request) {
        boolean caseSensitive = Boolean.parseBoolean(request.getParameter(RP_CASE_SENSITIVE));
        boolean respectNumbers = Boolean.parseBoolean(request.getParameter(RP_RESPECT_NUMBERS));
        boolean nonHierarchyFirst = request.getParameter(RP_NOT_HIERARCHY_FIRST) == null
                || Boolean.parseBoolean(request.getParameter(RP_NOT_HIERARCHY_FIRST));
        Comparator<Node> comparator = (n1, n2) -> 0;
        if (nonHierarchyFirst) {
            comparator = comparator.thenComparing(HierarchyNodeComparator.INSTANCE);
        }
        if (respectNumbers) {
            comparator = comparator.thenComparing(numbersFirstComparator);
        }

        comparator = comparator.thenComparing((n1, n2) -> {
            try {
                String name1 = n1.getName();
                String name2 = n2.getName();
                if (respectNumbers && isNumeric(name1) && isNumeric(name2)) {
                    Integer number1 = Integer.valueOf(name1);
                    Integer number2 = Integer.valueOf(name2);
                    return number1.compareTo(number2);
                } else {
                    return caseSensitive ? name1.compareTo(name2) :
                            name1.compareToIgnoreCase(name2);
                }

            } catch (RepositoryException e) {
                return 0;
            }
        });

        return comparator;
    }

}
