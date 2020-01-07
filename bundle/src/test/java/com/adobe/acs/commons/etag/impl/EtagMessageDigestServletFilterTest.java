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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.servlets.HttpConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.util.BufferedSlingHttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class EtagMessageDigestServletFilterTest {

    @Mock
    EtagMessageDigestServletFilter.Config configuration;

    @Mock
    SlingHttpServletResponse mockResponse;
    BufferedSlingHttpServletResponse bufferedResponse;

    @Mock
    SlingHttpServletRequest mockRequest;

    @Mock
    RequestProgressTracker tracker; 
    EtagMessageDigestServletFilter filter;

    private static final String EXAMPLE_TEXT = "The quick brown fox jumps over the lazy dog";

    @Before
    public void setUp() {
        Mockito.when(configuration.messageDigestAlgorithm()).thenReturn("MD5");
        Mockito.when(configuration.ignoredResponseHeaders()).thenReturn(new String[] { "ignoredHeader" });
        filter = new EtagMessageDigestServletFilter();
        filter.activate(configuration);
        bufferedResponse = new BufferedSlingHttpServletResponse(mockResponse);
        Mockito.when(mockRequest.getRequestProgressTracker()).thenReturn(tracker);
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
    public void testCalculateFromResponseWithOutputStreamAndNoHeaders() throws NoSuchAlgorithmException, IOException {
        bufferedResponse.getOutputStream().print(EXAMPLE_TEXT);
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
        
        Mockito.when(bufferedResponse.getHeaderNames()).thenReturn(Arrays.asList("header1", "ignoredHeader"));
        Mockito.when(bufferedResponse.getHeaders(Mockito.eq("ignoredHeader"))).thenReturn(Arrays.asList("somevalue"));
        String md5WithSomeIgnoredHeaders = filter.calculateDigestFromResponse(bufferedResponse);
        Assert.assertEquals(md5WithSomeHeaders, md5WithSomeIgnoredHeaders);
        
        Mockito.when(bufferedResponse.getHeaders(Mockito.eq("header1"))).thenReturn(Arrays.asList("value1", "value3"));
        String md5WithOtherHeaders = filter.calculateDigestFromResponse(bufferedResponse);
        Assert.assertNotEquals(md5WithSomeHeaders, md5WithOtherHeaders);
    }

    @Test
    public void testIsUnmodified() {
        Vector<String> ifNoneMatchETags = new Vector<>();
        ifNoneMatchETags.add("W/\"ab\"");
        ifNoneMatchETags.add("\"cd\"");
        Assert.assertTrue(EtagMessageDigestServletFilter.isUnmodified(ifNoneMatchETags.elements(), "cd"));
        // with weak tag it should still match
        ifNoneMatchETags = new Vector<>();
        ifNoneMatchETags.add("W/\"ab\"");
        ifNoneMatchETags.add("W/\"cd\"");
        Assert.assertTrue(EtagMessageDigestServletFilter.isUnmodified(ifNoneMatchETags.elements(), "cd"));
        // without quotes it should not match
        ifNoneMatchETags = new Vector<>();
        ifNoneMatchETags.add("W/\"ab\"");
        ifNoneMatchETags.add("cd");
        Assert.assertFalse(EtagMessageDigestServletFilter.isUnmodified(ifNoneMatchETags.elements(), "cd"));
        // non matching etag
        ifNoneMatchETags = new Vector<>();
        ifNoneMatchETags.add("W/\"ab\"");
        ifNoneMatchETags.add("W/\"cd\"");
        Assert.assertFalse(EtagMessageDigestServletFilter.isUnmodified(ifNoneMatchETags.elements(), "de"));
        // wildcard match
        ifNoneMatchETags = new Vector<>();
        ifNoneMatchETags.add("*");
        Assert.assertTrue(EtagMessageDigestServletFilter.isUnmodified(ifNoneMatchETags.elements(), "sometag"));
        // invalid quotes
        ifNoneMatchETags = new Vector<>();
        ifNoneMatchETags.add("\"ab");
        Assert.assertFalse(EtagMessageDigestServletFilter.isUnmodified(ifNoneMatchETags.elements(), "ab"));
    }

    @Test
    public void testIsUnmodifiedWithNullParameter() {
        Assert.assertFalse(EtagMessageDigestServletFilter.isUnmodified(null, "ab"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDoFilter() throws IOException, ServletException {
        Mockito.when(configuration.addAsHtmlComment()).thenReturn(true);
        Mockito.when(configuration.enabled()).thenReturn(true);
        StringWriter responseWriter = new StringWriter();
        Mockito.when(mockResponse.getWriter()).thenReturn(new PrintWriter(responseWriter));
        Mockito.when(mockResponse.getContentType()).thenReturn("text/html");
        
        // first test POST
        Mockito.when(mockRequest.getMethod()).thenReturn(HttpConstants.METHOD_POST);
        final FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                response.getWriter().write(EXAMPLE_TEXT);
            }
        };
        filter.doFilter(mockRequest, mockResponse, chain);
        Mockito.verify(mockResponse, Mockito.never()).setHeader(Mockito.eq(HttpConstants.HEADER_ETAG), Mockito.anyString());
        // don't change response code
        Mockito.verify(mockResponse, Mockito.never()).setStatus(Mockito.anyInt());
        
        // then test GET
        Mockito.when(mockRequest.getMethod()).thenReturn(HttpConstants.METHOD_GET);
        responseWriter = new StringWriter();
        Mockito.when(mockResponse.getWriter()).thenReturn(new PrintWriter(responseWriter));
        // write to response
        filter.doFilter(mockRequest, mockResponse, chain);
        Mockito.verify(mockResponse).setHeader(HttpConstants.HEADER_ETAG, "\"9e107d9d372bb6826bd81d3542a419d6\"");
        // don't change response code
        Mockito.verify(mockResponse, Mockito.never()).setStatus(Mockito.anyInt());
        Mockito.verify(mockResponse, Mockito.never()).setStatus(Mockito.anyInt(), Mockito.anyString());
        
        Assert.assertEquals(responseWriter.toString(), String.format("%s%n<!-- ETag: 9e107d9d372bb6826bd81d3542a419d6 -->%n", EXAMPLE_TEXT));
        
        // now request carries if-none-match header
        Vector<String> ifNoneMatchETags = new Vector<>();
        ifNoneMatchETags.add("\"someinvalidone\"");
        ifNoneMatchETags.add("\"9e107d9d372bb6826bd81d3542a419d6\"");
        Mockito.when(mockRequest.getHeaders(HttpHeaders.IF_NONE_MATCH)).thenReturn(ifNoneMatchETags.elements());
        filter.doFilter(mockRequest, mockResponse, chain);
        
        Mockito.verify(mockResponse).setStatus(304);
        
    }
}
