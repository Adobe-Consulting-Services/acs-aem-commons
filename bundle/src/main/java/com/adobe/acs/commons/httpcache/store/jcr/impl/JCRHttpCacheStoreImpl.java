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
import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ACS AEM Commons - HTTP Cache - JCR based cache store implementation.
 */

@Component(service = {HttpCacheStore.class, JcrCacheMBean.class, Runnable.class}, property = {
        HttpCacheStore.KEY_CACHE_STORE_TYPE + "=" + HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE,
        "jmx.objectname" + "=" + "com.adobe.acs.httpcache:type=JCR HTTP Cache Store",
        "scheduler.concurrent" + "=" + "false"
})
@Designate(ocd = JCRHttpCacheStoreImpl.Config.class)
public class JCRHttpCacheStoreImpl extends AbstractJCRCacheMBean<CacheKey, CacheContent> implements HttpCacheStore, JcrCacheMBean, Runnable {

    @ObjectClassDefinition(name = "ACS AEM Commons - Http Cache - JCR Cache Store.",
            description = "Cache data store implementation for JCR storage.")
    public @interface Config {

        //By default, we go for the maximum bucket depth. This uses the full hashcode of 10 digits.
        int DEFAULT_BUCKETDEPTH = 10;

        //Perform a save on a delta of 500 by default.
        int DEFAULT_SAVEDELTA = 500;

        // 1 week.
        long DEFAULT_EXPIRETIMEINMILISECONDS = 604800;

        String DEFAULT_ROOTPATH = "/var/acs-commons/httpcache";
        String DEFAULT_CRON_EXPRESSION = "0 0 12 1/1 * ? *";

        @AttributeDefinition(
                name = "Cache clean-up schedule",
                description = "[every minute = 0 * * * * ?] Visit www.cronmaker.com to generate cron expressions.",
                defaultValue = DEFAULT_CRON_EXPRESSION
        )
        String scheduler_expression() default DEFAULT_CRON_EXPRESSION;

        @AttributeDefinition(
                name = "Cache bucketing tree depth",
                description = "The depth the bucket tree goes. Minimum value is 1. "
                        + "This value can be used for tweaking performance. "
                        + "The more data cached, the higher this value should be. "
                        + "Downside is that the higher the value, the longer the retrieval of cache entries takes if the buckets are relatively low on entries.",
                defaultValue = "" + DEFAULT_BUCKETDEPTH
        )
        int httpcache_config_jcr_bucketdepth() default DEFAULT_BUCKETDEPTH;


        @AttributeDefinition(
                name = "Cache-root Parent Path location",
                description = "Points to the location of the cache root parent node in the JCR repository",
                defaultValue = DEFAULT_ROOTPATH
        )
        String httpcache_config_jcr_rootpath() default DEFAULT_ROOTPATH;

        @AttributeDefinition(
                name = "Save threshold",
                description = "The threshold to add,remove and modify nodes when handling the cache",
                defaultValue = "" + DEFAULT_SAVEDELTA
        )
        int httpcache_config_jcr_savedelta() default DEFAULT_SAVEDELTA;

        @AttributeDefinition(
                name = "Expire time in miliseconds",
                description = "The time in miliseconds after which nodes will be removed by the scheduled cleanup service. ",
                defaultValue = "" + DEFAULT_EXPIRETIMEINMILISECONDS
        )
        long httpcache_config_jcr_expiretimeinmiliseconds() default DEFAULT_EXPIRETIMEINMILISECONDS;
    }

    private static final String SERVICE_NAME = "httpcache-jcr-storage-service";

    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    private static final Logger log = LoggerFactory.getLogger(JCRHttpCacheStoreImpl.class);

    private Config config;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private DynamicClassLoaderManager dclm;


    private final CopyOnWriteArrayList<CacheKeyFactory> cacheKeyFactories = new CopyOnWriteArrayList<CacheKeyFactory>();

    public JCRHttpCacheStoreImpl() throws NotCompliantMBeanException {
        super(JcrCacheMBean.class);
    }

    @Activate
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public void put(final CacheKey key, final CacheContent content) throws HttpCacheDataStreamException {
        final long currentTime = System.currentTimeMillis();
        incrementLoadCount();

        withSession((Session session) -> {
            final BucketNodeFactory factory = new BucketNodeFactory(session, key, config);
            final Node bucketNode = factory.getBucketNode();

            final Node entryNode = new BucketNodeHandler(bucketNode, dclm).createOrRetrieveEntryNode(key, config.httpcache_config_jcr_expiretimeinmiliseconds());

            long expiryTime = (key.getExpiryForCreation() > 0) ? key.getExpiryForCreation() : config.httpcache_config_jcr_expiretimeinmiliseconds();

            new EntryNodeWriter(session, entryNode, key, content,expiryTime).write();
            session.save();

            incrementLoadSuccessCount();
            incrementTotalLoadTime(System.currentTimeMillis() - currentTime);
        }, (Exception e) -> {
            incrementLoadExceptionCount();
        });
    }

    @Override
    public boolean contains(final CacheKey key) {
        final long currentTime = System.currentTimeMillis();
        incrementRequestCount();

        return withSession((Session session) -> {
            final BucketNodeFactory factory = new BucketNodeFactory(session, key, config);
            final Node bucketNode = factory.getBucketNode();

            if (bucketNode != null) {
                Node entryNode = new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key);
                if (entryNode != null) {
                    incrementTotalLookupTime(System.currentTimeMillis() - currentTime);
                    incrementHitCount();

                    return true;
                }
            }

            incrementTotalLookupTime(System.currentTimeMillis() - currentTime);
            incrementMissCount();

            return false;
        });
    }

    @Override
    public CacheContent getIfPresent(final CacheKey key) {
        final long currentTime = System.currentTimeMillis();
        incrementRequestCount();

        return withSession((Session session) -> {
            final BucketNodeFactory factory = new BucketNodeFactory(session, key, config);
            final Node bucketNode = factory.getBucketNode();

            if (bucketNode != null) {
                final Node entryNode = new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key);
                final CacheContent content = new EntryNodeToCacheContentHandler(entryNode).get();

                if (content != null) {
                    incrementTotalLookupTime(System.currentTimeMillis() - currentTime);
                    incrementHitCount();
                    return content;
                }
            }

            incrementTotalLookupTime(System.currentTimeMillis() - currentTime);
            incrementMissCount();

            return null;
        });
    }

    @Override
    public long size() {
        return withSession((Session session) -> {
            final Node rootNode = session.getNode(config.httpcache_config_jcr_rootpath());
            final AllEntryNodesCountVisitor visitor = new AllEntryNodesCountVisitor(11);
            visitor.visit(rootNode);
            return visitor.getTotalEntryNodeCount();
        });
    }

    @Override
    public void invalidate(final CacheKey key) {
        withSession((Session session) -> {
            final BucketNodeFactory factory = new BucketNodeFactory(session, key, config);
            final Node bucketNode = factory.getBucketNode();

            if (bucketNode != null) {
                final Node entryNode = new BucketNodeHandler(bucketNode, dclm).getEntryIfExists(key);
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
            final InvalidateByCacheConfigVisitor visitor = new InvalidateByCacheConfigVisitor(11, config.httpcache_config_jcr_savedelta(), cacheConfig, dclm);
            final Node rootNode = session.getNode(config.httpcache_config_jcr_rootpath());
            visitor.visit(rootNode);
            visitor.close();
            incrementEvictionCount(visitor.getEvictionCount());
        });
    }

    @Override
    public void invalidateAll() {
        withSession((Session session) -> {
            final Node rootNode = session.getNode(config.httpcache_config_jcr_rootpath());
            final InvalidateAllNodesVisitor visitor = new InvalidateAllNodesVisitor(11, config.httpcache_config_jcr_savedelta());
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
            final Node rootNode = session.getNode(config.httpcache_config_jcr_rootpath());
            final ExpiredNodesVisitor visitor = new ExpiredNodesVisitor(11, config.httpcache_config_jcr_savedelta());
            visitor.visit(rootNode);
            visitor.close();
            incrementEvictionCount(visitor.getEvictionCount());
        });
    }

    @Override
    public long getTtl() {
        return config.httpcache_config_jcr_expiretimeinmiliseconds();
    }

    @Override
    public void clearCache() {
        invalidateAll();
    }

    @Override
    public String getCacheEntry(final String cacheKeyStr) throws CacheMBeanException {
        return withSession((Session session) -> {
            EntryNodeByStringKeyVisitor visitor = new EntryNodeByStringKeyVisitor(11, dclm, cacheKeyStr);
            final Node rootNode = session.getNode(config.httpcache_config_jcr_rootpath());
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
            final Node rootNode = session.getNode(config.httpcache_config_jcr_rootpath());
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
