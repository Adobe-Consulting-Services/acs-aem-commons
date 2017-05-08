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
package com.adobe.acs.commons.util.visitors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.apache.sling.api.resource.Resource;

public class SimpleFilteringResourceVisitor {

    public static enum TraversalMode {
        DEPTH, BREADTH
    };
    TraversalMode mode = TraversalMode.BREADTH;
    BiConsumer<Map.Entry<String, Object>, Integer> propertyVisitor = null;
    BiConsumer<Resource, Integer> resourceVisitor = null;
    BiConsumer<Resource, Integer> leafVisitor = null;
    Queue<Resource> currentLevel = new LinkedList<>();
    Queue<Resource> nextLevel = new LinkedList<>();
    Function<String, Boolean> propertyFilter = s -> true;
    Function<Resource, Boolean> traversalFilter = r -> true;

    public SimpleFilteringResourceVisitor() {
    }

    public void setPropertyFilter(Function<String, Boolean> filter) {
        propertyFilter = filter;
    }

    public final void setTraversalFilter(Function<Resource, Boolean> filter) {
        traversalFilter = filter;
    }

    public final void setResourceVisitor(BiConsumer<Resource, Integer> handler) {
        resourceVisitor = handler;
    }

    public final void setLeafVisitor(BiConsumer<Resource, Integer> handler) {
        leafVisitor = handler;
    }

    public final void setPropertyVisitor(BiConsumer<Map.Entry<String, Object>, Integer> handler) {
        propertyVisitor = handler;
    }

    public final void setBreadthFirstMode() {
        mode = TraversalMode.BREADTH;
    }

    public final void setDepthFirstMode() {
        mode = TraversalMode.DEPTH;
    }

    public void accept(final Resource res) {
        currentLevel.clear();
        nextLevel.clear();
        accept(res, 0);
    }

    protected void accept(final Resource res, int level) {
        if (res != null) {
            if (propertyVisitor != null) {
                res.getValueMap().entrySet().stream()
                        .filter(e -> propertyFilter.apply(e.getKey()))
                        .forEach(entry -> propertyVisitor.accept(entry, level));
            }
            if (mode == TraversalMode.DEPTH) {
                if (traversalFilter.apply(res)) {
                    this.traverseChildren(res.listChildren(), level);
                    if (resourceVisitor != null) {
                        resourceVisitor.accept(res, level);
                    }
                } else {
                    if (leafVisitor != null) {
                        leafVisitor.accept(res, level);
                    }
                }
            } else {
                if (traversalFilter.apply(res)) {
                    if (resourceVisitor != null) {
                        resourceVisitor.accept(res, level);
                    }
                    this.traverseChildren(res.listChildren(), level);
                } else {
                    if (leafVisitor != null) {
                        leafVisitor.accept(res, level);
                    }
                    this.traverseChildren(Iterators.empty(), level);
                }
            }

        }
    }

    protected void traverseChildren(final Iterator<Resource> children, int level) {
        if (mode == TraversalMode.DEPTH) {
            while (children.hasNext()) {
                final Resource child = children.next();
                accept(child, level+1);
            }
        } else {
            children.forEachRemaining(nextLevel::add);
            if (currentLevel.isEmpty()) {
                currentLevel.addAll(nextLevel);
                nextLevel.clear();
                level++;
            }
            Resource res = currentLevel.poll();
            if (res != null) {
                accept(res, level);
            }
        }
    }
}
