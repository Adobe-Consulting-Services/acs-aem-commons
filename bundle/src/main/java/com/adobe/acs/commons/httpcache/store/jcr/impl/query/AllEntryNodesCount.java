package com.adobe.acs.commons.httpcache.store.jcr.impl.query;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public final class AllEntryNodesCount extends AbstractCacheEntryQuery
{
    private static final String QUERY = "SELECT * FROM [nt:unstructured] AS node WHERE ISDESCENDANTNODE(node,'%s') AND node.isCacheEntryNode = CAST('true' AS BOOLEAN)";

    public AllEntryNodesCount(Session session, String cacheRootPath)
    {
        super(session,cacheRootPath);
    }

    @Override protected String createQueryStatement()
    {
        return String.format(QUERY,getCacheRootPath());
    }

    public long get() throws RepositoryException
    {
        return getQueryResult().getNodes().getSize();
    }
}
