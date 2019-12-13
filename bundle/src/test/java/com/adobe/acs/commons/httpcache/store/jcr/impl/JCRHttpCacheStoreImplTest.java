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

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class JCRHttpCacheStoreImplTest {

    private static final String INPUT = "SomeSillyTextForTesting";

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    JCRHttpCacheStoreImpl store;
    Map<String, Object> config;
    Map<String, List<String>> cacheContentHeaders = new HashMap<>();

    // default
    Instant currentInstant = Clock.systemUTC().instant();

    @Before
    public void setup() throws NotCompliantMBeanException {
        store = new JCRHttpCacheStoreImpl();
        config = new HashMap<>();
        config.put(JCRHttpCacheStoreImpl.PN_ROOTPATH, "/cache");
        config.put(JCRHttpCacheStoreImpl.PN_BUCKETDEPTH, 2);
        config.put(JCRHttpCacheStoreImpl.PN_EXPIRETIMEINSECONDS, 10);
        config.put(JCRHttpCacheStoreImpl.PN_SAVEDELTA, 10);
        setTime(currentInstant);

        // prepare repo
        context.build().resource("/cache/root", new HashMap<>()).commit();

    }

    private void setTime(Instant instant) {
        store.clock = Clock.fixed(instant, ZoneId.systemDefault());
    }

    @Test
    public void simplePutAndDelete() throws HttpCacheDataStreamException, CacheMBeanException {
        context.registerInjectActivateService(store, config);
        CacheKey key1 = new CacheKeyMock("http://localhost/content/geometrixx/en.html", "/content/geometrixx/en", 1234,
                "example");
        CacheContent content1 = new CacheContent("UTF-8", "text/html", cacheContentHeaders,
                new ByteArrayInputStream(INPUT.getBytes()));
        store.put(key1, content1);
        assertTrue(store.contains(key1));
        assertEquals(1, store.size());
        assertEquals("text/html", store.getIfPresent(key1).getContentType());
        assertEquals(INPUT, store.getCacheEntry("example"));

        store.invalidate(key1);
        assertEquals(0, store.size());
        assertNull(store.getIfPresent(key1));
        assertFalse(store.contains(key1));
    }

    @Test
    public void expirationTest() throws HttpCacheDataStreamException {
        context.registerInjectActivateService(store, config);
        CacheKey key1 = new CacheKeyMock("http://localhost/content/geometrixx/en.html", "/content/geometrixx/en", 1234,
                "example");
        CacheContent content1 = new CacheContent("UTF-8", "text/html", cacheContentHeaders,
                new ByteArrayInputStream(INPUT.getBytes()));
        store.put(key1, content1);
        assertTrue(store.contains(key1));

        // fast forward : 12 seconds
        setTime(currentInstant.plus(12, ChronoUnit.SECONDS));

        // the entry is expired, but not yet purged
        assertFalse(store.contains(key1));
        assertNull(store.getIfPresent(key1));
        assertEquals(1, store.getCacheEntriesCount());

        // purge
        store.purgeExpiredEntries();
        assertFalse(store.contains(key1));
        assertEquals(0, store.getCacheEntriesCount());
    }

    @Test
    public void complexExpiration() throws HttpCacheDataStreamException {
        context.registerInjectActivateService(store, config);
        CacheKey key1 = new CacheKeyMock("http://localhost/content/geometrixx/en.html", "/content/geometrixx/en", 1234,
                "example");
        CacheContent content1 = new CacheContent("UTF-8", "text/html", cacheContentHeaders,
                new ByteArrayInputStream(INPUT.getBytes()));
        store.put(key1, content1);
        assertTrue(store.contains(key1));

        // fast forward to +8 seconds
        setTime(currentInstant.plus(8, ChronoUnit.SECONDS));
        for (int i = 0; i < 10; i++) {
            CacheKey key = new CacheKeyMock("http://localhost/content/geometrixx/en.html", "/content/geometrixx/en",
                    1234, "example" + i);
            store.put(key, content1);
        }
        assertEquals(11, store.getCacheEntriesCount());

        // fast forward to +11 seconds
        setTime(currentInstant.plus(11, ChronoUnit.SECONDS));
        store.purgeExpiredEntries();
        // the first entry should have expired
        assertEquals(10, store.getCacheEntriesCount());

        store.invalidateAll();
        assertEquals(0, store.getCacheEntriesCount());

    }

}
