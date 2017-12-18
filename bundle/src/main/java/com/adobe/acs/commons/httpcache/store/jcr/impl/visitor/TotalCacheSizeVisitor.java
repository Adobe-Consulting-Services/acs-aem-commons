package com.adobe.acs.commons.httpcache.store.jcr.impl.visitor;

import static com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.AbstractNodeVisitor.isCacheEntryNode;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.jackrabbit.JcrConstants;

import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

public class TotalCacheSizeVisitor extends TraversingItemVisitor.Default
{
    private long bytes = 0;

    public TotalCacheSizeVisitor(){
        super(false,-1);
    }

    protected void entering(final Node node, int level) throws RepositoryException
    {
        super.entering(node, level);

        if(isCacheEntryNode(node) && node.hasNode(JCRHttpCacheStoreConstants.PATH_CONTENTS)){
            final Node contents = node.getNode(JCRHttpCacheStoreConstants.PATH_CONTENTS);

            if(contents.hasNode(JcrConstants.JCR_CONTENT)){
                final Node jcrContent = contents.getNode(JcrConstants.JCR_CONTENT);

                if(jcrContent.hasProperty(JcrConstants.JCR_DATA)){
                    final Property property = jcrContent.getProperty(JcrConstants.JCR_DATA);
                    final Binary binary = property.getBinary();
                    bytes += binary.getSize();
                }

            }
        }
    }

    public long getBytes()
    {
        return bytes;
    }
}
