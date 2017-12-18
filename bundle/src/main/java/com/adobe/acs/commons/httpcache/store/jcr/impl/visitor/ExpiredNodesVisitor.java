package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

/**
 * Traversed and automatically cleans up expired cache entry nodes / bucket nodes.
 */
public class ExpiredNodesVisitor extends AbstractNodeVisitor {

    public ExpiredNodesVisitor( int maxLevel, long deltaSaveThreshold) {
        super(maxLevel, deltaSaveThreshold);
    }

    protected void leaving(final Node node, int level) throws RepositoryException
    {
        if(isCacheEntryNode(node)) {
            //check and remove nodes that are expired.
            checkNodeForExpiry(node);
        }else if(isEmptyBucketNode(node)) {
            //cleanup empty bucket nodes.
            node.remove();
            persistSession();
        }
        super.leaving(node, level);
    }

    private void checkNodeForExpiry(final Node node) throws RepositoryException
    {
        if(node.hasProperty(JCRHttpCacheStoreConstants.PN_EXPIRES_ON)){
            final Property expiryProperty = node.getProperty(JCRHttpCacheStoreConstants.PN_EXPIRES_ON);

            final Calendar expireDate = expiryProperty.getDate();
            final Calendar now = Calendar.getInstance();

            if(expireDate.before(now)) {
                node.remove();
                persistSession();
            }
        }
    }




}
