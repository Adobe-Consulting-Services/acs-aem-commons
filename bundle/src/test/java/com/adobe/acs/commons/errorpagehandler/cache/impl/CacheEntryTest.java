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

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CacheEntryTest {

    @Test
    public void testGetData() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        final String expResult = "hello world";
        cacheEntry.setData(expResult);

        final String result = cacheEntry.getData();

        assertEquals(expResult, result);
    }

    @Test
    public void testSetData() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        final String expResult = "hello world";
        cacheEntry.setData(expResult);

        final String result = cacheEntry.getData();

        assertEquals(expResult, result);
    }

    @Test
    public void testSetData_Null() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        final String expResult = "";
        cacheEntry.setData(null);

        final String result = cacheEntry.getData();

        assertEquals(expResult, result);
    }

    @Test
    public void testGetHits() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        final int expResult = 10;

        for(int i = 0; i < expResult; i++) {
            cacheEntry.incrementHits();
        }

        final int result = cacheEntry.getHits();

        assertEquals(expResult, result);
    }

    @Test
    public void testIncrementHits() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        assertEquals(0, cacheEntry.getHits());

        cacheEntry.incrementHits();

        assertEquals(1, cacheEntry.getHits());

        cacheEntry.incrementHits();

        assertEquals(2, cacheEntry.getHits());
    }

    @Test
    public void testGetMisses() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        final int expResult = 10;

        for(int i = 0; i < expResult; i++) {
            cacheEntry.incrementMisses();
        }

        final int result = cacheEntry.getMisses();

        assertEquals(expResult, result);
    }

    @Test
    public void testIncrementMisses() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        assertEquals(0, cacheEntry.getMisses());

        cacheEntry.incrementMisses();

        assertEquals(1, cacheEntry.getMisses());

        cacheEntry.incrementMisses();

        assertEquals(2, cacheEntry.getMisses());
    }

    @Test
    public void testIsExpired() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        assertTrue(cacheEntry.isExpired(new Date()));
    }

    @Test
    public void testSetExpiresIn() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        cacheEntry.setExpiresIn(1000000);

        assertFalse(cacheEntry.isExpired(new Date()));

        cacheEntry.setExpiresIn(-100000);

        assertTrue(cacheEntry.isExpired(new Date()));
    }

    @Test
    public void testGetHitRate() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();
        final int hits = 7;
        final int misses = 3;

        final float expResult = 0.7F;

        for(int i = 0; i < hits; i++) {
            cacheEntry.incrementHits();
        }

        for(int i = 0; i < misses; i++) {
            cacheEntry.incrementMisses();
        }

        final float result = cacheEntry.getHitRate();

        assertEquals(expResult, result, 0.001);
    }

    @Test
    public void testGetMissRate() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();
        final int hits = 7;
        final int misses = 3;

        final float expResult = 0.3F;

        for(int i = 0; i < hits; i++) {
            cacheEntry.incrementHits();
        }

        for(int i = 0; i < misses; i++) {
            cacheEntry.incrementMisses();
        }

        final float result = cacheEntry.getMissRate();

        assertEquals(expResult, result, 0.001);
    }

    @Test
    public void testGetTotal() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();
        final int hits = 7;
        final int misses = 3;

        for(int i = 0; i < hits; i++) {
            cacheEntry.incrementHits();
        }

        for(int i = 0; i < misses; i++) {
            cacheEntry.incrementMisses();
        }

        final int expResult = hits + misses;
        final int result = cacheEntry.getTotal();

        assertEquals(expResult, result);
    }

    @Test
    public void testGetBytes() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        final int expResult = 11;
        cacheEntry.setData("hello world");

        final int result = cacheEntry.getBytes();

        assertEquals(expResult, result);
    }

    @Test
    public void testGetBytes_Null() throws Exception {
        CacheEntry cacheEntry = new CacheEntry();

        final int expResult = 0;
        cacheEntry.setData(null);

        final int result = cacheEntry.getBytes();

        assertEquals(expResult, result);
    }
}
