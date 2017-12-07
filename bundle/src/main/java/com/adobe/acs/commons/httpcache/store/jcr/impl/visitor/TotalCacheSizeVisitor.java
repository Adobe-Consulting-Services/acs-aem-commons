package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import static com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.AbstractNodeVisitor.isCacheEntryNode;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.jackrabbit.JcrConstants;

import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

public class TotalCacheSizeVisitor extends TraversingItemVisitor.Default
{
    private long bytes = 0;

    protected void entering(final Node node, int level) throws RepositoryException
    {
        if(isCacheEntryNode(node)){
            final Node contents = node.getNode(JCRHttpCacheStoreConstants.PATH_CONTENTS);

            final Binary binary = contents.getNode(JcrConstants.JCR_CONTENT).getProperty(JcrConstants.JCR_DATA).getBinary();
            bytes += binary.getSize();
        }
    }

    public long getBytes()
    {
        return bytes;
    }
}
