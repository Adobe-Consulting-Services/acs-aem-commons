/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.renovator;

import com.day.cq.audit.AuditLog;
import com.day.cq.audit.AuditLogEntry;
import com.day.cq.wcm.api.PageEvent;
import com.day.cq.wcm.api.PageModification;
import com.day.cq.wcm.commons.ReferenceSearch;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.mcp.impl.processes.renovator.Util.isActivated;
import static com.adobe.acs.commons.util.visitors.SimpleFilteringResourceVisitor.toList;

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

    protected abstract boolean isAuditableMove();

    public void addChild(MovingNode child) {
        if (child != this) {
            children.add(child);
            child.setParent(this);
        }
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
        if (parent != null && parent != this) {
            setDestinationPath(sourcePath.replaceFirst(Pattern.quote(parent.getSourcePath()), parent.getDestinationPath()));
        }
    }

    /**
     * @return the children
     */
    public List<MovingNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<MovingNode> children) {
        this.children = Optional.ofNullable(children)
                .map(list -> (List<MovingNode>) new ArrayList<>(list))
                .orElse(Collections.emptyList());
    }

    /**
     * @return the references
     */
    public List<String> getAllReferences() {
        return Collections.unmodifiableList(allReferences);
    }

    /**
     * @return the references
     */
    public List<String> getPublishedReferences() {
        return Collections.unmodifiableList(publishedReferences);
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

    public abstract void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws IllegalAccessException, MovingException;

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
        visit(consumer, consumer, null);
    }

    /**
     * Depth-first visitor
     *
     * @param consumer        Consumer for traversed nodes
     * @param leafConsumer    Consumer for leaf nodes (first level after
     *                        traversalFilter returns false), if no traversal function this is never
     *                        called
     * @param traversalFilter Function which determines if the tree should be
     *                        evaluated any deeper, Null means always true
     */
    public void visit(Consumer<MovingNode> consumer, Consumer<MovingNode> leafConsumer, Function<MovingNode, Boolean> traversalFilter) {
        LinkedList<MovingNode> stack = new LinkedList<>();
        stack.clear();
        stack.add(this);

        while (!stack.isEmpty()) {
            MovingNode node = stack.poll();
            if (traversalFilter == null || traversalFilter.apply(node)) {
                stack.addAll(toList(node.getChildren()));
                consumer.accept(node);
            } else if (leafConsumer != null) {
                leafConsumer.accept(node);
            }
        }
    }

    public Optional<MovingNode> findByPath(String path) {
        if (path.equals(getSourcePath())) {
            return Optional.of(this);
        }
        String[] parts = path.replaceFirst(getSourcePath() + "/", "").split("/");
        MovingNode current = this;
        for (String part : parts) {
            String childPath = current.getSourcePath() + "/" + part;
            boolean found = false;
            for (MovingNode child : current.getChildren()) {
                if (child.getSourcePath().equals(childPath)) {
                    current = child;
                    found = true;
                }
            }
            if (!found) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    public boolean hasChild(String path) {
        return getChildren().stream().anyMatch(n -> n.getSourcePath().equals(path));
    }

    protected Map<String, Object> getClonedProperties(Resource source) {
        HashMap<String, Object> props = new HashMap<>(source.getValueMap());
        props.remove("jcr:versionHistory");
        props.remove("jcr:uuid");
        props.remove("jcr:baseVersion");
        props.remove("jcr:predecessors");
        props.remove("jcr:isCheckedOut");
        return props;
    }

    public void addAuditRecordForMove(ResourceResolver rr, AuditLog auditLog) {
        if(isAuditableMove()) {
            Map<String, Object> props = new HashMap<>();

            props.put("path", getSourcePath());
            props.put("destination", getDestinationPath());
            props.put("type", PageModification.ModificationType.MOVED.toString());

            AuditLogEntry moveAuditEntry = new AuditLogEntry(PageEvent.EVENT_TOPIC,
                    Calendar.getInstance().getTime(),
                    rr.getUserID() != null ? rr.getUserID() : "renovator",
                    getSourcePath(),
                    PageModification.ModificationType.MOVED.toString(),
                    props);
            auditLog.add(moveAuditEntry);
        }
    }
}