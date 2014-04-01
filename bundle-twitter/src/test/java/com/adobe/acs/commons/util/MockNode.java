/*
 * #%L
 * ACS AEM Commons Twitter Support Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.util;

import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.sling.commons.testing.jcr.MockNodeIterator;

public class MockNode extends org.apache.sling.commons.testing.jcr.MockNode {
    private HashMap<String, Node> nodes = new HashMap<String, Node>();

    private Node[] nodeArray;

    public MockNode(String path) {
        super(path);
    }

    public MockNode(String path, String type) {
        super(path, type);
    }

    public MockNode(Node[] nodes) {
        super(null);
        nodeArray = nodes;
    }

    public Node addNode(String relPath) {
        Node node = new MockNode(relPath);

        nodes.put(relPath, node);

        return node;
    }

    public Node addNode(String relPath, String primaryNodeTypeName) {
        Node node = new MockNode(relPath, primaryNodeTypeName);

        nodes.put(relPath, node);

        return node;
    }

    public Node getNode(String relPath) {
        return nodes.get(relPath);
    }

    public NodeIterator getNodes() {
        return new MockNodeIterator(nodeArray);
    }
}
