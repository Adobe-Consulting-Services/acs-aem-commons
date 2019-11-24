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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ConcurrentHashMap;

import junitx.util.PrivateAccessor;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ErrorPageCacheImplTest {
    private static final int FAR_FUTURE_EXPIRY = Integer.MAX_VALUE;

    @Spy
    private ConcurrentHashMap<String, CacheEntry> cache;

    private ErrorPageCacheImpl errorPageCache;

    @Before
    public void setUp() throws Exception {
        errorPageCache = new ErrorPageCacheImpl(5, false);
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

        MockitoAnnotations.initMocks(this);
    }

    public void testGet() throws Exception {
        /**
         * Implemented in PowerMockErrorPageCacheImplTest
         *
         * Powermock was having problems running with @Spy'ed vars in this Test.
         */
    }

    @Test
    public void testGetTotalHits() throws Exception {
        final int expResult = 5;

        final int result = errorPageCache.getTotalHits();

        assertEquals(expResult, result);
    }

    @Test
    public void testGetCacheEntriesCount() throws Exception {
        final int expResult = 2;

        final int result = errorPageCache.getCacheEntriesCount();

        assertEquals(expResult, result);
    }

    @Test
    public void testGetTotalMisses() throws Exception {
        final int expResult = 3;

        final int result = errorPageCache.getTotalMisses();

        assertEquals(expResult, result);
    }

    @Test
    public void testGetTotalCacheRequests() throws Exception {
        final int expResult = 8;

        final int result = errorPageCache.getTotalCacheRequests();

        assertEquals(expResult, result);
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
