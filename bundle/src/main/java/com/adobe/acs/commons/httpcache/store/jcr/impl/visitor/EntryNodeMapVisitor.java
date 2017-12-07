package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheContentHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheKeyHandler;

public class EntryNodeMapVisitor extends AbstractNodeVisitor
{
    final Map<CacheKey, CacheContent> cache = new HashMap<CacheKey, CacheContent>();
    private final DynamicClassLoaderManager dclm;
    private static final Logger log = LoggerFactory.getLogger(EntryNodeMapVisitor.class);


    public EntryNodeMapVisitor( int maxLevel, long deltaSaveThreshold, DynamicClassLoaderManager dclm) {
        super(maxLevel, deltaSaveThreshold);
        this.dclm = dclm;
    }

    public Map<CacheKey, CacheContent> getCache()
    {
        return cache;
    }

    protected void entering(final Node node, int level) throws RepositoryException
    {
        if(isCacheEntryNode(node))
        {
            CacheKey cacheKey;
            try {
                cacheKey = new EntryNodeToCacheKeyHandler(node, dclm).get();
                CacheContent content = new EntryNodeToCacheContentHandler(node).get();
                cache.put(cacheKey, content);
            } catch (Exception e) {
                log.error("Error in reading cache node!", e);
            }

        }
    }
}
