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

import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.functions.CheckedFunction;

import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
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
import com.adobe.acs.commons.httpcache.store.jcr.impl.exceptions.BucketNodeFactoryException;
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
@Component(
        label = "ACS AEM Commons - HTTP Cache - JCR Cache Store",
        description = "Cache data store implementation for JCR storage.",
        metatype = true
)
@Properties({
        @Property(
                name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
                value = HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE,
                propertyPrivate = true
        ),
        @Property(
                name = "jmx.objectname",
                value = "com.adobe.acs.commons.httpcache:type=HTTP Cache - JCR Cache Store",
                propertyPrivate = true
        ),
        @Property(
                label = "Cache clean-up schedule",
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
                label = "Cache-root Parent Path location",
                description = "Points to the location of the cache root parent node in the JCR repository",
                name = JCRHttpCacheStoreImpl.PN_ROOTPATH,
                value = JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH
        ),
        @Property(
                label = "Cache bucketing tree depth",
                description = "The depth the bucket tree goes. Minimum value is 1. "
                        + "This value can be used for tweaking performance. "
                        + "The more data cached, the higher this value should be. "
                        + "Downside is that the higher the value, the longer the retrieval of cache entries takes if the buckets are relatively low on entries.",
                name = JCRHttpCacheStoreImpl.PN_BUCKETDEPTH,
                intValue = JCRHttpCacheStoreImpl.DEFAULT_BUCKETDEPTH,
                propertyPrivate = true
        ),
        @Property(
                label = "Save threshold",
                description = "The threshold to add,remove and modify nodes when handling the cache",
                name = JCRHttpCacheStoreImpl.PN_SAVEDELTA,
                intValue = JCRHttpCacheStoreImpl.DEFAULT_SAVEDELTA
        ),
        @Property(
                label = "Expire time in seconds",
                description = "The time seconds after which nodes will be removed by the scheduled cleanup service. ",
                name = JCRHttpCacheStoreImpl.PN_EXPIRETIMEINSECONDS,
                intValue = JCRHttpCacheStoreImpl.DEFAULT_EXPIRETIMEINSECONDS
        )
})
@Service(value = {HttpCacheStore.class, Runnable.class})
public class JCRHttpCacheStoreImpl extends AbstractJCRCacheMBean<CacheKey, CacheContent> implements HttpCacheStore, JcrCacheMBean, Runnable {

    //property keys
    public static final String PN_ROOTPATH = "httpcache.config.jcr.rootpath";
    public static final String PN_BUCKETDEPTH = "httpcache.config.jcr.bucketdepth";
    public static final String PN_SAVEDELTA = "httpcache.config.jcr.savedelta";
    public static final String PN_EXPIRETIMEINSECONDS = "httpcache.config.jcr.expiretimeinseconds";

    //defaults
    public static final String DEFAULT_ROOTPATH = "/var/acs-commons/httpcache";
    private static final String SERVICE_NAME = "httpcache-jcr-storage-service";

    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    //By default, we go for the maximum bucket depth. This uses the full hashcode of 10 digits.
    public static final int DEFAULT_BUCKETDEPTH = 10;

    //Perform a save on a delta of 500 by default.
    public static final int DEFAULT_SAVEDELTA = 500;

    // 1 week.
    public static final int DEFAULT_EXPIRETIMEINSECONDS = 604800;

    private static final Logger log = LoggerFactory.getLogger(JCRHttpCacheStoreImpl.class);

    //fields
    private String cacheRootPath;
    private int bucketTreeDepth;
    private int deltaSaveThreshold;
    private int expireTimeInSeconds;
    
    
    protected Clock clock;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private DynamicClassLoaderManager dclm;

    private final CopyOnWriteArrayList<CacheKeyFactory> cacheKeyFactories = new CopyOnWriteArrayList<CacheKeyFactory>();

    public JCRHttpCacheStoreImpl() throws NotCompliantMBeanException {
        super(JcrCacheMBean.class);
        clock = Clock.systemUTC();
    }

    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        cacheRootPath = PropertiesUtil.toString(properties.get(PN_ROOTPATH), DEFAULT_ROOTPATH) + "/" + JCRHttpCacheStoreConstants.ROOT_NODE_NAME;
        bucketTreeDepth = PropertiesUtil.toInteger(properties.get(PN_BUCKETDEPTH), DEFAULT_BUCKETDEPTH);
        deltaSaveThreshold = PropertiesUtil.toInteger(properties.get(PN_SAVEDELTA), DEFAULT_SAVEDELTA);
        expireTimeInSeconds = PropertiesUtil.toInteger(properties.get(PN_EXPIRETIMEINSECONDS), DEFAULT_EXPIRETIMEINSECONDS);
    }

    @Override
    public void put(final CacheKey key, final CacheContent content) throws HttpCacheDataStreamException {
        final long currentTime = clock.instant().toEpochMilli();
        incrementLoadCount();

        withSession((Session session) -> {
            final long expireTimeInMilliSeconds = expireTimeInSeconds * 1000;
            final BucketNodeFactory factory = createBucketNodeFactory(session, key);
            final Node bucketNode = factory.getBucketNode();

            final Node entryNode = createBucketNodeHandler(bucketNode).createOrRetrieveEntryNode(key, expireTimeInMilliSeconds);

            long expiryTime = (key.getExpiryForCreation() > 0) ? key.getExpiryForCreation() : expireTimeInMilliSeconds;

            createEntryNodeWriter(session, entryNode, key, content, expiryTime).write();

            session.save();

            incrementLoadSuccessCount();
            incrementTotalLoadTime(clock.instant().toEpochMilli() - currentTime);
        }, (Exception e) -> {
            incrementLoadExceptionCount();
        });
    }

    /* This is broken out into its own method to allow for easier unit testing */
    protected BucketNodeFactory createBucketNodeFactory(final Session session, final CacheKey key)
            throws RepositoryException, BucketNodeFactoryException {
        return new BucketNodeFactory(session, cacheRootPath, key, bucketTreeDepth);
    }

    /* This is broken out into its own method to allow for easier unit testing */
    protected BucketNodeHandler createBucketNodeHandler(final Node bucketNode) {
        return new BucketNodeHandler(bucketNode, dclm, clock);
    }

    /* This is broken out into its own method to allow for easier unit testing */
    protected EntryNodeWriter createEntryNodeWriter(final Session session, final Node entryNode, final CacheKey key,
            final CacheContent content, long expiryTime) {
        return new EntryNodeWriter(session, entryNode, key, content, expiryTime,clock);
    }

    @Override
    public boolean contains(final CacheKey key) {
        final long currentTime = clock.instant().toEpochMilli();
        incrementRequestCount();

        return withSession((Session session) -> {
            final BucketNodeFactory factory = createBucketNodeFactory(session, key);
            final Node bucketNode = factory.getBucketNode();

            if (bucketNode != null) {
                Node entryNode = createBucketNodeHandler(bucketNode).getEntryIfExists(key);
                if (entryNode != null) {
                    incrementTotalLookupTime(clock.instant().toEpochMilli() - currentTime);
                    incrementHitCount();

                    return true;
                }
            }

            incrementTotalLookupTime(clock.instant().toEpochMilli() - currentTime);
            incrementMissCount();

            return false;
        });
    }

    @Override
    public CacheContent getIfPresent(final CacheKey key) {
        final long currentTime = clock.instant().toEpochMilli();
        incrementRequestCount();

        return withSession((Session session) -> {
            final BucketNodeFactory factory = createBucketNodeFactory(session, key);
            final Node bucketNode = factory.getBucketNode();

            if (bucketNode != null) {
                final Node entryNode = createBucketNodeHandler(bucketNode).getEntryIfExists(key);
                if (entryNode != null) {

                    final CacheContent content = new EntryNodeToCacheContentHandler(entryNode).get();
                    if (content != null) {
                        incrementTotalLookupTime(clock.instant().toEpochMilli() - currentTime);
                        incrementHitCount();
                        return content;
                    }
                }
            }

            incrementTotalLookupTime(clock.instant().toEpochMilli() - currentTime);
            incrementMissCount();

            return null;
        });
    }

    @Override
    public long size() {
        return withSession((Session session) -> {
            final Node rootNode = session.getNode(cacheRootPath);
            final AllEntryNodesCountVisitor visitor = new AllEntryNodesCountVisitor(11);
            visitor.visit(rootNode);
            return visitor.getTotalEntryNodeCount();
        });
    }

    @Override
    public void invalidate(final CacheKey key) {
        withSession((Session session) -> {
            final BucketNodeFactory factory = createBucketNodeFactory(session, key);
            final Node bucketNode = factory.getBucketNode();

            if (bucketNode != null) {
                final Node entryNode = createBucketNodeHandler(bucketNode).getEntryIfExists(key,true);
                if (entryNode != null) {
                    entryNode.remove();
                    session.save();
                    incrementEvictionCount(1);
                }
            }
        });
    }

    @Override
    public void invalidate(final HttpCacheConfig cacheConfig) {
        withSession((Session session) -> {
            final InvalidateByCacheConfigVisitor visitor = new InvalidateByCacheConfigVisitor(11, deltaSaveThreshold, cacheConfig, dclm);
            final Node rootNode = session.getNode(cacheRootPath);
            visitor.visit(rootNode);
            visitor.close();
            incrementEvictionCount(visitor.getEvictionCount());
        });
    }

    @Override
    public void invalidateAll() {
        withSession((Session session) -> {
            final Node rootNode = session.getNode(cacheRootPath);
            final InvalidateAllNodesVisitor visitor = new InvalidateAllNodesVisitor(11, deltaSaveThreshold);
            visitor.visit(rootNode);
            visitor.close();
            incrementEvictionCount(visitor.getEvictionCount());
        });
    }

    @Override
    public TempSink createTempSink() {
        return new MemTempSinkImpl();
    }

    @Override
    public String getStoreType() {
        return HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE;
    }

    @Override
    public void run() {
        purgeExpiredEntries();
    }

    @Override
    public void purgeExpiredEntries() {
        withSession((Session session) -> {
            final Node rootNode = session.getNode(cacheRootPath);
            final ExpiredNodesVisitor visitor = new ExpiredNodesVisitor(11, deltaSaveThreshold, clock);
            visitor.visit(rootNode);
            visitor.close();
            incrementEvictionCount(visitor.getEvictionCount());
        });
    }

    @Override
    public long getTtl() {
        return expireTimeInSeconds;
    }

    @Override
    public void clearCache() {
        invalidateAll();
    }

    @Override
    public String getCacheEntry(final String cacheKeyStr) throws CacheMBeanException {
        return withSession((Session session) -> {
            EntryNodeByStringKeyVisitor visitor = new EntryNodeByStringKeyVisitor(11, dclm, cacheKeyStr);
            final Node rootNode = session.getNode(cacheRootPath);
            visitor.visit(rootNode);
            CacheContent content = visitor.getCacheContentIfPresent();

            if (content != null) {
                return IOUtils.toString(content.getInputDataStream(), "UTF-8");
            } else {
                return "not found";
            }
        });
    }

    protected void bindCacheKeyFactory(CacheKeyFactory cacheKeyFactory) {
        cacheKeyFactories.add(cacheKeyFactory);
    }

    protected void unbindCacheKeyFactory(CacheKeyFactory cacheKeyFactory) {
        if (cacheKeyFactories.contains(cacheKeyFactory)) {
            cacheKeyFactories.remove(cacheKeyFactory);
        }
    }

    @Override
    protected Map<CacheKey, CacheContent> getCacheAsMap() {
        return withSession((Session session) -> {
            final Node rootNode = session.getNode(cacheRootPath);
            final EntryNodeMapVisitor visitor = new EntryNodeMapVisitor(11, dclm);
            visitor.visit(rootNode);
            return visitor.getCache();
        });
    }

    @Override
    protected long getBytesLength(CacheContent cacheObj) {
        try {
            return IOUtils.toByteArray(cacheObj.getInputDataStream()).length;
        } catch (IOException e) {
            log.error("Error reading the byte length on cachecontent {}", cacheObj);
        }
        return 0;
    }

    @Override
    protected void addCacheData(Map<String, Object> data, CacheContent cacheObj) {
        data.put(JMX_PN_STATUS, cacheObj.getStatus());
        data.put(JMX_PN_CONTENTTYPE, cacheObj.getContentType());
        data.put(JMX_PN_CHARENCODING, cacheObj.getCharEncoding());

        try {
            data.put(JMX_PN_SIZE, FileUtils.byteCountToDisplaySize(IOUtils.toByteArray(cacheObj.getInputDataStream()).length));
        } catch (IOException e) {
            log.error("Error adding cache data to JMX data map", e);
            data.put(JMX_PN_SIZE, "0");
        }
    }

    @Override
    protected String toString(CacheContent cacheObj) throws CacheMBeanException {
        try {
            return IOUtils.toString(
                    cacheObj.getInputDataStream(),
                    cacheObj.getCharEncoding());
        } catch (IOException e) {
            throw new CacheMBeanException("Failed to get the cache contents", e);
        }
    }

    @Override
    protected CompositeType getCacheEntryType() throws OpenDataException {
        return new CompositeType(JMX_PN_CACHEENTRY, JMX_PN_CACHEENTRY,
                new String[]{JMX_PN_CACHEKEY, JMX_PN_STATUS, JMX_PN_SIZE, JMX_PN_CONTENTTYPE, JMX_PN_CHARENCODING},
                new String[]{JMX_PN_CACHEKEY, JMX_PN_STATUS, JMX_PN_SIZE, JMX_PN_CONTENTTYPE, JMX_PN_CHARENCODING},
                new OpenType[]{SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});

    }

    public void withSession(final CheckedConsumer<Session> onSuccess) {
        withSession(onSuccess, null);
    }

    public void withSession(final CheckedConsumer<Session> onSuccess, final CheckedConsumer<Exception> onError) {
        withSession(
                (Session session) -> {
                    onSuccess.accept(session);
                    return null;
                },
                onError
        );
    }

    public <T> T withSession(final CheckedFunction<Session, T> onSuccess) {
        return withSession(onSuccess, null);
    }

    public <T> T withSession(final CheckedFunction<Session, T> onSuccess, final CheckedConsumer<Exception> onError) {
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            final Session session = resourceResolver.adaptTo(Session.class);
            return onSuccess.apply(session);

        } catch (Exception e) {
            log.error("Error in executing the session", e);
            try {
                if (onError != null) {
                    onError.accept(e);
                }
            } catch (Exception subException) {
                log.error("Error in handling the exception", subException);
            }
        }
        return null;
    }
}
