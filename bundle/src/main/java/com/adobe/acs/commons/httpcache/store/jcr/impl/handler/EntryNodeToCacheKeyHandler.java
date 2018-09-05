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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;
import com.adobe.acs.commons.util.DynamicObjectInputStream;

public class EntryNodeToCacheKeyHandler
{
    private final Node entryNode;
    private final DynamicClassLoaderManager dynamicClassLoaderManager;

    public EntryNodeToCacheKeyHandler(Node entryNode, DynamicClassLoaderManager dynamicClassLoaderManager){

        this.entryNode = entryNode;
        this.dynamicClassLoaderManager = dynamicClassLoaderManager;
    }

    public CacheKey get()
            throws RepositoryException, IOException, ClassNotFoundException
    {
        if(entryNode != null){
            final Property cacheKeyProperty = entryNode.getProperty(JCRHttpCacheStoreConstants.PN_CACHEKEY);
            final InputStream inputStream = cacheKeyProperty.getBinary().getStream();

            final ClassLoader dynamicClassLoader = dynamicClassLoaderManager.getDynamicClassLoader();


            final DynamicObjectInputStream dynamicObjectInputStream = new DynamicObjectInputStream(inputStream, dynamicClassLoader);
            return (CacheKey) dynamicObjectInputStream.readObject();
        }

        return null;
    }
}
