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
package com.adobe.acs.commons.httpcache.store.jcr.impl.handler;

import static org.apache.jackrabbit.commons.JcrUtils.getOrCreateUniqueByPath;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

public class BucketNodeHandler
{

    private final Node bucketNode;
    private final DynamicClassLoaderManager dynamicClassLoaderManager;

    public BucketNodeHandler(Node node, DynamicClassLoaderManager dynamicClassLoaderManager){
        this.bucketNode = node;
        this.dynamicClassLoaderManager = dynamicClassLoaderManager;
    }

    public Node createOrRetrieveEntryNode(CacheKey key)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final Node existingEntryNode = getEntryIfExists(key);

        if(null != existingEntryNode) {
            return existingEntryNode;
        }else {
            return getOrCreateUniqueByPath(bucketNode, JCRHttpCacheStoreConstants.PATH_ENTRY, JCRHttpCacheStoreConstants.OAK_UNSTRUCTURED);
        }
    }

    public Node getEntryIfExists(CacheKey key)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final NodeIterator entryNodeIterator  = bucketNode.getNodes();

        while(entryNodeIterator.hasNext()){
            Node entryNode = entryNodeIterator.nextNode();
            CacheKey entryKey = new EntryNodeToCacheKeyHandler(entryNode, dynamicClassLoaderManager).get();
            if(key.equals(entryKey)) {
                return entryNode;
            }
        }

        return null;
    }
}
