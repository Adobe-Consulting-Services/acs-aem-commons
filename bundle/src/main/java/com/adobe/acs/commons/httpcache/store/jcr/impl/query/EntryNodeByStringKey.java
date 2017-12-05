package com.adobe.acs.commons.httpcache.store.jcr.impl.query;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheKeyHandler;

public class EntryNodeByStringKey extends AbstractCacheEntryQuery
{
    private static final String QUERY = "SELECT * FROM [nt:unstructured] AS node WHERE ISDESCENDANTNODE(node,'%s') AND node.isCacheEntryNode = CAST('true' AS BOOLEAN)";
    private final DynamicClassLoaderManager dynamicClassLoaderManager;
    private final String cacheKeyString;

    public EntryNodeByStringKey(
            Session session,
            String cacheRootPath,
            DynamicClassLoaderManager dynamicClassLoaderManager,
            String cacheKeyString
    )
    {
        super(session,cacheRootPath);
        this.dynamicClassLoaderManager = dynamicClassLoaderManager;
        this.cacheKeyString = cacheKeyString;
    }

    protected String createQueryStatement()
    {
        return String.format(QUERY,getCacheRootPath());
    }

    public Node get() throws RepositoryException
    {
        final NodeIterator nodeIterator = getQueryResult().getNodes();

        while(nodeIterator.hasNext()){
            final Node node = nodeIterator.nextNode();

            final EntryNodeToCacheKeyHandler entryNodeToCacheKeyHandler = new EntryNodeToCacheKeyHandler(node, dynamicClassLoaderManager);

            try {
                CacheKey  cacheKey = entryNodeToCacheKeyHandler.get();
                if(StringUtils.equals(cacheKey.toString(), cacheKeyString)){
                    return node;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


        }
        return null;
    }
}
