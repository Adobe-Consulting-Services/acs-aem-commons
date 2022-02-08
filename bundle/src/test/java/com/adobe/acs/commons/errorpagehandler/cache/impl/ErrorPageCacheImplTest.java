/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.errorpagehandler.cache.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import junitx.util.PrivateAccessor;

@RunWith(MockitoJUnitRunner.class)
public class ErrorPageCacheImplTest {
    private static final int FAR_FUTURE_EXPIRY = Integer.MAX_VALUE;

    @Spy
    private ConcurrentHashMap<String, CacheEntry> cache;

    private ErrorPageCacheImpl errorPageCache;
    private Supplier<String> includedStringSupplier;

    private static final int NUM_INITIAL_HITS = 5;
    private static final int NUM_INITIAL_MISSES = 3;
    private static final int NUM_INITIAL_CACHE_ENTRIES = 2;
    private static final int NUM_INITIAL_REQUESTS = NUM_INITIAL_HITS + NUM_INITIAL_MISSES;

    @Before
    public void setUp() throws Exception {
        errorPageCache = new ErrorPageCacheImpl(1, false) {

            @Override
            public String getIncludeAsString(String path, SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse) {
                return includedStringSupplier.get();
            }
        };
        PrivateAccessor.setField(errorPageCache, "cache", cache);

        // 1 Miss
        // 2 Hits
        CacheEntry earth = new CacheEntry();
        earth.setData("hello earth");
        earth.incrementMisses();
        earth.incrementHits();
        earth.incrementHits();
        earth.setExpiresIn(FAR_FUTURE_EXPIRY);

        cache.put("/content/earth", earth);

        // 2 Misses
        // 3 Hits
        CacheEntry mars = new CacheEntry();
        mars.setData("hello mars");
        mars.incrementMisses();
        mars.incrementHits();
        mars.incrementMisses();
        mars.incrementHits();
        mars.incrementHits();
        mars.setExpiresIn(FAR_FUTURE_EXPIRY);

        cache.put("/content/mars", mars);

    }

    @Test
    public void testGet() throws Exception {
        String data = "";

        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        includedStringSupplier = () -> "hello world";

       
        assertEquals(NUM_INITIAL_REQUESTS, errorPageCache.getTotalCacheRequests());

        // MISS
        data = errorPageCache.get("/content/world", request, response);

        assertEquals(NUM_INITIAL_HITS, errorPageCache.getTotalHits());
        assertEquals(NUM_INITIAL_MISSES + 1, errorPageCache.getTotalMisses());
        assertEquals(NUM_INITIAL_REQUESTS + 1, errorPageCache.getTotalCacheRequests());
        assertEquals(NUM_INITIAL_CACHE_ENTRIES + 1, errorPageCache.getCacheEntriesCount());

        assertEquals("hello world", data);

        includedStringSupplier = () -> "hello new world";

        // HIT
        data = errorPageCache.get("/content/world", request, response);

        assertEquals(NUM_INITIAL_HITS + 1, errorPageCache.getTotalHits());
        assertEquals(NUM_INITIAL_MISSES + 1, errorPageCache.getTotalMisses());
        assertEquals(NUM_INITIAL_REQUESTS + 2, errorPageCache.getTotalCacheRequests());
        assertEquals(NUM_INITIAL_CACHE_ENTRIES + 1, errorPageCache.getCacheEntriesCount());

        assertEquals("hello world", data);

        // Sleep for > 1 second (cache ttl)
        Thread.sleep(1001);

        // MISS
        data = errorPageCache.get("/content/world", request, response);

        assertEquals(NUM_INITIAL_HITS + 1, errorPageCache.getTotalHits());
        assertEquals(NUM_INITIAL_MISSES + 2, errorPageCache.getTotalMisses());
        assertEquals(NUM_INITIAL_REQUESTS + 3, errorPageCache.getTotalCacheRequests());
        assertEquals(NUM_INITIAL_CACHE_ENTRIES + 1, errorPageCache.getCacheEntriesCount());

        assertEquals("hello new world", data);
    }

    @Test
    public void testGet_Null() throws Exception {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        includedStringSupplier = () -> null;

        String data = errorPageCache.get("/content/world", request, response);

        assertEquals("", data);
    }

    @Test
    public void testGetTotalHits() throws Exception {
        assertEquals(NUM_INITIAL_HITS, errorPageCache.getTotalHits());
    }

    @Test
    public void testGetCacheEntriesCount() throws Exception {
        assertEquals(NUM_INITIAL_CACHE_ENTRIES,  errorPageCache.getCacheEntriesCount());
    }

    @Test
    public void testGetTotalMisses() throws Exception {
        assertEquals(NUM_INITIAL_MISSES, errorPageCache.getTotalMisses());
    }

    @Test
    public void testGetTotalCacheRequests() throws Exception {
        assertEquals(NUM_INITIAL_REQUESTS, errorPageCache.getTotalCacheRequests());
    }

    @Test
    public void testGetCacheSizeInKb() throws Exception {
        final long expResult = ("hello earth".getBytes().length
                + "hello mars".getBytes().length) / 1000L;

        final long result = errorPageCache.getCacheSizeInKB();

        assertEquals(expResult, result, 0.000001);
    }

    @Test
    public void testGetCacheEntries() throws Exception {
        // MBean formatting; Skip test
    }

    @Test
    public void testClearCache() throws Exception {
        final int expResult = 0;

        errorPageCache.clearCache();

        final int result = cache.size();

        assertEquals(expResult, result);
    }

    @Test
    public void testGetCacheData_earth() throws Exception {
        final String expResult = "hello earth";

        final String result = errorPageCache.getCacheData("/content/earth");

        assertEquals(expResult, result);
    }

    @Test
    public void testGetCacheData_mars() throws Exception {
        final String expResult = "hello mars";

        final String result = errorPageCache.getCacheData("/content/mars");

        assertEquals(expResult, result);
    }
}
