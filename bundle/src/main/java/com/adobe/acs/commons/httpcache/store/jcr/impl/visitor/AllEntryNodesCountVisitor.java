package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class AllEntryNodesCountVisitor extends AbstractNodeVisitor
{
    public AllEntryNodesCountVisitor( int maxLevel, long deltaSaveThreshold) {
        super(maxLevel, deltaSaveThreshold);
    }

    private long totalEntryNodeCount = 0;

    protected void entering(final Node node, int level) throws RepositoryException
    {
        if(isCacheEntryNode(node)) {
            totalEntryNodeCount++;
        }
    }

    public long getTotalEntryNodeCount()
    {
        return totalEntryNodeCount;
    }
}
