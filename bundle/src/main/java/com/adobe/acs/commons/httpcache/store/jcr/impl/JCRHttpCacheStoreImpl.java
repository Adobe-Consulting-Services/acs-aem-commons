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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
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
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.BucketNodeHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheKeyHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheContentHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.AllEntryNodes;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.AllEntryNodesCount;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.AllExpiredEntries;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.EntryNodeByStringKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.query.TotalCacheSize;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;

/**
 * ACS AEM Commons - HTTP Cache - JCR based cache store implementation.
 */
@Component( label = "ACS AEM Commons - JCR Cache - Java Content Repository cache store.",
            description = "Cache data store implementation for JCR storage.",
            metatype = true)
@Service( value = {HttpCacheStore.class, JcrCacheMBean.class, Runnable.class} )
@Properties({
        @Property(
            name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
            value = HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE,
            propertyPrivate = true),
        @Property(name = "jmx.objectname",
            value = "com.adobe.acs.httpcache:type=JCR HTTP Cache Store",
            propertyPrivate = true),
        @Property(
            label = "Cron expression defining when this Scheduled Service will run",
            description = "[every minute = 0 * * * * ?] Visit www.cronmaker.com to generate cron expressions.",
            name = "scheduler.expression",
            value = "0 0 12 1/1 * ? *"
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
        ),
        @Property(
            label = "Expiretime in ms",
            description = "The time seconds after which nodes will be removed by the scheduled cleanup service. ",
            name = JCRHttpCacheStoreImpl.PROP_EXPIRETIMEINSECONDS,
            intValue = JCRHttpCacheStoreImpl.DEFAULT_EXPIRETIMEINSECONDS
        )
})
public class JCRHttpCacheStoreImpl implements HttpCacheStore, JcrCacheMBean, Runnable {

    //property keys
    public static final String  PROP_ROOTPATH            = "httpcache.config.jcr.roothpath",
                                PROP_BUCKETDEPTH         = "httpcache.config.jcr.bucketdepth",
                                PROP_SAVEDELTA           = "httpcache.config.jcr.savedelta",
                                PROP_EXPIRETIMEINSECONDS = "httpcache.config.jcr.expiretimeinms";

    //defaults
    public static final String  DEFAULT_ROOTPATH            = "/etc/acs-commons/httpcacheroot";
    public static final int     DEFAULT_BUCKETDEPTH         = 3,
                                DEFAULT_SAVEDELTA           = 500,
                                DEFAULT_EXPIRETIMEINSECONDS = 6000;

    private static final Logger log = LoggerFactory.getLogger(JCRHttpCacheStoreImpl.class);

    //fields
    private String              cacheRootPath;
    private int                 bucketTreeDepth,
                                deltaSaveThreshold,
                                expireTimeInSeconds;

    @Reference private ResourceResolverFactory   resourceResolverFactory;
    @Reference private DynamicClassLoaderManager dclm;

    private final CopyOnWriteArrayList<CacheKeyFactory> cacheKeyFactories = new CopyOnWriteArrayList<CacheKeyFactory>();


    @Activate protected void activate(ComponentContext context)
    {
        Dictionary<?, ?> properties = context.getProperties();
        cacheRootPath = PropertiesUtil.toString(properties.get(PROP_ROOTPATH), DEFAULT_ROOTPATH);
        bucketTreeDepth = PropertiesUtil.toInteger(properties.get(PROP_BUCKETDEPTH), DEFAULT_BUCKETDEPTH);
        deltaSaveThreshold = PropertiesUtil.toInteger(properties.get(PROP_SAVEDELTA), DEFAULT_SAVEDELTA);
        expireTimeInSeconds = PropertiesUtil.toInteger(properties.get(PROP_EXPIRETIMEINSECONDS), DEFAULT_EXPIRETIMEINSECONDS);
    }

    @Deactivate protected void deactivate(){
    }

    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {

        Session session = null;

        try {
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
            final Node bucketNode = factory.getBucketNode();

            final Node entryNode = new BucketNodeHandler(bucketNode, dclm).createOrRetrieveEntryNode(key);

            new EntryNodeWriter(session, entryNode, key, content, expireTimeInSeconds).write();
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
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
            final Node bucketNode = factory.getBucketNode();

            if(bucketNode != null)
                return (null != new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key));
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
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
            final Node bucketNode = factory.getBucketNode();

            if(bucketNode != null) {
                final Node entryNode = new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key);
                return new EntryNodeToCacheContentHandler(entryNode).get();
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
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            return new AllEntryNodesCount(session,cacheRootPath).get();

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
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

            final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
            final Node bucketNode = factory.getBucketNode();

            if(bucketNode != null){
                final Node entryNode = new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key);
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

    @Override
    public void invalidateAll(){
        Session session = null;
        try {
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

            final NodeIterator nodeIterator = new AllEntryNodes(session,cacheRootPath).get();
            int delta = 0;
            while (nodeIterator.hasNext()) {
                delta++;
                Node node = nodeIterator.nextNode();
                node.remove();
                if(delta > deltaSaveThreshold || !nodeIterator.hasNext())
                    session.save();
            }
            session.save();

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
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

            final NodeIterator nodeIterator = new AllEntryNodes(session, cacheRootPath).get();
            int delta = 0;
            while(nodeIterator.hasNext()){
                delta++;
                final Node entryNode = nodeIterator.nextNode();
                final CacheKey key = new EntryNodeToCacheKeyHandler(entryNode, dclm).get();
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
        purgeExpiredEntries();
    }

    @Override public void purgeExpiredEntries(){
        Session session = null;
        try {
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);

            final NodeIterator nodeIterator = new AllExpiredEntries(session, cacheRootPath).get();

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

    @Override public long getTtl()
    {
        return expireTimeInSeconds;
    }

    @Override public void clearCache()
    {
        invalidateAll();
    }

    @Override public long getCacheEntriesCount()
    {
        return size();
    }

    @Override public String getCacheSize(){
        Session session = null;
        String total;
        try {
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            total = new TotalCacheSize(session,cacheRootPath).get();

        } catch (Exception e) {
            log.error("Error retrieving the node count of entries", e);
            total = e.getMessage();
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }
        return total;

    }

    @Override public TabularData getCacheStats() throws OpenDataException
    {
        return null;
    }

    @Override public String getCacheEntry(String cacheKeyStr) throws Exception
    {

        Session session = null;

        try {
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            Node entryNode = new EntryNodeByStringKey(session,cacheRootPath, dclm, cacheKeyStr).get();
            EntryNodeToCacheContentHandler builder = new EntryNodeToCacheContentHandler(entryNode);
            return IOUtils.toString(builder.get().getInputDataStream());

        } catch (Exception e) {
            log.error("Error retrieving the node count of entries", e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }

        return "No entry found!";

    }

    @Override public TabularData getCacheContents() throws OpenDataException
    {
        final CompositeType cacheEntryType = getCacheEntryType();

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType("Cache Entries", "Cache Entries", cacheEntryType, new String[] { "Cache Key" }));

        Session session = null;

        try {
            final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            final NodeIterator nodeIterator = new AllEntryNodes(session, cacheRootPath).get();

            while(nodeIterator.hasNext()){
                final Node node = nodeIterator.nextNode();
                final CacheKey cacheKey = new EntryNodeToCacheKeyHandler(node, dclm).get();
                EntryNodeToCacheContentHandler entryNodeToCacheContentHandler = new EntryNodeToCacheContentHandler(node);
                final CacheContent cacheContent = entryNodeToCacheContentHandler.get();

                final Map<String, Object> data = new HashMap<String, Object>();
                data.put("Cache Key", cacheKey.toString());
                data.put("Status", cacheContent.getStatus());
                data.put("Size",entryNodeToCacheContentHandler.getBinary().getSize() + "b" );
                data.put("Content Type", cacheContent.getContentType());
                data.put("Character Encoding", cacheContent.getCharEncoding());

                tabularData.put(new CompositeDataSupport(cacheEntryType, data));
            }

        } catch (Exception e) {
            log.error("Error retrieving the node count of entries", e);
        } finally {
            if(session != null && session.isLive())
                session.logout();
        }

        return tabularData;
    }

    protected void bindCacheKeyFactory(CacheKeyFactory cacheKeyFactory){
        cacheKeyFactories.add(cacheKeyFactory);
    }

    protected void unbindCacheKeyFactory(CacheKeyFactory cacheKeyFactory){
        if(cacheKeyFactories.contains(cacheKeyFactory))
            cacheKeyFactories.remove(cacheKeyFactory);
    }


    protected CompositeType getCacheEntryType() throws OpenDataException {
        return new CompositeType("Cache Entry", "Cache Entry",
                new String[] { "Cache Key", "Status", "Size", "Content Type", "Character Encoding" },
                new String[] { "Cache Key", "Status", "Size", "Content Type", "Character Encoding" },
                new OpenType[] { SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING });

    }
}
