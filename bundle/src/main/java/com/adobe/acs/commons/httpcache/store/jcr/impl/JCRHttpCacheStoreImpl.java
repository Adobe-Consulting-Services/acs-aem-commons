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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.commons.io.FileUtils;
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

import com.adobe.acs.commons.functions.Consumer;
import com.adobe.acs.commons.functions.Function;
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
import com.adobe.acs.commons.httpcache.store.jcr.impl.writer.BucketNodeFactory;
import com.adobe.acs.commons.httpcache.store.jcr.impl.writer.EntryNodeWriter;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;
import com.adobe.acs.commons.util.impl.AbstractJCRCacheMBean;
import com.adobe.acs.commons.util.impl.JcrCacheMBean;

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
            description = "The depth the bucket tree goes. Minimum value is 1. This should be between 8 and 10.",
            name = JCRHttpCacheStoreImpl.PROP_BUCKETDEPTH,
            intValue = JCRHttpCacheStoreImpl.DEFAULT_BUCKETDEPTH,
            propertyPrivate = true
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
public class JCRHttpCacheStoreImpl extends AbstractJCRCacheMBean<CacheKey, CacheContent> implements HttpCacheStore, JcrCacheMBean, Runnable {

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

    public JCRHttpCacheStoreImpl() throws NotCompliantMBeanException
    {
        super(JcrCacheMBean.class);
    }

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
    public void put(final CacheKey key, final CacheContent content) throws HttpCacheDataStreamException {
        final long currentTime = System.currentTimeMillis();
        incrementLoadCount();

        withSession(
            new Consumer<Session>(){
                @Override public void accept(Session session) throws Exception{
                    final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
                    final Node bucketNode = factory.getBucketNode();

                    final Node entryNode = new BucketNodeHandler(bucketNode, dclm).createOrRetrieveEntryNode(key);

                    new EntryNodeWriter(session, entryNode, key, content, expireTimeInSeconds).write();
                    session.save();

                    incrementLoadSuccessCount();
                    incrementTotalLoadTime(System.currentTimeMillis() - currentTime);
                }
            },
            new Consumer<Exception>(){
                @Override public void accept(Exception e) throws Exception
                {
                    incrementLoadExceptionCount();
                }
            }
        );
    }

    @Override
    public boolean contains(final CacheKey key) {
        final long currentTime = System.currentTimeMillis();
        incrementRequestCount();

        return withSession(new Function<Session, Boolean>()
        {
            @Override public Boolean apply(Session session) throws Exception
            {
                final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
                final Node bucketNode = factory.getBucketNode();

                if(bucketNode != null) {
                    Node entryNode = new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key);
                    if(entryNode != null){
                        incrementTotalLookupTime(System.currentTimeMillis() - currentTime);
                        incrementHitCount();

                        return true;
                    }
                }

                incrementTotalLookupTime(System.currentTimeMillis() - currentTime);
                incrementMissCount();

                return false;
            }
        });
    }

    @Override
    public CacheContent getIfPresent(final CacheKey key) {
        final long currentTime = System.currentTimeMillis();
        incrementRequestCount();

        return withSession(new Function<Session, CacheContent>()
        {
            @Override public CacheContent apply(Session session) throws Exception
            {
                final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
                final Node bucketNode = factory.getBucketNode();

                if(bucketNode != null) {
                    final Node entryNode = new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key);
                    final CacheContent content = new EntryNodeToCacheContentHandler(entryNode).get();

                    if(content != null){
                        incrementTotalLookupTime(System.currentTimeMillis() - currentTime);
                        incrementHitCount();
                        return content;
                    }
                }

                incrementTotalLookupTime(System.currentTimeMillis() - currentTime);
                incrementMissCount();

                return null;
            }
        });
    }

    @Override
    public long size() {
        return withSession(new Function<Session, Long>()
        {
            @Override public Long apply(Session session) throws Exception
            {
                return new AllEntryNodesCount(session,cacheRootPath).get();
            }
        });
    }

    @Override
    public void invalidate(final CacheKey key) {
        withSession(new Consumer<Session>()
        {
            @Override public void accept(Session session) throws Exception
            {
                final BucketNodeFactory factory = new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
                final Node bucketNode = factory.getBucketNode();

                if(bucketNode != null){
                    final Node entryNode = new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key);
                    if(entryNode != null){
                        entryNode.remove();
                        session.save();
                        incrementEvictionCount(1);
                    }
                }
            }
        });
    }

    @Override
    public void invalidateAll(){
        withSession(new Consumer<Session>()
        {
            @Override public void accept(Session session) throws Exception
            {
                final NodeIterator nodeIterator = new AllEntryNodes(session,cacheRootPath).get();
                int delta = 0;
                while (nodeIterator.hasNext()) {
                    delta++;
                    Node node = nodeIterator.nextNode();
                    node.remove();
                    if(delta > deltaSaveThreshold || !nodeIterator.hasNext()) {
                        session.save();
                        incrementEvictionCount(delta);
                        delta = 0;
                    }
                }
                incrementEvictionCount(delta);
                session.save();
            }
        });
    }

    @Override
    public void invalidate(final HttpCacheConfig cacheConfig) {
        withSession(new Consumer<Session>()
        {
            @Override public void accept(Session session) throws Exception
            {
                final NodeIterator nodeIterator = new AllEntryNodes(session, cacheRootPath).get();
                int delta = 0;
                while(nodeIterator.hasNext()){
                    delta++;
                    final Node entryNode = nodeIterator.nextNode();
                    final CacheKey key = new EntryNodeToCacheKeyHandler(entryNode, dclm).get();
                    if(cacheConfig.knows(key)) {
                        entryNode.remove();

                        if(delta > deltaSaveThreshold || !nodeIterator.hasNext()){
                            session.save();
                            incrementEvictionCount(delta);
                            delta = 0;
                        }
                    }
                }

                session.save();
                incrementEvictionCount(delta);
            }
        });
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
        withSession(new Consumer<Session>()
        {
            @Override public void accept(Session session) throws Exception
            {
                final NodeIterator nodeIterator = new AllExpiredEntries(session, cacheRootPath).get();

                int delta = 0;

                while(nodeIterator.hasNext()){
                    delta++;

                    if(delta > deltaSaveThreshold || !nodeIterator.hasNext()){
                        session.save();
                        incrementEvictionCount(delta);
                        delta = 0;
                    }
                }
                session.save();
                incrementEvictionCount(delta);
            }
        });
    }

    @Override public long getTtl()
    {
        return expireTimeInSeconds;
    }

    @Override public void clearCache()
    {
        invalidateAll();
    }

    @Override public String getCacheEntry(final String cacheKeyStr) throws Exception
    {
        return withSession(new Function<Session, String>()
        {
            @Override public String apply(Session session) throws Exception
            {
                Node entryNode = new EntryNodeByStringKey(session,cacheRootPath, dclm, cacheKeyStr).get();
                EntryNodeToCacheContentHandler builder = new EntryNodeToCacheContentHandler(entryNode);
                return IOUtils.toString(builder.get().getInputDataStream());
            }
        });
    }


    protected void bindCacheKeyFactory(CacheKeyFactory cacheKeyFactory){
        cacheKeyFactories.add(cacheKeyFactory);
    }

    protected void unbindCacheKeyFactory(CacheKeyFactory cacheKeyFactory){
        if(cacheKeyFactories.contains(cacheKeyFactory))
            cacheKeyFactories.remove(cacheKeyFactory);
    }

    @Override protected Map<CacheKey, CacheContent> getCacheAsMap()
    {
        final Map<CacheKey, CacheContent> cache = new HashMap<CacheKey, CacheContent>();

        withSession(new Consumer<Session>()
        {
            @Override public void accept(Session session) throws Exception
            {
                NodeIterator nodeIterator = new AllEntryNodes(session, cacheRootPath).get();

                while(nodeIterator.hasNext()){
                    Node entryNode = nodeIterator.nextNode();
                    CacheKey cacheKey = new EntryNodeToCacheKeyHandler(entryNode, dclm).get();
                    CacheContent content = new EntryNodeToCacheContentHandler(entryNode).get();
                    cache.put(cacheKey, content);
                }
            }
        });

        return cache;
    }

    @Override protected long getBytesLength(CacheContent cacheObj)
    {
        try {
            return IOUtils.toByteArray(cacheObj.getInputDataStream()).length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override protected void addCacheData(Map<String, Object> data, CacheContent cacheObj)
    {
        data.put("Status", cacheObj.getStatus());
        data.put("Content Type", cacheObj.getContentType());
        data.put("Character Encoding", cacheObj.getCharEncoding());

        try {
            data.put("Size", FileUtils.byteCountToDisplaySize(IOUtils.toByteArray(cacheObj.getInputDataStream()).length));
        } catch (IOException e) {
            data.put("Size", "0");
        }
    }

    @Override protected String toString(CacheContent cacheObj) throws Exception
    {
        return IOUtils.toString(
                cacheObj.getInputDataStream(),
                cacheObj.getCharEncoding());
    }

    protected CompositeType getCacheEntryType() throws OpenDataException {
        return new CompositeType("Cache Entry", "Cache Entry",
                new String[] { "Cache Key", "Status", "Size", "Content Type", "Character Encoding" },
                new String[] { "Cache Key", "Status", "Size", "Content Type", "Character Encoding" },
                new OpenType[] { SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING });

    }

    private void withSession(final Consumer<Session> onSuccess){
        withSession(onSuccess, null);
    }

    private void withSession(final Consumer<Session> onSuccess, final Consumer<Exception> onError)
    {
        withSession(new Function<Session, Object>()
        {
            @Override public Object apply(Session session) throws Exception
            {
                onSuccess.accept(session);
                return null;
            }
        },
        onError);
    }

    private <T> T withSession(final Function<Session, T> onSuccess){
        return withSession(onSuccess, null);
    }

    private <T> T withSession(final Function<Session, T> onSuccess, final Consumer<Exception> onError){
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Session session = resourceResolver.adaptTo(Session.class);
            return onSuccess.apply(session);

        } catch (Exception e) {
            log.error("Error in executing the session", e);
            try {
                if(onError != null) {
                    onError.accept(e);
                }
            } catch (Exception subException) {
                log.error("Error in handling the exception");
            }
        } finally {
            if(resourceResolver != null && resourceResolver.isLive())
                resourceResolver.close();
        }
        return null;
    }
}
