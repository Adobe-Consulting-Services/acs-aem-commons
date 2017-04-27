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

import com.adobe.acs.commons.functions.CheckedBiConsumer;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.commons.predicate.Predicate;
import org.apache.jackrabbit.commons.visitor.FilteringItemVisitor;

public class SimpleFilteringItemVisitor extends FilteringItemVisitor {
    CheckedBiConsumer<Property, Integer> enterPropertyHandler = null;
    CheckedBiConsumer<Property, Integer> leavePropertyHandler = null;
    CheckedBiConsumer<Node, Integer> enterNodeHandler = null;
    CheckedBiConsumer<Node, Integer> leaveNodeHandler = null;
    Predicate nodePredicate = Predicate.TRUE;
    Predicate propertyPredicate = Predicate.FALSE;

    public SimpleFilteringItemVisitor() {
        setIncludePredicate(this::evaluatePredicates);
        setTraversalPredicate(this::evaluatePredicates);
    }
    
    protected boolean evaluatePredicates(Object o) {
        if (o instanceof Node) {
            return nodePredicate.evaluate(o);
        } else if (o instanceof Property) {
            return propertyPredicate.evaluate(o);
        }
        return true;
    }

    public void searchForMatchingNodes(Predicate filter) {
        nodePredicate = filter;
    }

    public void searchForMatchingProperties(Predicate filter) {
        propertyPredicate = filter;
    }
    
    public void onEnterProperty(CheckedBiConsumer<Property, Integer> handler) {
        enterPropertyHandler = handler;
    }

    public void onLeaveProperty(CheckedBiConsumer<Property, Integer> handler) {
        leavePropertyHandler = handler;
    }
    
    public void onEnterNode(CheckedBiConsumer<Node, Integer> handler) {
        enterNodeHandler = handler;
    }
        
    public void onLeaveNode(CheckedBiConsumer<Node, Integer> handler) {
        leaveNodeHandler = handler;
    }

        
    @Override
    protected void entering(Property prprt, int i) throws RepositoryException {
        if (enterPropertyHandler != null) {
            try {
                enterPropertyHandler.accept(prprt, i);
            } catch (Exception ex) {
                throw new RepositoryException(ex);
            }
        }
    }

    @Override
    protected void entering(Node node, int i) throws RepositoryException {
        if (enterNodeHandler != null) {
            try {
                enterNodeHandler.accept(node, i);
            } catch (Exception ex) {
                throw new RepositoryException(ex);
            }
        }
    }

    @Override
    protected void leaving(Property prprt, int i) throws RepositoryException {
        if (leavePropertyHandler != null) {
            try {
                leavePropertyHandler.accept(prprt, i);
            } catch (Exception ex) {
                throw new RepositoryException(ex);
            }
        }
    }

    @Override
    protected void leaving(Node node, int i) throws RepositoryException {
        if (leaveNodeHandler != null) {
            try {
                leaveNodeHandler.accept(node, i);
            } catch (Exception ex) {
                throw new RepositoryException(ex);
            }
        }
    }
}
