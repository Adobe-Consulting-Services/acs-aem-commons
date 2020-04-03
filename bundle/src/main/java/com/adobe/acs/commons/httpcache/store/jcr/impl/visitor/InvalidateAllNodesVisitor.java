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
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;

/**
 * Removes all nodes by traversal and saves based on save threshold.
 */
public class InvalidateAllNodesVisitor extends AbstractNodeVisitor
{

    public InvalidateAllNodesVisitor( int maxLevel, long deltaSaveThreshold) {
        super(maxLevel, deltaSaveThreshold);
    }



    protected void leaving(final Node node, int level) throws RepositoryException
    {
        if(nodeQualifiesForRemoval(node)){
            node.remove();
            persistSession();
        }
        super.leaving(node, level);
    }

    private boolean nodeQualifiesForRemoval(Node node) throws RepositoryException
    {
        return
                isCacheEntryNode(node)
                        ||
                isBucketNode(node);
    }
}