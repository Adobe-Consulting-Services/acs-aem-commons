package com.adobe.acs.commons.httpcache.store.jcr.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.adobe.acs.commons.httpcache.keys.CacheKey;

/**
 * Wrapper class for the bucket node
 */
public class BucketNodeFactory
{
    private final Session session;
    private final CacheKey key;
    private final int cacheKeySplitDepth;
    private final Node cacheRoot;

    public BucketNodeFactory(Session session, String cacheRootPath, CacheKey key, int cacheKeySplitDepth)
            throws RepositoryException
    {
        this.session = session;
        this.key = key;
        this.cacheKeySplitDepth = cacheKeySplitDepth;
        this.cacheRoot = session.getNode(cacheRootPath);
    }

    public Node getBucketNode() throws RepositoryException
    {
        final Node bucketNode;
        final String hashCodeString = String.valueOf(key.hashCode());

        if(!cacheRoot.hasNode(String.valueOf(hashCodeString)))
            bucketNode = cacheRoot.addNode(hashCodeString);
        else
            bucketNode = cacheRoot.getNode(hashCodeString);
        return bucketNode;
    }


}
