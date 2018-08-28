/*
 * Copyright 2018 Adobe.
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
package com.adobe.acs.commons.mcp.impl.processes.reorganizer;

import static com.adobe.acs.commons.mcp.impl.processes.reorganizer.Util.*;
import static com.adobe.acs.commons.util.visitors.SimpleFilteringResourceVisitor.toList;
import com.day.cq.wcm.commons.ReferenceSearch;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a node in the process of moving.
 */
public abstract class MovingNode {

    private String sourcePath;
    private String destinationPath;
    private String previousSibling;
    private MovingNode parent;
    private List<MovingNode> children = new ArrayList<>();
    private final List<String> allReferences = new ArrayList<>();
    private final List<String> publishedReferences = new ArrayList<>();
    private boolean destinationAlreadyExists;
    private boolean sourceActivated;

    public abstract boolean isCopiedBeforeMove();

    public abstract boolean isSupposedToBeReferenced();

    public abstract boolean isAbleToHaveChildren();

    public void addChild(MovingNode child) {
        children.add(child);
        child.setParent(this);
    }

    public boolean isLeaf() {
        return getChildren().isEmpty();
    }

    public boolean isSourceActivated() {
        return sourceActivated;
    }

    /**
     * @return the sourcePath
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * @param sourcePath the sourcePath to set
     */
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    /**
     * @return the destinationPath
     */
    public String getDestinationPath() {
        return destinationPath;
    }

    /**
     * @param destinationPath the destinationPath to set
     */
    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    /**
     * @return the parent
     */
    public MovingNode getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(MovingNode parent) {
        this.parent = parent;
        setDestinationPath(sourcePath.replaceFirst(Pattern.quote(parent.getSourcePath()), parent.getDestinationPath()));
    }

    /**
     * @return the children
     */
    public List<MovingNode> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<MovingNode> children) {
        this.children = children;
    }

    /**
     * @return the references
     */
    public List<String> getAllReferences() {
        return allReferences;
    }

    /**
     * @return the references
     */
    public List<String> getPublishedReferences() {
        return publishedReferences;
    }

    /**
     * @return the destinationAlreadyExists
     */
    public boolean isDestinationAlreadyExists() {
        return destinationAlreadyExists;
    }

    /**
     * @param destinationAlreadyExists the destinationAlreadyExists to set
     */
    public void setDestinationAlreadyExists(boolean destinationAlreadyExists) {
        this.destinationAlreadyExists = destinationAlreadyExists;
    }

    public String getPreviousSibling() {
        return previousSibling;
    }

    public abstract void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws IllegalAccessException, Exception;

    public void findReferences(ResourceResolver rr, String referenceSearchRoot, int maxReferences) throws IllegalAccessException {
        ReferenceSearch refSearch = new ReferenceSearch();
        refSearch.setExact(true);
        refSearch.setHollow(true);
        refSearch.setMaxReferencesPerPage(maxReferences);
        refSearch.setSearchRoot(referenceSearchRoot);
        refSearch.search(rr, sourcePath).values().stream()
                .peek(p -> allReferences.add(p.getPagePath()))
                .filter(p -> isActivated(rr, p.getPagePath()))
                .map(ReferenceSearch.Info::getPagePath)
                .collect(Collectors.toCollection(() -> publishedReferences));
    }

    /**
     * Depth-first visitor, provide consumer function every node in the tree
     *
     * @param consumer Consumer which accepts nodes
     */
    public void visit(Consumer<MovingNode> consumer) {
        visit(consumer, null, null);
    }

    /**
     * Depth-first visitor
     *
     * @param consumer Consumer for traversed nodes
     * @param leafConsumer Consumer for leaf nodes (first level after
     * traversalFilter returns false), if no traversal function this is never
     * called
     * @param traversalFilter Function which determines if the tree should be
     * evaluated any deeper, Null means always true
     */
    public void visit(Consumer<MovingNode> consumer, Consumer<MovingNode> leafConsumer, Function<MovingNode, Boolean> traversalFilter) {
        LinkedList<MovingNode> stack = new LinkedList<>();
        stack.clear();
        stack.add(this);

        while (!stack.isEmpty()) {
            MovingNode node = stack.poll();
            consumer.accept(node);
            if (traversalFilter == null || traversalFilter.apply(node)) {
                stack.addAll(toList(node.getChildren()));
            } else if (leafConsumer != null) {
                leafConsumer.accept(node);
            }
        }
    }

    public MovingNode findByPath(String path) {
        String[] parts = path.replaceFirst(getSourcePath() + "/", "").split("/");
        MovingNode current = this;
        for (String part : parts) {
            String childPath = current.getSourcePath() + "/" + part;
            boolean found = false;
            for (MovingNode child : getChildren()) {
                if (child.getSourcePath().equals(childPath)) {
                    current = child;
                    found = true;
                }
            }
            if (!found) {
                return null;
            }
        }
        return current;
    }
}
