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
package com.adobe.acs.commons.sorter;

import com.day.cq.commons.JcrLabeledResource;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.apache.sling.servlets.post.PostOperation.PROP_OPERATION_NAME;

/**
 * The <code>SortNodesOperation</code> class implements the
 * {@link #OPERATION_SORT} operation for the Sling POST servlet.
 */
@Component(property = {
        PROP_OPERATION_NAME + "=" + SortNodesOperation.OPERATION_SORT
})
public class SortNodesOperation implements PostOperation {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Name of the sort operation
     */
    public static final String OPERATION_SORT = "sort";

    /**
     * Name of the request parameter indicating whether to sort nodes by jcr:title
     *
     * If this request parameter is missing then nodes will be sorted by node name.
     */
    public static final String RP_SORT_BY_TITLE = ":byTitle";

    /**
     * Name of the request parameter indicating whether the sort  should be case sensitive
     *
     * If this request parameter is missing then the sort will be case insensitive
     */
    public static final String RP_CASE_SENSITIVE = ":caseSensitive";

    @Override
    public void run(SlingHttpServletRequest slingRequest, PostResponse response, SlingPostProcessor[] processors) {
        try {
            long t0 = System.currentTimeMillis();
            boolean byTitle = Boolean.parseBoolean(slingRequest.getParameter(RP_SORT_BY_TITLE));
            boolean caseSensitive = Boolean.parseBoolean(slingRequest.getParameter(RP_CASE_SENSITIVE));

            Node targetNode = slingRequest.getResource().adaptTo(Node.class);
            if (targetNode == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND,"Missing target node to sort: " + slingRequest.getResource().getPath());
                return;
            }
            response.setLocation(targetNode.getPath());

            Comparator<Node> comparator = createComparator(byTitle, caseSensitive);
            List<Node> children = getSortedNodes(targetNode, comparator);

            Node prev = null;
            for (int i = 0; i < children.size(); i++) {
                Node n = children.get(children.size() - 1 - i);
                if (prev != null) {
                    log.trace("orderBefore: {}, {}", n.getName(), prev.getName());

                    targetNode.orderBefore(n.getName(), prev.getName());
                }
                response.onChange("ordered", n.getPath(), prev == null ? "" : prev.getName());
                prev = n;
            }
            targetNode.getSession().save();

            response.setTitle("Content sorted " + targetNode.getPath());

            log.info("all done, " + children.size() + " nodes sorted in " + (System.currentTimeMillis()-t0) + " ms");
        } catch (RepositoryException e) {
            response.setError(e);
        }
    }

    /**
     * Collect child nodes and sort them using the supplied Comparator
     *
     * @param   node   the node who's children need to be sorted
     * @param   comparator    the comparator to sort nodes
     * @return  list of sorted nodes
     * @throws RepositoryException  if something went wrong
     */
    List<Node> getSortedNodes(Node node, Comparator<Node> comparator) throws RepositoryException {
        List<Node> children = new ArrayList<>();
        NodeIterator it = node.getNodes();
        while (it.hasNext()) {
            Node child = it.nextNode();
            children.add(child);
        }

        children.sort(comparator);
        return children;
    }

    /**
     * Create a comparator to sort nodes
     *
     * @param byTitle   whether to sort by jcr:title. If <code>false</code> then sort by node name.
     * @param caseSensitive whether comparison should be case-sensitive
     * @return  comparator
     */
    static Comparator<Node> createComparator(boolean byTitle, boolean caseSensitive) {

        Comparator<Node> comparator = (n1, n2) -> {
            try {
                return Boolean.compare(
                        n1.isNodeType(JcrConstants.NT_HIERARCHYNODE),
                        n2.isNodeType(JcrConstants.NT_HIERARCHYNODE));
            } catch (RepositoryException e) {
                return 0;
            }
        };

        if (byTitle) {
            comparator = comparator.thenComparing((n1, n2) -> {
                try {
                    String title1 = new JcrLabeledResource(n1).getTitle();
                    if (title1 == null) title1 = n1.getName();
                    String title2 = new JcrLabeledResource(n2).getTitle();
                    if (title2 == null) title2 = n2.getName();
                    return caseSensitive ? title1.compareTo(title2) :
                            title1.compareToIgnoreCase(title2);
                } catch (RepositoryException e) {
                    return 0;
                }
            });
        } else {
            comparator = comparator.thenComparing((n1, n2) -> {
                try {
                    String name1 = n1.getName();
                    String name2 = n2.getName();
                    return caseSensitive ? name1.compareTo(name2) :
                            name1.compareToIgnoreCase(name2);
                } catch (RepositoryException e) {
                    return 0;
                }
            });
        }
        return comparator;
    }

}
