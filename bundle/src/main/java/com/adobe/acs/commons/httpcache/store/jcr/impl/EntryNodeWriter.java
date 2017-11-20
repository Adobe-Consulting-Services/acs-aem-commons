package com.adobe.acs.commons.httpcache.store.jcr.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.JcrUtils;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.day.cq.commons.jcr.JcrConstants;

public class EntryNodeWriter
{

    private final Session session;
    private final Node entryNode;
    private final CacheKey cacheKey;
    private final CacheContent cacheContent;
    private final int expireTimeInSeconds;

    public EntryNodeWriter(Session session, Node entryNode, CacheKey cacheKey, CacheContent cacheContent,
            int expireTimeInSeconds){
        this.session = session;
        this.entryNode = entryNode;
        this.cacheKey = cacheKey;
        this.cacheContent = cacheContent;
        this.expireTimeInSeconds = expireTimeInSeconds;
    }

    /**
     * Populate the entry node with values
     * @throws RepositoryException
     */
    public void write() throws RepositoryException, IOException
    {
        entryNode.setProperty("isCacheEntryNode", true);

        populateMetaData();
        populateHeaders();
        populateBinaryContent();
        setExpireTime();

        if(!entryNode.hasProperty("cacheKeySerialized"))
            populateCacheKey();
    }

    private void setExpireTime() throws RepositoryException
    {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, expireTimeInSeconds);
        entryNode.setProperty("expiresOn",  calendar.getTimeInMillis() );
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
        final Node contents = JcrUtils.getOrCreateByPath(entryNode, "contents", false, JcrConstants.NT_FILE, JcrConstants.NT_FILE, false);
        final Node jcrContent = JcrUtils.getOrCreateByPath(contents, JcrConstants.JCR_CONTENT, false, JcrConstants.NT_RESOURCE, JcrConstants.NT_RESOURCE, false);
        //save input stream to node
        final Binary binary = session.getValueFactory().createBinary(cacheContent.getInputDataStream());
        jcrContent.setProperty(JcrConstants.JCR_DATA, binary);
        jcrContent.setProperty(JcrConstants.JCR_MIMETYPE, "text/plain");
    }

    /**
     * Save the headers into a headers node under the cache entry node.
     * @throws RepositoryException
     */
    private void populateHeaders() throws RepositoryException
    {
        final Node headers = JcrUtils.getOrCreateByPath(entryNode, "headers", false, JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, false);

        for(Iterator<Map.Entry<String, List<String>>> entryIterator = cacheContent.getHeaders().entrySet().iterator(); entryIterator.hasNext();){
            Map.Entry<String, List<String>> entry = entryIterator.next();
            final String key = entry.getKey();
            final List<String> values = entry.getValue();
            headers.setProperty(key, values.toArray(new String[values.size()]));
        }
    }

    private void populateCacheKey() throws RepositoryException, IOException
    {
        ByteArrayInputStream inputStream = null;

        try{
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(cacheKey);
            objectOutputStream.close();
            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            final Binary binary = session.getValueFactory().createBinary(inputStream);

            entryNode.setProperty("cacheKeySerialized", binary);
        }finally {
            if(inputStream != null)
                inputStream.close();
        }
    }
}
