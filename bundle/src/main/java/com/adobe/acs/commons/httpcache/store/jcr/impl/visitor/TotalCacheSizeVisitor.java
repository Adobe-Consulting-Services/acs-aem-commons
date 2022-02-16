/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
