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
package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemCachePersistenceObject;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CaffeineMemHttpCacheStoreImplTest {

    CaffeineMemHttpCacheStoreImpl caffeine;

    private long ttl = 5000;
    private long maxSizeInMb = 100;

    @Before
    public void setUp() throws Exception {
        caffeine = new CaffeineMemHttpCacheStoreImpl();

        final Map<String, Object> config = new HashMap<>();

        config.put("httpcache.cachestore.caffeine.ttl", 60L);
        config.put("httpcache.cachestore.caffeine.maxsize", 10L);

        caffeine.activate(config);
    }

    @Test
    public void test_put() throws HttpCacheDataStreamException, IOException {
        CacheKey key = mock(CacheKey.class);
        CacheContent content = mock(CacheContent.class);
        InputStream inputStream = getClass().getResourceAsStream("cachecontent.html");
        when(content.getInputDataStream()).thenReturn(inputStream);
        caffeine.put(key, content);
        assertTrue("contains entry we just put in", caffeine.contains(key));

        assertEquals(1, caffeine.size());

        CacheContent retrievedContent = caffeine.getIfPresent(key);
        String retrievedContentString = IOUtils.toString(retrievedContent.getInputDataStream(), StandardCharsets.UTF_8);
        String expectedContentString = IOUtils.toString(getClass().getResourceAsStream("cachecontent.html"), StandardCharsets.UTF_8);

        assertEquals(expectedContentString, retrievedContentString);
    }

    @Test
    public void test_remove() throws HttpCacheDataStreamException {
        CacheKey key = mock(CacheKey.class);
        CacheContent content = mock(CacheContent.class);
        InputStream inputStream = getClass().getResourceAsStream("cachecontent.html");
        when(content.getInputDataStream()).thenReturn(inputStream);
        caffeine.put(key, content);

        assertTrue("contains entry we just put in", caffeine.contains(key));

        CacheKey secondKey = mock(CacheKey.class);
        when(key.isInvalidatedBy(secondKey)).thenReturn(true);

        caffeine.invalidate(secondKey);

        assertFalse("doesn't contain entry we just removed", caffeine.contains(key));
    }

    @Test
    public void test_remove_by_cacheconfig() throws HttpCacheDataStreamException, HttpCacheKeyCreationException {
        HttpCacheConfig config = mock(HttpCacheConfig.class);
        CacheKey key = mock(CacheKey.class);
        CacheContent content = mock(CacheContent.class);
        InputStream inputStream = getClass().getResourceAsStream("cachecontent.html");
        when(content.getInputDataStream()).thenReturn(inputStream);
        caffeine.put(key, content);

        when(config.knows(key)).thenReturn(true);
        assertTrue("contains entry we just put in", caffeine.contains(key));

        caffeine.invalidate(config);

        assertFalse("doesn't contain entry we just removed", caffeine.contains(key));
    }

    @Test
    public void test_get_cache_entry_type() throws OpenDataException {
        CompositeType compositeType = caffeine.getCacheEntryType();
        assertEquals(7, compositeType.keySet().size());
    }

    @Test
    public void test_addCacheData() throws HttpCacheDataStreamException, IOException {
        Map<String, Object> data = new HashMap<>();
        MemCachePersistenceObject cacheObject = new MemCachePersistenceObject();
        InputStream inputStream = getClass().getResourceAsStream("cachecontent.html");

        cacheObject.buildForCaching(200, "utf-8", "text/html", Collections.emptyMap(), inputStream, HttpCacheServletResponseWrapper.ResponseWriteMethod.PRINTWRITER);
        caffeine.addCacheData(data, cacheObject);

        int size = getClass().getResourceAsStream("cachecontent.html").available();
        assertEquals(size + " bytes", data.get("Size"));
        assertEquals("utf-8", data.get("Character Encoding"));
        assertEquals("text/html", data.get("Content Type"));
    }

    @Test
    public void test_getCacheStats() throws OpenDataException, HttpCacheDataStreamException {
        HttpCacheConfig config = mock(HttpCacheConfig.class);
        CacheKey key = mock(CacheKey.class);
        CacheContent content = mock(CacheContent.class);
        InputStream inputStream = getClass().getResourceAsStream("cachecontent.html");
        when(content.getInputDataStream()).thenReturn(inputStream);
        caffeine.put(key, content);

        TabularData data = caffeine.getCacheStats();
        assertEquals(12, data.size());
    }
}
