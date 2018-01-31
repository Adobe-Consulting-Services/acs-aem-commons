package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;

/**
 * Removes all nodes by traversal and saves based on save threshold.
 */
public class InvalidateAllNodesVisitor extends AbstractNodeVisitor
{

    private final String rootNodePath;

    public InvalidateAllNodesVisitor( int maxLevel, long deltaSaveThreshold, String rootNodePath) {
        super(maxLevel, deltaSaveThreshold);
        this.rootNodePath = rootNodePath;
    }



    protected void leaving(final Node node, int level) throws RepositoryException
    {
        final String nodeName = node.getName();
        if(nodeQualifiesForRemoval(node, nodeName)){
            node.remove();
            persistSession();
        }
        super.leaving(node, level);
    }

    private boolean nodeQualifiesForRemoval(Node node, String nodeName) throws RepositoryException
    {
        return
                !StringUtils.equals(nodeName, AccessControlConstants.NT_REP_POLICY)
                        &&
                !StringUtils.equals(nodeName, JcrConstants.JCR_CONTENT)
                        &&
                !StringUtils.equals(node.getPath(), rootNodePath);
    }
}