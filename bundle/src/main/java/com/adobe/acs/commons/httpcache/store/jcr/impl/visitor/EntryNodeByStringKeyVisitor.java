package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheContentHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheKeyHandler;

public class EntryNodeByStringKeyVisitor extends AbstractNodeVisitor
{
    private static final Logger log = LoggerFactory.getLogger(EntryNodeByStringKeyVisitor.class);

    private final DynamicClassLoaderManager dclm;
    private final String cacheKeyStr;
    private CacheContent cacheContent;

    public EntryNodeByStringKeyVisitor(int maxLevel, DynamicClassLoaderManager dclm, String cacheKeyStr) {
        super( maxLevel, -1);
        this.dclm = dclm;
        this.cacheKeyStr = cacheKeyStr;
    }

    public CacheContent getCacheContentIfPresent()
    {
        return cacheContent;
    }

    protected void entering(final Node node, int level) throws RepositoryException
    {
        if(isCacheEntryNode(node)){
            try {
                final CacheKey cacheKey = getCacheKey(node);
                if(StringUtils.equals(cacheKey.toString(), cacheKeyStr)) {
                    cacheContent = new EntryNodeToCacheContentHandler(node).get();
                }
            } catch (Exception e) {
                log.error("Exception occured in retrieving the cacheKey from the entryNode", e);
                throw new RepositoryException(e);
            }
        }
    }

    private CacheKey getCacheKey(final Node node) throws Exception
    {
        return new EntryNodeToCacheKeyHandler(node, dclm).get();
    }

    public void visit(Node node) throws RepositoryException {
        if(cacheContent == null){
            //only continue visiting
            super.visit(node);
        }
    }
}
