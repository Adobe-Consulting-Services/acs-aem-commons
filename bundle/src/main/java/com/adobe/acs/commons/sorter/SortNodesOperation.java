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

import com.adobe.acs.commons.sorter.impl.NodeNameSorter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.sling.servlets.post.PostOperation.PROP_OPERATION_NAME;

/**
 * The <code>SortNodesOperation</code> class implements the {@link #OPERATION_SORT} operation for the Sling POST servlet.
 * <p>
 * Use this operation to alphabetize JCR nodes. For example, the following command line sorts the children of the /content/sample page:
 * <pre>
 *     curl -u admin:admin -F":operation=acs-commons:sortNodes" http://localhost:4502/content/sample
 * </pre>
 * </p>
 *
 * You can use optional {@link NodeNameSorter#RP_CASE_SENSITIVE} and {@link #RP_SORTER_NAME} parameters to control whether to sort by
 * by node name (default) or by jcr:title and whether the sort should be case-sensitive:
 * <pre>
 *     curl -u admin:admin -F":operation=acs-commons:sortNodes" \
 *     -F":sorterName:byTitle" -F":caseSensitive:true" \
 *     http://localhost:4502/content/someFolder
 * </pre>
  */
@Component(property = {
        PROP_OPERATION_NAME + "=" + SortNodesOperation.OPERATION_SORT
})
public class SortNodesOperation implements PostOperation {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Name of the sort operation.
     * The acs-commons: prefix is to avoid name clash with other PostOperations .
     */
    public static final String OPERATION_SORT = "acs-commons:sortNodes";

    /**
     * Name of the request parameter indicating whether to sort nodes by jcr:title
     *
     * If this request parameter is missing then nodes will be sorted by node name.
     */
    public static final String RP_SORTER_NAME = ":sorterName";

    public static final String DEFAULT_SORTER_NAME = NodeNameSorter.SORTER_NAME;


    private final Map<String, NodeSorter> nodeSorters = Collections.synchronizedMap(new LinkedHashMap<>());

    @Reference(service = NodeSorter.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindNodeSorter(NodeSorter sorter, Map<String, Object> properties){
        if (sorter != null ) {
            String sorterName = sorter.getName();
            log.debug("registering node sorter: {} -> {}", sorterName, sorter.getClass().getName());
            nodeSorters.put(sorterName, sorter);
        }
    }

    protected void unbindNodeSorter(NodeSorter sorter, Map<String, Object> properties){
        String sorterName = sorter.getName();
        log.debug("un-registering node sorter: {} -> {}", sorterName, sorter.getClass().getName());
        nodeSorters.remove(sorterName);
    }

    @Override
    public void run(SlingHttpServletRequest slingRequest, PostResponse response, SlingPostProcessor[] processors) {
        try {
            Node targetNode = slingRequest.getResource().adaptTo(Node.class);
            if (targetNode == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND,"Missing target node to sort: " + slingRequest.getResource().getPath());
                return;
            }
            response.setPath(targetNode.getPath());
            response.setLocation(targetNode.getPath());
            if(targetNode.getParent() != null) {
                response.setParentLocation(targetNode.getParent().getPath());
            }

            final long t0 = System.currentTimeMillis();
            Comparator<Node> comparator = getNodeSorter(slingRequest);
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
            response.setTitle("Content sorted " + response.getPath());

            log.info("{} nodes sorted in {} ms", children.size(), System.currentTimeMillis()-t0 );
        } catch (RepositoryException | IllegalArgumentException e) {
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

    Comparator<Node> getNodeSorter(SlingHttpServletRequest slingRequest){
        String sorterId = slingRequest.getParameter(RP_SORTER_NAME);
        if(sorterId == null){
            sorterId = DEFAULT_SORTER_NAME;
        }
        NodeSorter sorter = nodeSorters.get(sorterId);
        if(sorter == null){
            String msg = "NodeSorter was not found: " + sorterId
                    + ". Available sorters are: " + nodeSorters.keySet().toString();
            throw new IllegalArgumentException(msg);
        }
        return sorter.createComparator(slingRequest);
    }
}
