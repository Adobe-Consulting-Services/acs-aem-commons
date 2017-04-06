/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.functions.BiConsumer;
import com.adobe.acs.commons.wcm.impl.SimpleFilteringItemVisitor;
import com.day.cq.commons.jcr.JcrConstants;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import org.apache.sling.jcr.resource.JcrResourceConstants;

/**
 * Tree visitor which allows special cases such as how to handle child nodes
 * which will not be traversed further.
 *
 * In short, all nodes identified as container/folder items will be visited
 * recursively, and any child which is not a folder will be visited via a
 * special case method. This allows a cleaner delineation of leaf nodes vs
 * structure nodes.
 */
public class TreeFilteringItemVisitor extends SimpleFilteringItemVisitor {

    public static String[] TREE_TYPES = {
        JcrConstants.NT_FOLDER,
        JcrResourceConstants.NT_SLING_FOLDER,
        JcrResourceConstants.NT_SLING_ORDERED_FOLDER
    };

    public String[] treeTypes;

    /**
     * Create a standard visitor for commonly used folder structures.
     */
    public TreeFilteringItemVisitor() {
        this(TREE_TYPES);
    }

    /**
     * Create a visitor for a specific set of folder structures. This is useful
     * for including other things such as nt:unstructured or oak folder types.
     *
     * @param treeTypes List of all node types to consider as folder-level
     * containers.
     */
    public TreeFilteringItemVisitor(String... treeTypes) {
        this.treeTypes = treeTypes;
        searchForMatchingNodes(this::isFolder);
    }

    public boolean isFolder(Object obj) {
        if (obj instanceof Node) {
            try {
                Node n = (Node) obj;
                String type = n.getPrimaryNodeType().getName();
                for (String treeType : treeTypes) {
                    if (type.equalsIgnoreCase(treeType)) {
                        return true;
                    }
                }
            } catch (RepositoryException ex) {
                Logger.getLogger(TreeFilteringItemVisitor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    BiConsumer<Node, Integer> childVisitorHandler = null;

    public void onVisitChild(BiConsumer<Node, Integer> handler) {
        childVisitorHandler = handler;
    }

    public void visitChild(Node node, int level) throws RepositoryException {
        if (childVisitorHandler != null) {
            try {
                childVisitorHandler.accept(node, level);
            } catch (Exception ex) {
                throw new RepositoryException(ex);
            }
        }
    }

    ;

    /**
     * Called when the Visitor is passed to a Node. -- modified variant of what
     * is provided in the FilteringItemVisitor implementation.
     *
     *
     * It calls TraversingItemVisitor.entering(Node, int) followed by
     * TraversingItemVisitor.leaving(Node, int). Implement these abstract
     * methods to specify behavior on 'arrival at' and 'after leaving' the
     * Node.
     *
     * The final case is that child nodes, which are not to be traversed, are
     * managed via visitChild(Node)
     *
     *
     * If this method throws, the visiting process is aborted.
     *
     * @param node the Node that is accepting this visitor.
     * @throws RepositoryException if an error occurs
     */
    @Override
    public void visit(Node node) throws RepositoryException {
        try {
            if (traversalPredicate.evaluate(node)) {
                if (!breadthFirst) {
                    // depth-first traversal
                    entering(node, currentLevel);
                    if (maxLevel == -1 || currentLevel < maxLevel) {
                        currentLevel++;
                        if (this.walkProperties) {
                            PropertyIterator propIter = node.getProperties();
                            while (propIter.hasNext()) {
                                propIter.nextProperty().accept(this);
                            }
                        }
                        NodeIterator nodeIter = node.getNodes();
                        while (nodeIter.hasNext()) {
                            nodeIter.nextNode().accept(this);
                        }
                        currentLevel--;
                    }
                    leaving(node, currentLevel);
                } else {
                    // breadth-first traversal
                    entering(node, currentLevel);
                    leaving(node, currentLevel);

                    if (maxLevel == -1 || currentLevel < maxLevel) {
                        if (this.walkProperties) {
                            PropertyIterator propIter = node.getProperties();
                            while (propIter.hasNext()) {
                                nextQueue.addLast(propIter.nextProperty());
                            }
                        }
                        NodeIterator nodeIter = node.getNodes();
                        while (nodeIter.hasNext()) {
                            nextQueue.addLast(nodeIter.nextNode());
                        }
                    }

                    while (!currentQueue.isEmpty() || !nextQueue.isEmpty()) {
                        if (currentQueue.isEmpty()) {
                            currentLevel++;
                            currentQueue = nextQueue;
                            nextQueue = new LinkedList();
                        }
                        Item e = (Item) currentQueue.removeFirst();
                        e.accept(this);
                    }
                    currentLevel = 0;
                }
            } else {
                visitChild(node, currentLevel);
            }
        } catch (RepositoryException re) {
            currentLevel = 0;
            throw re;
        }
    }
}
