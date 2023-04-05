/*
 * work-around for ACS AEM Commons issue 3045
 */

package com.adobe.acs.commons.replication.dispatcher;

import com.day.cq.replication.AggregateHandler;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements an aggregate handler that detects hierarchy nodes as aggregate root.
 */
public class FlushAggregateHandler implements AggregateHandler 
{

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(FlushAggregateHandler.class);

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code> if the node is a hierarchy node.
     */
    public boolean isAggregateRoot(Node node) 
	{
        try 
		{
            return node.isNodeType(NodeType.NT_HIERARCHY_NODE);
        } 
		catch (RepositoryException e) 
		{
            log.warn("Unable to determine aggregate root.", e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Returns the provided path.
     */
    public List<String> prepareForReplication(Session session, ReplicationActionType type, String path)
        throws ReplicationException 
	{
        LinkedList<String> paths = new LinkedList<String>();
        paths.add(path);
        return paths;
    }

    /**
     * {@inheritDoc}
     *
     * 
     */
    public void processForReplication(Session session, ReplicationAction action) 
		throws ReplicationException 
	{
    }

} // public class FlushAggregateHandler implements AggregateHandler 
