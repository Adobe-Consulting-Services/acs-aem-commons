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

import java.util.concurrent.ConcurrentHashMap;

import javax.management.NotCompliantMBeanException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import junitx.util.PrivateAccessor;

@RunWith(MockitoJUnitRunner.class)
public final class ErrorPageCacheImplTest {

    private static final int FAR_FUTURE_EXPIRY = Integer.MAX_VALUE;

    private final ErrorPageCacheImpl errorPageCache;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public ErrorPageCacheImplTest() throws NotCompliantMBeanException {
		errorPageCache = new ErrorPageCacheImpl(5, false);
	}

	@Before
    public void setUp() throws NoSuchFieldException {
        PrivateAccessor.setField(errorPageCache, "cache", cache);

        // 1 Miss
        // 2 Hits
        final CacheEntry earth = new CacheEntry();
        earth.setData("hello earth");
        earth.incrementMisses();
        earth.incrementHits();
        earth.incrementHits();
        earth.setExpiresIn(FAR_FUTURE_EXPIRY);

        cache.put("/content/earth", earth);

        // 2 Misses
        // 3 Hits
        final CacheEntry mars = new CacheEntry();
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
    public void testGetTotalHits() {
        final int result = errorPageCache.getTotalHits();
        assertEquals(5, result);
    }

    @Test
    public void testGetCacheEntriesCount() {
        final int result = errorPageCache.getCacheEntriesCount();
        assertEquals(2, result);
    }

    @Test
    public void testGetTotalMisses() {
        final int result = errorPageCache.getTotalMisses();
        assertEquals(3, result);
    }

    @Test
    public void testGetTotalCacheRequests() {
        final int result = errorPageCache.getTotalCacheRequests();
        assertEquals(8, result);
    }

    @Test
    public void testGetCacheSizeInKb() {
        final long expResult = ("hello earth".getBytes().length
                + "hello mars".getBytes().length) / 1000L;

        final long result = errorPageCache.getCacheSizeInKB();

        assertEquals(expResult, result, 0.000001);
    }

    @Test
    public void testClearCache() {
        errorPageCache.clearCache();
        final int result = cache.size();
        assertEquals(0, result);
    }

    @Test
    public void testGetCacheData_earth() {
        final String result = errorPageCache.getCacheData("/content/earth");
        assertEquals("hello earth", result);
    }

    @Test
    public void testGetCacheData_mars() {
        final String result = errorPageCache.getCacheData("/content/mars");
        assertEquals("hello mars", result);
    }
}
