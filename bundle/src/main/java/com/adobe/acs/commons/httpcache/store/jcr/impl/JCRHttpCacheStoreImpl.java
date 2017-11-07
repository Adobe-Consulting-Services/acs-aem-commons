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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Dictionary;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import org.apache.commons.lang.NotImplementedException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.TempSink;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.AllEntryNodes;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.AllEntryNodesCount;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.workflow.exec.WorkflowProcess;

/**
 * ACS AEM Commons - HTTP Cache - JCR based cache store implementation.
 */
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
        ),
        @Property(
            label = "Cache Root Path location",
            description = "Points to the location cache root node in the JCR repository",
            name = JCRHttpCacheStoreImpl.PROP_ROOTPATH,
            value = JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH
        ),
        @Property(
            label = "Bucket Tree depth",
            description = "The depth the bucket tree goes. Minimum value is 1.",
            name = JCRHttpCacheStoreImpl.PROP_BUCKETDEPTH,
            intValue = JCRHttpCacheStoreImpl.DEFAULT_BUCKETDEPTH
        ),
        @Property(
            label = "Delta save threshold",
            description = "The threshold to remove nodes when invalidating the cache",
            name = JCRHttpCacheStoreImpl.PROP_SAVEDELTA,
            intValue = JCRHttpCacheStoreImpl.DEFAULT_SAVEDELTA
        )
})
@References({
        @Reference(
            referenceInterface = CacheKeyFactory.class,
            policy = ReferencePolicy.DYNAMIC,
            name = "cacheKeyFactory",
            cardinality = ReferenceCardinality.MANDATORY_MULTIPLE
        )
})
public class JCRHttpCacheStoreImpl implements HttpCacheStore, JcrCacheMBean, Runnable {

    public static final String PROP_ROOTPATH = "httpcache.config.jcr.roothpath",
                                PROP_BUCKETDEPTH = "httpcache.config.jcr.bucketdepth",
                                PROP_SAVEDELTA = "httpcache.config.jcr.savedelta";

    public static final String DEFAULT_ROOTPATH = "/etc/acs-commons/httpcacheroot";
    public static final int    DEFAULT_BUCKETDEPTH = 3,
                                DEFAULT_SAVEDELTA = 500;

    private static final Logger log = LoggerFactory.getLogger(JCRHttpCacheStoreImpl.class);

    @Reference private ResourceResolverFactory resourceResolverFactory;


    private final CopyOnWriteArrayList<CacheKeyFactory> cacheKeyFactories = new CopyOnWriteArrayList<>();

    private String cacheRootPath;
    private int bucketTreeDepth;
    private int deltaSaveThreshold;



    @Activate protected void activate(ComponentContext context)
    {
        Dictionary<?, ?> properties = context.getProperties();
        cacheRootPath = PropertiesUtil.toString(properties.get(PROP_ROOTPATH), DEFAULT_ROOTPATH);
        bucketTreeDepth = PropertiesUtil.toInteger(properties.get(PROP_BUCKETDEPTH), DEFAULT_BUCKETDEPTH);
        deltaSaveThreshold = PropertiesUtil.toInteger(properties.get(PROP_SAVEDELTA), DEFAULT_SAVEDELTA);
    }

    @Deactivate protected void deactivate(){


    }

    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {

        Session session = null;

        try {
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
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
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
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
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
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
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
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
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
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
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

            NodeIterator nodeIterator = new AllEntryNodes(session).get();
            int delta = 0;
            while (nodeIterator.hasNext()) {
                delta++;
                Node node = nodeIterator.nextNode();
                node.remove();
                if(delta > deltaSaveThreshold || !nodeIterator.hasNext())
                    session.save();
            }

        } catch (Exception e) {
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
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

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
        return new MemTempSinkImpl();
    }

    @Override public void run()
    {
        Session session = null;
        try {
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

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
        InputStream inputStream = cacheKeyProperty.getBinary().getStream();

        for(CacheKeyFactory cacheKeyFactory : cacheKeyFactories){
            CacheKey cacheKey = cacheKeyFactory.unserialize(inputStream);
            if(cacheKey != null)
                return cacheKey;
        }
        return null;
    }

    protected void bindCacheKeyFactory(CacheKeyFactory cacheKeyFactory){
        cacheKeyFactories.add(cacheKeyFactory);
    }

    protected void unbindCacheKeyFactory(CacheKeyFactory cacheKeyFactory){
        if(cacheKeyFactories.contains(cacheKeyFactory))
            cacheKeyFactories.remove(cacheKeyFactory);
    }

    @Override public long getTtl()
    {
        return 0;
    }

    @Override public void clearCache()
    {
        invalidateAll();
    }

    @Override public long getCacheEntriesCount()
    {
        return size();
    }

    @Override public String getCacheSize()
    {
        return null;
    }

    @Override public TabularData getCacheStats() throws OpenDataException
    {
        return null;
    }

    @Override public String getCacheEntry(String cacheKeyStr) throws Exception
    {
        return null;
    }

    @Override public TabularData getCacheContents() throws OpenDataException
    {
        return null;
    }
}
