package com.adobe.acs.commons.httpcache.store.jcr.impl.writer;

import static com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants.OAK_UNSTRUCTURED;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.exceptions.BucketNodeFactoryException;

/**
 * Wrapper class for the bucket node
 */
public class BucketNodeFactory
{
    public static final double HASHCODE_LENGTH = 10d;

    private final CacheKey key;
    private final int cacheKeySplitDepth;
    private final Node cacheRoot;

    public BucketNodeFactory(Session session, String cacheRootPath, CacheKey key, int cacheKeySplitDepth)
            throws RepositoryException, BucketNodeFactoryException
    {
        this.key = key;
        this.cacheKeySplitDepth = cacheKeySplitDepth;

        if(!session.nodeExists(cacheRootPath))
            throw new BucketNodeFactoryException("Cache root path " + cacheRootPath + " not found!");
        this.cacheRoot = session.getNode(cacheRootPath);
    }

    public Node getBucketNode() throws RepositoryException
    {
        final String[] pathArray = getPathArray();

        Node targetNode = cacheRoot;

        for(String path : pathArray){
            Node childNode = JcrUtils.getOrCreateByPath(targetNode, path,false,OAK_UNSTRUCTURED, OAK_UNSTRUCTURED,false);
            targetNode = childNode;
        }

        return targetNode;
    }

    private String[] getPathArray(){
        final String hashCodeString = StringUtils.leftPad(String.valueOf(key.hashCode()), (int)HASHCODE_LENGTH, "0");

        int increment = (int) Math.ceil(HASHCODE_LENGTH / cacheKeySplitDepth);
        final String[] pathArray = new String[cacheKeySplitDepth];

        for(int position = 0, i = 0; i < (cacheKeySplitDepth); position += increment, i++){
            int endIndex = (position + increment > hashCodeString.length()) ? hashCodeString.length() : position + increment;
            String nodeName =  StringUtils.leftPad(hashCodeString.substring(position, endIndex), 5, "0");
            pathArray[i] = nodeName;
        }
        return pathArray;
    }


}
