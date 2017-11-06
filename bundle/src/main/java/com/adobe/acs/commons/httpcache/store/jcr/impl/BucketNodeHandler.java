package com.adobe.acs.commons.httpcache.store.jcr.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.adobe.acs.commons.httpcache.keys.CacheKey;

/**
 * Wrapper class for the bucket node
 */
public class BucketNodeHandler
{
    private final Node bucketNode;

    public BucketNodeHandler(Node bucketNode){
        this.bucketNode = bucketNode;
    }

    public static Node getBucketNode(Node cacheRoot, CacheKey key, int cacheKeySplitDepth) throws RepositoryException
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
