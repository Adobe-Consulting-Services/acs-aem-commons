package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheKeyHandler;

import java.io.IOException;

public class InvalidateByCacheConfigVisitor extends AbstractNodeVisitor
{
    private static final Logger log = LoggerFactory.getLogger(InvalidateByCacheConfigVisitor.class);

    private final HttpCacheConfig cacheConfig;
    private final DynamicClassLoaderManager dclm;



    public InvalidateByCacheConfigVisitor(
            int maxLevel,
            long deltaSaveThreshold,
            final HttpCacheConfig cacheConfig,
            final DynamicClassLoaderManager dclm
    ){
        super(maxLevel, deltaSaveThreshold);
        this.cacheConfig = cacheConfig;
        this.dclm = dclm;
    }

    protected void leaving(final Node node, int level) throws RepositoryException
    {
        if(isCacheEntryNode(node)) {
            //check and remove nodes that are expired.
            try {
                final CacheKey key = getCacheKey(node);
                if(cacheConfig.knows(key)) {
                    node.remove();
                    persistSession();
                }
            } catch (Exception e) {
                log.error("Exception occured in retrieving the CacheKey from the entry node", e);
                throw new RepositoryException(e);
            }
        }else if(isEmptyBucketNode(node)) {
            //cleanup empty bucket nodes.
            node.remove();
            persistSession();
        }
        super.leaving(node, level);
    }

    private CacheKey getCacheKey(final Node node) throws RepositoryException, IOException, ClassNotFoundException {
        return new EntryNodeToCacheKeyHandler(node, dclm).get();
    }
}
