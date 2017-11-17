package com.adobe.acs.commons.httpcache.store.jcr.impl.query;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public final class AllExpiredEntries extends AbstractCacheEntryQuery
{
    private static final String QUERY = "SELECT * FROM [nt:unstructured] AS node WHERE ISDESCENDANTNODE(node,'%s') AND node.isCacheEntryNode = CAST('true' AS BOOLEAN) AND node.expiresOn < %d";

    public AllExpiredEntries(Session session, String cacheRootPath)
    {
        super(session,cacheRootPath);
    }

    @Override protected String createQueryStatement()
    {
        return String.format(QUERY,getCacheRootPath(), System.currentTimeMillis());
    }

    public NodeIterator get() throws RepositoryException
    {
        return getQueryResult().getNodes();
    }
}
