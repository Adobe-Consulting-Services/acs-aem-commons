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
