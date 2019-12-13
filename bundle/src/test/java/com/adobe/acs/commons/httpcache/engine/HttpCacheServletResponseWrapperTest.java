/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.httpcache.engine;

import com.adobe.acs.commons.httpcache.store.TempSink;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;
import com.day.cq.commons.feed.StringResponseWrapper;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpCacheServletResponseWrapperTest {

    @Spy
    SlingHttpServletResponse response = new MockSlingHttpServletResponse();

    @Before
    public void init(){
        response.setCharacterEncoding("utf-8");
    }

    @Test
    public void getHeaderNames_NullHeaderNames() throws IOException {
        TempSink tempSink = new MemTempSinkImpl();
        when(response.getHeaderNames()).thenThrow(AbstractMethodError.class);

        HttpCacheServletResponseWrapper systemUnderTest = new HttpCacheServletResponseWrapper(response, tempSink);

        assertEquals(0, systemUnderTest.getHeaderNames().size());
    }

    @Test
    public void test_printwriter() throws IOException {
        TempSink tempSink = new MemTempSinkImpl();

        HttpCacheServletResponseWrapper systemUnderTest = new HttpCacheServletResponseWrapper(response, tempSink);
        PrintWriter writer = systemUnderTest.getWriter();
        assertNotNull(writer);
        assertEquals(HttpCacheServletResponseWrapper.ResponseWriteMethod.PRINTWRITER, systemUnderTest.getWriteMethod());

    }

    @Test(expected = IllegalStateException.class)
    public void test_printwriter_exception() throws IOException {
        TempSink tempSink = new MemTempSinkImpl();

        HttpCacheServletResponseWrapper systemUnderTest = new HttpCacheServletResponseWrapper(response, tempSink);
        systemUnderTest.getWriter();
        systemUnderTest.getOutputStream();

    }

    @Test
    public void test_outputstream() throws IOException {
        TempSink tempSink = new MemTempSinkImpl();

        HttpCacheServletResponseWrapper systemUnderTest = new HttpCacheServletResponseWrapper(new StringResponseWrapper(response), tempSink);
        OutputStream outputStream = systemUnderTest.getOutputStream();
        assertNotNull(outputStream);
        assertEquals(HttpCacheServletResponseWrapper.ResponseWriteMethod.OUTPUTSTREAM, systemUnderTest.getWriteMethod());

    }

    @Test(expected = IllegalStateException.class)
    public void test_outputstream_exception() throws IOException {
        TempSink tempSink = new MemTempSinkImpl();

        HttpCacheServletResponseWrapper systemUnderTest = new HttpCacheServletResponseWrapper(new StringResponseWrapper(response), tempSink);
        systemUnderTest.getOutputStream();
        systemUnderTest.getWriter();

    }
}
