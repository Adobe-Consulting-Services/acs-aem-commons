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

import javax.management.NotCompliantMBeanException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * ErrorPageCacheImpl test using PowerMockRunner.
 *
 * Split out as ErrorPageCacheImplTest was having problems with @Spy'ied vars
 */
@RunWith(MockitoJUnitRunner.class)
public final class PowerMockErrorPageCacheImplTest {

    private final SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    private final SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    private final ErrorPageCacheImpl errorPageCache;

    public PowerMockErrorPageCacheImplTest() throws NotCompliantMBeanException {
        errorPageCache = Mockito.spy(new ErrorPageCacheImpl(1, false));
    }

    private void assertData(final String expected) {
        final String data = errorPageCache.get("/content/world", request, response);
        assertEquals(expected, data);
    }

    private void getIncludeAsString(final String toBeReturned) {
        Mockito.doReturn(toBeReturned).when(errorPageCache)
            .getIncludeAsString("/content/world", request, response);
    }

    @Test
    public void testGet() throws InterruptedException {
    	getIncludeAsString("hello world");

        assertEquals(0, errorPageCache.getTotalCacheRequests());

        // MISS
        assertData("hello world");

        assertEquals(0, errorPageCache.getTotalHits());
        assertEquals(1, errorPageCache.getTotalMisses());
        assertEquals(1, errorPageCache.getTotalCacheRequests());
        assertEquals(1, errorPageCache.getCacheEntriesCount());

        getIncludeAsString("hello new world");

        // HIT
        assertData("hello world");

        assertEquals(1, errorPageCache.getTotalHits());
        assertEquals(1, errorPageCache.getTotalMisses());
        assertEquals(2, errorPageCache.getTotalCacheRequests());
        assertEquals(1, errorPageCache.getCacheEntriesCount());

        // Sleep for > 1 second
        Thread.sleep(1001);

        // MISS
        assertData("hello new world");

        assertEquals(1, errorPageCache.getTotalHits());
        assertEquals(2, errorPageCache.getTotalMisses());
        assertEquals(3, errorPageCache.getTotalCacheRequests());
        assertEquals(1, errorPageCache.getCacheEntriesCount());
    }


    @Test
    public void testGet_Null() {
    	getIncludeAsString(null);
        assertData("");
    }
}
