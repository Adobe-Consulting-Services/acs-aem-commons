package com.adobe.acs.commons.httpcache.store.jcr.impl.handler;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;
import com.adobe.acs.commons.util.DynamicObjectInputStream;

public class EntryNodeToCacheKeyHandler
{
    private final Node entryNode;
    private final DynamicClassLoaderManager dynamicClassLoaderManager;

    public EntryNodeToCacheKeyHandler(Node entryNode, DynamicClassLoaderManager dynamicClassLoaderManager){

        this.entryNode = entryNode;
        this.dynamicClassLoaderManager = dynamicClassLoaderManager;
    }

    public CacheKey get()
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final javax.jcr.Property cacheKeyProperty = entryNode.getProperty(JCRHttpCacheStoreConstants.PN_CACHEKEY);
        final InputStream inputStream = cacheKeyProperty.getBinary().getStream();

        final ClassLoader dynamicClassLoader = dynamicClassLoaderManager.getDynamicClassLoader();


        final DynamicObjectInputStream dynamicObjectInputStream = new DynamicObjectInputStream(inputStream, dynamicClassLoader);
        return (CacheKey) dynamicObjectInputStream.readObject();
    }
}
