package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.util.TraversingItemVisitor;

import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

public abstract class AbstractNodeVisitor extends TraversingItemVisitor.Default
{
    private final long deltaSaveThreshold;
    private long delta = 0;
    private long evictionCount = 0;
    private Session session;

    public AbstractNodeVisitor( int maxLevel, long deltaSaveThreshold) {
        super(false, maxLevel);
        this.deltaSaveThreshold = deltaSaveThreshold;
    }

    public void visit(Node node) throws RepositoryException {
        session = node.getSession();
        super.visit(node);
        //perform final save
        session.save();
        delta = 0;
    }

    public static boolean isCacheEntryNode(final Node node) throws RepositoryException
    {
        return node.hasProperty(JCRHttpCacheStoreConstants.PN_ISCACHEENTRYNODE);
    }

    public static boolean isEmptyBucketNode(final Node node) throws RepositoryException
    {
        return !node.hasProperty(JCRHttpCacheStoreConstants.PN_ISCACHEENTRYNODE) && !node.hasNodes();
    }

    protected void persistSession() throws RepositoryException
    {
        if(delta > deltaSaveThreshold){
            session.save();
            delta = 0;
        }

        delta++;
        evictionCount++;
    }

    public long getEvictionCount()
    {
        return evictionCount;
    }
}
