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
package com.adobe.acs.commons.httpcache.store.jcr.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.jcr.impl.handler.BucketNodeHandler;
import com.adobe.acs.commons.httpcache.store.jcr.impl.writer.BucketNodeFactory;
import com.adobe.acs.commons.httpcache.store.jcr.impl.writer.EntryNodeWriter;

public final class JCRHttpCacheStoreMocks {

    private final JCRHttpCacheStoreImpl store = spy(new JCRHttpCacheStoreImpl());
    private final DynamicClassLoaderManager dclm = mock(DynamicClassLoaderManager.class);
    private final Session session = mock(Session.class);
    private final ResourceResolverFactory resourceResolverFactory = mock(ResourceResolverFactory.class);
    private final ResourceResolver resourceResolver = mock(ResourceResolver.class);

    private final Node rootNode = mock(Node.class);

    private final CacheKey cacheKey = generateCacheKey();
    private final CacheContent cacheContent = mock(CacheContent.class);

    private static final Logger log = mock(Logger.class);
    private final EntryNodeWriter entryNodeWriter = mock(EntryNodeWriter.class);
    private final BucketNodeHandler bucketNodeHandler = mock(BucketNodeHandler.class);
    private final BucketNodeFactory factory = mock(BucketNodeFactory.class);

    public JCRHttpCacheStoreMocks() throws Exception {
        mockLogger();
        mockRepository();
        mockStore();
        mockBucketNodeFactory();
        mockBucketNodeHandler();
        mockEntryNodeWriter();
    }

    private CacheKeyMock generateCacheKey() {
        return new CacheKeyMock(
                "https://adobe-consulting-services.github.io/acs-aem-commons/",
                "some/random/string",
                1234567890,
                "/content/some/resource/path"
        );
    }

    private void mockLogger() {
        Whitebox.setInternalState(JCRHttpCacheStoreImpl.class, "log", log);
    }

    @SuppressWarnings("unchecked")
    private void mockRepository() throws Exception {
        when(resourceResolverFactory.getServiceResourceResolver(any(Map.class))).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(session.getNode(JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH)).thenReturn(rootNode);
        when(session.nodeExists(JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH)).thenReturn(true);
    }

    private void mockStore() throws Exception {
        Whitebox.setInternalState(store, "resourceResolverFactory", resourceResolverFactory);
        Whitebox.setInternalState(store, "dclm", dclm);
        Whitebox.setInternalState(store, "cacheRootPath", JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH);
        Whitebox.setInternalState(store, "cacheRootPath", JCRHttpCacheStoreImpl.DEFAULT_ROOTPATH);
        Whitebox.setInternalState(store, "bucketTreeDepth", JCRHttpCacheStoreImpl.DEFAULT_BUCKETDEPTH);
        Whitebox.setInternalState(store, "deltaSaveThreshold", JCRHttpCacheStoreImpl.DEFAULT_SAVEDELTA);
        Whitebox.setInternalState(store, "expireTimeInSeconds", JCRHttpCacheStoreImpl.DEFAULT_EXPIRETIMEINSECONDS);
    }

    private void mockBucketNodeFactory() throws Exception {
        doReturn(factory).when(store)
                .createBucketNodeFactory(any(Session.class), any(CacheKey.class));
    }

    private void mockBucketNodeHandler() {
        doReturn(bucketNodeHandler).when(store)
                .createBucketNodeHandler(any(Node.class));
    }

    private void mockEntryNodeWriter() {
        doReturn(entryNodeWriter).when(store)
            .createEntryNodeWriter(any(Session.class), any(Node.class), any(CacheKey.class), any(CacheContent.class), any(long.class));
    }

    public JCRHttpCacheStoreImpl getStore() {
        return store;
    }

    public Session getSession() {
        return session;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public CacheKey getCacheKey() {
        return cacheKey;
    }

    public CacheContent getCacheContent() {
        return cacheContent;
    }

    public static Logger getLog() {
        return log;
    }

    public EntryNodeWriter getEntryNodeWriter() {
        return entryNodeWriter;
    }
}
