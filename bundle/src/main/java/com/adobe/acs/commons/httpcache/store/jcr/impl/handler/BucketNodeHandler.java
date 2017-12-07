package com.adobe.acs.commons.httpcache.store.jcr.impl.handler;

import static org.apache.jackrabbit.commons.JcrUtils.getOrCreateUniqueByPath;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

public class BucketNodeHandler
{

    private final Node bucketNode;
    private final DynamicClassLoaderManager dynamicClassLoaderManager;

    public BucketNodeHandler(Node node, DynamicClassLoaderManager dynamicClassLoaderManager){
        this.bucketNode = node;
        this.dynamicClassLoaderManager = dynamicClassLoaderManager;
    }

    public Node createOrRetrieveEntryNode(CacheKey key)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final Node existingEntryNode = getEntryIfExists(key);

        if(null != existingEntryNode)
            return existingEntryNode;
        else
            return getOrCreateUniqueByPath(bucketNode, JCRHttpCacheStoreConstants.PATH_ENTRY, JCRHttpCacheStoreConstants.OAK_UNSTRUCTURED);
    }

    public Node getEntryIfExists(CacheKey key)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final NodeIterator entryNodeIterator  = bucketNode.getNodes();

        while(entryNodeIterator.hasNext()){
            Node entryNode = entryNodeIterator.nextNode();
            CacheKey entryKey = new EntryNodeToCacheKeyHandler(entryNode, dynamicClassLoaderManager).get();
            if(key.equals(entryKey))
                return entryNode;
        }

        return null;
    }
}
