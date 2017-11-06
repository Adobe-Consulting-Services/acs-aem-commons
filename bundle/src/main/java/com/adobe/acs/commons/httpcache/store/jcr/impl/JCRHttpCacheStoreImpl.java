/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.store.jcr.impl;

import static org.apache.jackrabbit.commons.JcrUtils.getOrCreateUniqueByPath;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.NotImplementedException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.TempSink;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.AllEntryNodes;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.AllEntryNodesCount;
import com.day.cq.commons.jcr.JcrConstants;

/**
 * ACS AEM Commons - HTTP Cache - JCR based cache store implementation.
 */

// TODO - Placeholder component. To be implemented.

@Component
@Service

@Properties({
        @Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
                value = HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE,
                propertyPrivate = true),
        @Property(
            label = "Cron expression defining when this Scheduled Service will run",
            description = "[every minute = 0 * * * * ?] Visit www.cronmaker.com to generate cron expressions.",
            name = "scheduler.expression",
            value = "0 1 0 ? * *"
        ),
        @Property(
            label = "Allow concurrent executions",
            description = "Allow concurrent executions of this Scheduled Service. This is almost always false.",
            name = "scheduler.concurrent",
            propertyPrivate = true,
            boolValue = false
        )
})
public class JCRHttpCacheStoreImpl implements HttpCacheStore, Runnable {

    @Reference private Repository repository;


    @Property  private String cacheRootPath;

    @Property
    private int     bucketDimensionDepth,

                    /**
                     * The delta on which nodes are persisted to the repository with session.save().
                     */
                    deltaSaveThreshold;

    private static final Logger log = LoggerFactory.getLogger(JCRHttpCacheStoreImpl.class);

    private final AtomicInteger deltaCounter = new AtomicInteger();

    private Credentials credentials;


    @Activate protected void activate() throws RepositoryException
    {
        credentials = new SimpleCredentials("", "".toCharArray());
    }

    @Deactivate protected void deactivate(){


    }

    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {

        Session session = null;

        try {
            session = repository.login(credentials);
            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketDimensionDepth);
            final Node bucketNode = factory.getBucketNode();

            final Node entryNode = createOrRetrieveEntryNode(bucketNode, key);

            new EntryNodeWriter(session, entryNode, key, content).write();
            session.save();

        } catch (Exception e) {
            log.error("Error persisting cache content in JCR", e);
            throw new HttpCacheDataStreamException(e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
    }


    @Override
    public boolean contains(CacheKey key) {
        Session session = null;

        try {
            session = repository.login(credentials);
            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketDimensionDepth);
            final Node bucketNode = factory.getBucketNode();

            if(bucketNode != null)
                return (null != getEntryIfExists(bucketNode, key));
        } catch (Exception e) {
            log.error("Error checking if the entry node exists in the repository", e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
        return false;
    }

    @Override
    public CacheContent getIfPresent(CacheKey key) {

        Session session = null;

        try {
            session = repository.login(credentials);
            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketDimensionDepth);
            final Node bucketNode = factory.getBucketNode();

            if(bucketNode != null) {
                Node entryNode = getEntryIfExists(bucketNode, key);
                return new EntryNodeToCacheContentBuilder(entryNode).build();
            }


        } catch (Exception e) {
            log.error("Error retrieving the entry node", e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
        return null;
    }



    @Override
    public long size() {
        Session session = null;

        try {
            session = repository.login(credentials);
            return new AllEntryNodesCount(session).get();

        } catch (Exception e) {
            log.error("Error retrieving the node count of entries", e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
        return 0;
    }



    @Override
    public void invalidate(CacheKey key) {

        Session session = null;

        try {
            session = repository.login(credentials);

            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketDimensionDepth);
            final Node bucketNode = factory.getBucketNode();

            if(bucketNode != null){
                Node entryNode = getEntryIfExists(bucketNode, key);
                if(entryNode != null){
                    entryNode.remove();
                    session.save();
                }
            }

        } catch (Exception e) {
            log.error("error invalidating cachekey: {}", key);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
    }

    @Override public void invalidateAll()
    {
        Session session = null;
        try {
            session = repository.login(credentials);
            NodeIterator nodeIterator = new AllEntryNodes(session).get();
            int delta = 0;
            while (nodeIterator.hasNext()) {
                delta++;
                Node node = nodeIterator.nextNode();
                node.remove();
                if(delta > deltaSaveThreshold || !nodeIterator.hasNext())
                    session.save();
            }

        } catch (RepositoryException e) {
            log.error("Error removing bucket nodes from JCRHttpCacheStore", e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
    }

    @Override
    public void invalidate(HttpCacheConfig cacheConfig) {

        Session session = null;
        try {
            session = repository.login(credentials);
            NodeIterator nodeIterator = new AllEntryNodes(session).get();
            int delta = 0;
            while(nodeIterator.hasNext()){
                delta++;
                Node entryNode = nodeIterator.nextNode();
                CacheKey key = getCacheKeyFromEntryNode(entryNode);
                if(cacheConfig.knows(key)) {
                    entryNode.remove();

                    if(delta > deltaSaveThreshold || !nodeIterator.hasNext())
                        session.save();
                }
            }

        } catch (Exception e) {
            log.error("Error removing bucket nodes from JCRHttpCacheStore.", e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
    }

    @Override
    public TempSink createTempSink() {
        throw new NotImplementedException();
    }

    @Override public void run()
    {
        Session session = null;
        try {
            session = repository.login(credentials);
            NodeIterator nodeIterator = new AllEntryNodes(session).get();

            while(nodeIterator.hasNext())
                nodeIterator.nextNode().remove();

            session.save();

        } catch (Exception e) {
            log.error("Error removing bucket nodes from JCRHttpCacheStore.", e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
    }


    private Node createOrRetrieveEntryNode(Node bucketNode, CacheKey key)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final Node existingEntryNode = getEntryIfExists(bucketNode, key);

        if(null != existingEntryNode)
            return existingEntryNode;
        else
            return getOrCreateUniqueByPath(bucketNode, "entry", JcrConstants.NT_UNSTRUCTURED);
    }

    private Node getEntryIfExists(Node bucketNode, CacheKey key)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        NodeIterator entryNodeIterator  = bucketNode.getNodes();

        while(entryNodeIterator.hasNext()){
            Node entryNode = entryNodeIterator.nextNode();
            CacheKey entryKey = getCacheKeyFromEntryNode(entryNode);
            if(key.equals(entryKey))
                return entryNode;
        }

        return null;
    }

    private CacheKey getCacheKeyFromEntryNode(Node entryNode)
            throws RepositoryException, IOException, ClassNotFoundException
    {
        final javax.jcr.Property cacheKeyProperty = entryNode.getProperty("cacheKeySerialized");

        ObjectInputStream objectInputStream = new ObjectInputStream(cacheKeyProperty.getBinary().getStream());
        return (CacheKey) objectInputStream.readObject();
    }



}
