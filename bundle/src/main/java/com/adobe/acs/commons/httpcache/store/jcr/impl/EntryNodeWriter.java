package com.adobe.acs.commons.httpcache.store.jcr.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.day.cq.commons.jcr.JcrConstants;

public class EntryNodeWriter
{

    private final Session session;
    private final Node entryNode;
    private final CacheContent cacheContent;

    public EntryNodeWriter(Session session, Node entryNode, CacheContent cacheContent){
        this.session = session;
        this.entryNode = entryNode;
        this.cacheContent = cacheContent;
    }

    /**
     * Populate the entry node with values
     * @throws RepositoryException
     */
    public void write() throws RepositoryException
    {
        populateMetaData();
        populateHeaders();
        populateBinaryContent();

        //write the node
        session.save();

    }

    private void populateMetaData() throws RepositoryException
    {
        entryNode.setProperty("status", cacheContent.getStatus());
        entryNode.setProperty("char-encoding", cacheContent.getCharEncoding());
        entryNode.setProperty("content-type", cacheContent.getContentType());
    }

    /**
     * Save the inputstream to a binary property under the cache entry node.
     * @throws RepositoryException
     */
    private void populateBinaryContent() throws RepositoryException
    {
        final Node contents = entryNode.addNode("contents");
        contents.setPrimaryType(JcrConstants.NT_UNSTRUCTURED);

        //save input stream to node
        Binary binary = session.getValueFactory().createBinary(cacheContent.getInputDataStream());
        contents.setProperty("binary", binary);
    }

    /**
     * Save the headers into a headers node under the cache entry node.
     * @throws RepositoryException
     */
    private void populateHeaders() throws RepositoryException
    {
        Node headers = entryNode.addNode("headers");
        headers.setPrimaryType(JcrConstants.NT_UNSTRUCTURED);


        for(Iterator<Map.Entry<String, List<String>>> entryIterator = cacheContent.getHeaders().entrySet().iterator(); entryIterator.hasNext();){

            Map.Entry<String, List<String>> entry = entryIterator.next();
            final String key = entry.getKey();
            final List<String> values = entry.getValue();
            headers.setProperty(key, values.toArray(new String[values.size()]));
        }
    }
}
