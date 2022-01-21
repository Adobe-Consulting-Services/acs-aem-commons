/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.renovator;

import com.adobe.acs.commons.fam.actions.Actions;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.api.WCMException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.adobe.acs.commons.mcp.impl.processes.renovator.Util.*;

/**
 * Represents a page being moved.
 */
public class MovingPage extends MovingNode {

    PageManagerFactory pageManagerFactory;

    public MovingPage(PageManagerFactory pageManagerFactory) {
        this.pageManagerFactory = pageManagerFactory;
    }

    @Override
    public boolean isCopiedBeforeMove() {
        return false;
    }

    @Override
    public boolean isSupposedToBeReferenced() {
        return true;
    }

    @Override
    public boolean isAbleToHaveChildren() {
        return true;
    }

    @Override
    protected boolean isAuditableMove() {
        return true;
    }

    @Override
    public void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws IllegalAccessException, MovingException {
        // For starters, create a page manager with a modified replicator queue
        PageManager manager = pageManagerFactory.getPageManager(rr);
        Replicator replicator = (Replicator) Proxy.newProxyInstance(
                Replicator.class.getClassLoader(),
                new Class[]{Replicator.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        switch (method.getName()){
                            case "replicate":
                                if(args.length == 3){
                                    replicatorQueue.replicate((Session)args[0], (ReplicationActionType)args[1], (String)args[2]);
                                } else if (args.length == 4){
                                    replicatorQueue.replicate((Session)args[0], (ReplicationActionType)args[1], (String)args[2], (ReplicationOptions)args[3]);
                                }
                                break;
                            default:
                                break;
                        }
                        return null;
                    }
                });
        Field replicatorField = FieldUtils.getDeclaredField(manager.getClass(), "replicator", true);
        FieldUtils.writeField(replicatorField, manager, replicator, true);

        // Some simple transformations
        String contentPath = getSourcePath() + "/" + JcrConstants.JCR_CONTENT;
        String destinationParent = StringUtils.substringBeforeLast(getDestinationPath(), "/");

        // Attempt move operation
        try {
            Actions.retry(10, 500, res -> {
                waitUntilResourceFound(res, destinationParent);
                moveOrClonePage(rr, manager, contentPath, destinationParent, res);
                movePageChildren(rr, res);
            }).accept(rr);
        } catch (Exception e) {
            throw new MovingException(getSourcePath(), e);
        }
    }

    private void moveOrClonePage(ResourceResolver rr, PageManager manager, String contentPath, String destinationParent, ResourceResolver res) throws WCMException, PersistenceException {
        Resource source = rr.getResource(getSourcePath());
        if (resourceExists(res, contentPath)) {
            manager.move(source,
                    getDestinationPath(),
                    getPreviousSibling(),
                    true,
                    true,
                    listToStringArray(getAllReferences()),
                    listToStringArray(getPublishedReferences()));
        } else if (!resourceExists(res, getDestinationPath())) {
            Resource parent = res.getResource(destinationParent);
            res.create(parent, source.getName(), getClonedProperties(source));
        }
        res.commit();
        res.refresh();
    }

    private void movePageChildren(ResourceResolver rr, ResourceResolver res) throws MovingException {
        Resource source;
        source = rr.getResource(getSourcePath());
        try {
            if (source != null && source.hasChildren()) {
                for (Resource child : source.getChildren()) {
                    if (!hasChild(child.getPath())) {
                        String childDestination = child.getPath().replaceAll(getSourcePath(), getDestinationPath());
                        String childDestinationParent = StringUtils.substringBeforeLast(childDestination, "/");
                        if (!resourceExists(res, childDestination)) {
                            waitUntilResourceFound(res, childDestinationParent);
                            res.move(child.getPath(), childDestination);
                        }
                    }
                }
                res.commit();
            }
        } catch (PersistenceException e) {
            throw new MovingException(getSourcePath(), e);
        }
    }
}
