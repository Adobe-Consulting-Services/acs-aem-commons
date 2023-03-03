/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.marketo.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.acs.commons.marketo.client.MarketoApiException;
import com.adobe.acs.commons.marketo.client.MarketoClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMarketoConnectionServletTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private MarketoClient client;
    private TestMarketoConnectionServlet servlet;
    private CloseableHttpClient httpClient;

    @Before
    public void before() {
        client = mock(MarketoClient.class);
        httpClient = mock(CloseableHttpClient.class);
        when(client.getHttpClient()).thenReturn(httpClient);
        context.registerService(MarketoClient.class, client);
        servlet = context.registerInjectActivateService(new TestMarketoConnectionServlet());

        context.addModelsForClasses(MarketoClientConfigurationImpl.class);
    }

    private String getResponseTitle(MockSlingHttpServletResponse response) throws IOException {
        Map<String, Object> body = new ObjectMapper().readValue(context.response().getOutputAsString(),
                new TypeReference<Map<String, Object>>() {
                });
        return (String) body.get("title");
    }

    private void createConfig() {

        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        properties.put("clientId", "123");
        properties.put("clientSecret", "456");
        properties.put("endpointHost", "123.mktorest.com");
        properties.put("munchkinId", "123");
        properties.put("serverInstance", "123.marketo.com");

        context.create().resource("/conf/marketo/jcr:content", properties);
    }

    @Test
    public void requiresPath() throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());

        assertEquals("Missing Config Path", getResponseTitle(context.response()));
    }

    @Test
    public void pathMustExist() throws ServletException, IOException {
        context.request().addRequestParameter("path", "/conf/marketo");
        servlet.doGet(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());

        assertEquals("Configuration Not Found", getResponseTitle(context.response()));
    }

    @Test
    public void pathMustContainConfiguration() throws ServletException, IOException {
        context.create().resource("/conf/marketo/jcr:content");
        context.request().addRequestParameter("path", "/conf/marketo");
        servlet.doGet(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());

        assertEquals("Invalid Configuration", getResponseTitle(context.response()));
    }

    @Test
    public void mustGetAccessToken() throws ServletException, IOException {
        createConfig();

        when(client.getApiToken(any())).thenThrow(mock(MarketoApiException.class));

        context.request().addRequestParameter("path", "/conf/marketo");
        servlet.doGet(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());

        assertEquals("Unable to Retrieve API Token", getResponseTitle(context.response()));
    }

    @Test
    public void mustGetForms() throws ServletException, IOException {
        createConfig();

        when(client.getApiToken(any())).thenReturn("TOKEN");
        when(client.getForms(any())).thenThrow(mock(MarketoApiException.class));

        context.request().addRequestParameter("path", "/conf/marketo");
        servlet.doGet(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());

        assertEquals("Unable to Retrieve Forms", getResponseTitle(context.response()));
    }

    @Test
    public void mustGetJavaScriptResponse() throws ServletException, IOException {
        createConfig();

        when(client.getApiToken(any())).thenReturn("TOKEN");
        when(client.getForms(any())).thenReturn(Collections.emptyList());

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine sl = mock(StatusLine.class);
        when(sl.getStatusCode()).thenReturn(404);
        when(response.getStatusLine()).thenReturn(sl);
        when(response.getEntity()).thenReturn(new StringEntity(""));
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);

        context.request().addRequestParameter("path", "/conf/marketo");
        servlet.doGet(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());

        assertEquals("Invalid Script Response", getResponseTitle(context.response()));
    }

    @Test
    public void canRunSuccessfully() throws ServletException, IOException {
        createConfig();

        when(client.getApiToken(any())).thenReturn("TOKEN");
        when(client.getForms(any())).thenReturn(Collections.emptyList());

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine sl = mock(StatusLine.class);
        when(sl.getStatusCode()).thenReturn(200);

        Header ct = mock(Header.class);
        when(ct.getValue()).thenReturn("application/json");
        when(response.getHeaders(anyString())).thenReturn(new Header[] { ct });
        when(response.getStatusLine()).thenReturn(sl);
        when(response.getEntity()).thenReturn(new StringEntity(""));
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);

        context.request().addRequestParameter("path", "/conf/marketo");
        servlet.doGet(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());

        assertEquals("Invalid Script Response", getResponseTitle(context.response()));
    }

}
