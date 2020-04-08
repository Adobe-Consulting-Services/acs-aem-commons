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

import com.adobe.acs.commons.util.ResourceDataUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * ErrorPageCacheImpl test using PowerMockRunner.
 *
 * Split out as ErrorPageCacheImplTest was having problems with @Spy'ied vars
 */

// See https://stackoverflow.com/questions/52966897/powermock-java-11 for details about the 
// @PowerMockIgnore statement

@RunWith(PowerMockRunner.class)
@PrepareForTest(ResourceDataUtil.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class PowerMockErrorPageCacheImplTest {

    private ErrorPageCacheImpl errorPageCache;

    @Before
    public void setUp() throws Exception {
        errorPageCache = new ErrorPageCacheImpl(1, false);
    }

    @Test
    public void testGet() throws Exception {
        mockStatic(ResourceDataUtil.class);

        String data = "";

        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        when(ResourceDataUtil.getIncludeAsString("/content/world", request,
                response)).thenReturn("hello world");

        assertEquals(0, errorPageCache.getTotalCacheRequests());

        // MISS
        data = errorPageCache.get("/content/world", request, response);

        assertEquals(0, errorPageCache.getTotalHits());
        assertEquals(1, errorPageCache.getTotalMisses());
        assertEquals(1, errorPageCache.getTotalCacheRequests());
        assertEquals(1, errorPageCache.getCacheEntriesCount());

        assertEquals("hello world", data);

        when(ResourceDataUtil.getIncludeAsString("/content/world", request,
                response)).thenReturn("hello new world");

        // HIT
        data = errorPageCache.get("/content/world", request, response);

        assertEquals(1, errorPageCache.getTotalHits());
        assertEquals(1, errorPageCache.getTotalMisses());
        assertEquals(2, errorPageCache.getTotalCacheRequests());
        assertEquals(1, errorPageCache.getCacheEntriesCount());

        assertEquals("hello world", data);

        // Sleep for > 1 second
        Thread.sleep(1001);

        // MISS
        data = errorPageCache.get("/content/world", request, response);

        assertEquals(1, errorPageCache.getTotalHits());
        assertEquals(2, errorPageCache.getTotalMisses());
        assertEquals(3, errorPageCache.getTotalCacheRequests());
        assertEquals(1, errorPageCache.getCacheEntriesCount());

        assertEquals("hello new world", data);
    }


    @Test
    public void testGet_Null() throws Exception {
        mockStatic(ResourceDataUtil.class);

        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        when(ResourceDataUtil.getIncludeAsString("/content/world", request,
                response)).thenReturn(null);

        String data = errorPageCache.get("/content/world", request, response);

        assertEquals("", data);
    }
}
