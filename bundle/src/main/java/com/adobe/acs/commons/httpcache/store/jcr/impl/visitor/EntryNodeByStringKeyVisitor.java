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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheContentHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheKeyHandler;

import java.io.IOException;

public class EntryNodeByStringKeyVisitor extends AbstractNodeVisitor
{
    private static final Logger log = LoggerFactory.getLogger(EntryNodeByStringKeyVisitor.class);

    private final DynamicClassLoaderManager dclm;
    private final String cacheKeyStr;
    private CacheContent cacheContent;

    public EntryNodeByStringKeyVisitor(int maxLevel, DynamicClassLoaderManager dclm, String cacheKeyStr) {
        super( maxLevel, -1);
        this.dclm = dclm;
        this.cacheKeyStr = cacheKeyStr;
    }

    public CacheContent getCacheContentIfPresent()
    {
        return cacheContent;
    }

    protected void entering(final Node node, int level) throws RepositoryException
    {
        super.entering(node, level);

        if(isCacheEntryNode(node)){
            try {
                final CacheKey cacheKey = getCacheKey(node);
                if(StringUtils.equals(cacheKey.toString(), cacheKeyStr)) {
                    cacheContent = new EntryNodeToCacheContentHandler(node).get();
                }
            } catch (Exception e) {
                log.error("Exception occured in retrieving the cacheKey from the entryNode", e);
                throw new RepositoryException(e);
            }
        }
    }

    private CacheKey getCacheKey(final Node node) throws RepositoryException, IOException, ClassNotFoundException {
        return new EntryNodeToCacheKeyHandler(node, dclm).get();
    }

    public void visit(Node node) throws RepositoryException {
        if(cacheContent == null){
            //only continue visiting
            super.visit(node);
        }
    }
}
