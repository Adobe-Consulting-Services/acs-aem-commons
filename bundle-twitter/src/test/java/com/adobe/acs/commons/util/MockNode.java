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
