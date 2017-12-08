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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;
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
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.EntryNodeToCacheContentHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.AllEntryNodesCountVisitor;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.EntryNodeByStringKeyVisitor;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.EntryNodeMapVisitor;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.ExpiredNodesVisitor;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.InvalidateAllNodesVisitor;
import com.adobe.acs.commons.httpcache.store.jcr.impl.visitor.InvalidateByCacheConfigVisitor;
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
            name = JCRHttpCacheStoreImpl.PN_ROOTPATH,
            value = JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH
        ),
        @Property(
            label = "Bucket Tree depth",
            description = "The depth the bucket tree goes. Minimum value is 1. This should be between 8 and 10.",
            name = JCRHttpCacheStoreImpl.PN_BUCKETDEPTH,
            intValue = JCRHttpCacheStoreImpl.DEFAULT_BUCKETDEPTH,
            propertyPrivate = true
        ),
        @Property(
            label = "Delta save threshold",
            description = "The threshold to remove nodes when invalidating the cache",
            name = JCRHttpCacheStoreImpl.PN_SAVEDELTA,
            intValue = JCRHttpCacheStoreImpl.DEFAULT_SAVEDELTA
        ),
        @Property(
            label = "Expiretime in ms",
            description = "The time seconds after which nodes will be removed by the scheduled cleanup service. ",
            name = JCRHttpCacheStoreImpl.PN_EXPIRETIMEINSECONDS,
            intValue = JCRHttpCacheStoreImpl.DEFAULT_EXPIRETIMEINSECONDS
        )
})
public class JCRHttpCacheStoreImpl extends AbstractJCRCacheMBean<CacheKey, CacheContent> implements HttpCacheStore, JcrCacheMBean, Runnable {

    //property keys
    public static final String  PN_ROOTPATH            = "httpcache.config.jcr.roothpath",
                                PN_BUCKETDEPTH         = "httpcache.config.jcr.bucketdepth",
                                PN_SAVEDELTA           = "httpcache.config.jcr.savedelta",
                                PN_EXPIRETIMEINSECONDS = "httpcache.config.jcr.expiretimeinms";

    //defaults
    public static final String  DEFAULT_ROOTPATH            = "/etc/acs-commons/httpcache/root";
    public static final int     DEFAULT_BUCKETDEPTH         = 10,
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
        cacheRootPath = PropertiesUtil.toString(properties.get(PN_ROOTPATH), DEFAULT_ROOTPATH);
        bucketTreeDepth = PropertiesUtil.toInteger(properties.get(PN_BUCKETDEPTH), DEFAULT_BUCKETDEPTH);
        deltaSaveThreshold = PropertiesUtil.toInteger(properties.get(PN_SAVEDELTA), DEFAULT_SAVEDELTA);
        expireTimeInSeconds = PropertiesUtil.toInteger(properties.get(PN_EXPIRETIMEINSECONDS), DEFAULT_EXPIRETIMEINSECONDS);
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
                final Node rootNode = session.getNode(cacheRootPath);
                final AllEntryNodesCountVisitor visitor = new AllEntryNodesCountVisitor(11, deltaSaveThreshold);
                visitor.visit(rootNode);
                return visitor.getTotalEntryNodeCount();
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
            final Node rootNode = session.getNode(cacheRootPath);
            final InvalidateAllNodesVisitor visitor = new InvalidateAllNodesVisitor(11, deltaSaveThreshold, cacheRootPath);
            visitor.visit(rootNode);
            incrementEvictionCount(visitor.getEvictionCount());
            }
        });
    }

    @Override
    public void invalidate(final HttpCacheConfig cacheConfig) {
        withSession(new Consumer<Session>()
        {
            @Override public void accept(Session session) throws Exception
            {
                final InvalidateByCacheConfigVisitor visitor = new InvalidateByCacheConfigVisitor(11, deltaSaveThreshold, cacheConfig, dclm);
                final Node rootNode = session.getNode(cacheRootPath);
                visitor.visit(rootNode);
                incrementEvictionCount(visitor.getEvictionCount());
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
                final Node rootNode = session.getNode(cacheRootPath);
                final ExpiredNodesVisitor visitor = new ExpiredNodesVisitor(11, deltaSaveThreshold);
                visitor.visit(rootNode);
                incrementEvictionCount(visitor.getEvictionCount());
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

    @Override public String getCacheEntry(final String cacheKeyStr) throws Exception {
        return withSession(new Function<Session, String>()
        {
            @Override public String apply(Session session) throws Exception
            {
                EntryNodeByStringKeyVisitor visitor = new EntryNodeByStringKeyVisitor(11, dclm, cacheKeyStr);
                final Node rootNode = session.getNode(cacheRootPath);
                visitor.visit(rootNode);
                CacheContent content = visitor.getCacheContentIfPresent();

                if(content != null)
                    return IOUtils.toString(content.getInputDataStream());
                else
                    return "not found";
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
        return withSession(new Function<Session, Map<CacheKey,CacheContent>>()
        {
            @Override public Map<CacheKey,CacheContent> apply(Session session) throws Exception
            {
                final Node rootNode = session.getNode(cacheRootPath);
                final EntryNodeMapVisitor visitor = new EntryNodeMapVisitor(11, deltaSaveThreshold, dclm);
                visitor.visit(rootNode);
                return visitor.getCache();
            }
        });
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
