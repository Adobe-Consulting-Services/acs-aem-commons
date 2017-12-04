package com.adobe.acs.commons.httpcache.store.jcr.impl.query;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AllEntryNodes extends AbstractCacheEntryQuery
{
    private static final String QUERY = "SELECT * FROM [nt:unstructured] AS node WHERE ISDESCENDANTNODE(node,'%s') AND node.isCacheEntryNode = CAST('true' AS BOOLEAN)";

    public AllEntryNodes(Session session, String cacheRootPath)
    {
        super(session,cacheRootPath);
    }

    protected String createQueryStatement()
    {
        return String.format(QUERY,getCacheRootPath());
    }

    public NodeIterator get() throws RepositoryException
    {
        return getQueryResult().getNodes();
    }
}
