package com.adobe.acs.commons.httpcache.store.jcr.impl.query;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;

public class TotalCacheSize extends AbstractCacheEntryQuery
{
    private static final String QUERY = "SELECT * FROM [nt:unstructured] AS node WHERE ISDESCENDANTNODE(node,'%s') AND node.isCacheEntryNode = CAST('true' AS BOOLEAN)";

    public TotalCacheSize(Session session, String cacheRootPath)
    {
        super(session,cacheRootPath);
    }

    protected String createQueryStatement()
    {
        return String.format(QUERY,getCacheRootPath());
    }

    public String get() throws RepositoryException
    {
        final NodeIterator entryIterator = getQueryResult().getNodes();
        long bytes = 0;

        while(entryIterator.hasNext()){
            final Node entryNode = entryIterator.nextNode();
            final Node contents = entryNode.getNode("contents");

            final Binary binary = contents.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA).getBinary();
            bytes += binary.getSize();
        }

        return bytes + " bytes";


    }
}
