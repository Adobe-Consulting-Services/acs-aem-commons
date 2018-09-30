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
package com.adobe.acs.commons.util.visitors;

import com.adobe.acs.commons.functions.CheckedBiConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleFilteringResourceVisitorTest {
    MockResourceResolver rr = new MockResourceResolver();
    MockResource root = new MockResource(rr, "/", "sling:folder");
    Resource node1a = new MockResource(rr, "/1a", "sling:folder");
    Resource node1b = new MockResource(rr, "/1b", "sling:folder");
    Resource node1a1 = new MockResource(rr, "/1a/1", "sling:folder");
    Resource node1a2 = new MockResource(rr, "/1a/2", "sling:folder");
    Resource node1b1 = new MockResource(rr, "/1b/1", "sling:folder");
    Resource node1b2 = new MockResource(rr, "/1b/2", "sling:folder");
    
    Resource[] breadthFirstList = new Resource[]{
        root, node1a, node1b, node1a1, node1a2, node1b1, node1b2
    };

    Resource[] depthFirstList = new Resource[]{
        root, node1a, node1a1, node1a2, node1b, node1b1, node1b2
    };
   
    ValueMap properties = new ValueMapDecorator(new HashMap<>());
    
    @Before
    public void setUp() throws Exception {
        rr.addChildren(root, Arrays.asList(node1a, node1b));
        rr.addChildren(node1a, Arrays.asList(node1a1, node1a2));
        rr.addChildren(node1b, Arrays.asList(node1b1, node1b2));
    }
    
    @Test
    public void accept_breadthFirst() throws Exception {
        SimpleFilteringResourceVisitor visitor = new SimpleFilteringResourceVisitor();
        ArrayList<Resource> visitOrder = new ArrayList<>();
        CheckedBiConsumer<Resource, Integer> tracker = (res, lvl) -> visitOrder.add(res);
        
        visitor.setBreadthFirstMode();
        visitor.setResourceVisitorChecked(tracker);
        visitor.accept(root);
        assertArrayEquals(breadthFirstList, visitOrder.toArray());
    }
    
    @Test
    public void accept_depthFirst() throws Exception {
        SimpleFilteringResourceVisitor visitor = new SimpleFilteringResourceVisitor();
        ArrayList<Resource> visitOrder = new ArrayList<>();
        CheckedBiConsumer<Resource, Integer> tracker = (res, lvl) -> visitOrder.add(res);
        
        visitor.setDepthFirstMode();
        visitor.setResourceVisitorChecked(tracker);
        visitor.accept(root);
        assertArrayEquals(depthFirstList, visitOrder.toArray());
    }
    
    @Test
    public void accept_leafTest_breadthFirst() throws Exception {
        SimpleFilteringResourceVisitor visitor = new SimpleFilteringResourceVisitor();
        visitor.setBreadthFirstMode();
        visitor.setTraversalFilter(res->res.getPath().equals("/"));
        ArrayList<Resource> nodes = new ArrayList<>();
        ArrayList<Resource> leaves = new ArrayList<>();
        
        visitor.setResourceVisitor((res,lvl)->nodes.add(res));
        visitor.setLeafVisitor((res,lvl)->leaves.add(res));
        visitor.accept(root);
        
        assertArrayEquals(new Resource[] {root}, nodes.toArray());        
        assertArrayEquals(new Resource[] {node1a, node1b}, leaves.toArray());        
    }    

    @Test
    public void accept_leafTest_depthFirst() throws Exception {
        SimpleFilteringResourceVisitor visitor = new SimpleFilteringResourceVisitor();
        visitor.setDepthFirstMode();
        visitor.setTraversalFilter(res->res.getPath().equals("/"));
        ArrayList<Resource> nodes = new ArrayList<>();
        ArrayList<Resource> leaves = new ArrayList<>();
        
        visitor.setResourceVisitor((res,lvl)->nodes.add(res));
        visitor.setLeafVisitor((res,lvl)->leaves.add(res));
        visitor.accept(root);
        
        assertArrayEquals(new Resource[] {root}, nodes.toArray());        
        assertArrayEquals(new Resource[] {node1a, node1b}, leaves.toArray());        
    }
}
