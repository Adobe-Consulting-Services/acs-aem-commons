/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.etag.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.util.BufferedSlingHttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class EtagMessageDigestServletFilterTest {

    @Mock
    EtagMessageDigestServletFilter.Config configuration;

    @Mock
    SlingHttpServletResponse mockResponse;
    BufferedSlingHttpServletResponse bufferedResponse;
    
    EtagMessageDigestServletFilter filter;

    private static final String EXAMPLE_TEXT = "The quick brown fox jumps over the lazy dog";

    @Before
    public void setUp() {
        Mockito.when(configuration.enabled()).thenReturn(true);
        Mockito.when(configuration.messageDigestAlgorithm()).thenReturn("MD5");
        filter = new EtagMessageDigestServletFilter();
        filter.activate(configuration);
        bufferedResponse = new BufferedSlingHttpServletResponse(mockResponse);
    }

    @Test
    public void testCalculateFromResponseWithEmptyString() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // the MD5 is taken from https://en.wikipedia.org/wiki/MD5
        Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", filter.calculateDigestFromResponse(bufferedResponse));
    }

    @Test
    public void testCalculateFromResponseWithEmptyStringAndSalt() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Mockito.when(configuration.salt()).thenReturn("some-salt");
        // the MD5 is taken from https://en.wikipedia.org/wiki/MD5
        Assert.assertNotEquals("d41d8cd98f00b204e9800998ecf8427e", filter.calculateDigestFromResponse(bufferedResponse));
    }

    @Test
    public void testCalculateFromResponseWithSimpleContentAndNoHeaders() throws NoSuchAlgorithmException, IOException {
        bufferedResponse.getWriter().write(EXAMPLE_TEXT);
        // the MD5 is taken from https://en.wikipedia.org/wiki/MD5
        Assert.assertEquals("9e107d9d372bb6826bd81d3542a419d6", filter.calculateDigestFromResponse(bufferedResponse));
    }

    @Test
    public void testCalculateFromResponseConsidersHeaders() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Mockito.when(configuration.considerResponseHeaders()).thenReturn(true);
        
        Mockito.when(bufferedResponse.getHeaderNames()).thenReturn(Collections.singletonList("header1"));
        Mockito.when(bufferedResponse.getHeaders(Mockito.eq("header1"))).thenReturn(Arrays.asList("value1", "value2"));
        String md5WithSomeHeaders = filter.calculateDigestFromResponse(bufferedResponse);
        String md5WithSameHeaders = filter.calculateDigestFromResponse(bufferedResponse);
        Assert.assertEquals(md5WithSomeHeaders, md5WithSameHeaders);
        Mockito.when(bufferedResponse.getHeaders(Mockito.eq("header1"))).thenReturn(Arrays.asList("value1", "value3"));
        String md5WithOtherHeaders = filter.calculateDigestFromResponse(bufferedResponse);
        Assert.assertNotEquals(md5WithSomeHeaders, md5WithOtherHeaders);
    }
}
