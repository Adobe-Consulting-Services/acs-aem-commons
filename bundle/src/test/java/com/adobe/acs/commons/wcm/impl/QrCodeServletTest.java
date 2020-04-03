/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import com.day.cq.commons.Externalizer;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QrCodeServletTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Mock
    Externalizer externalizer;

    @InjectMocks
    QrCodeServlet qrCodeServlet = new QrCodeServlet();

    MockSlingHttpServletRequest request;
    MockSlingHttpServletResponse response;
    Resource requestResource;

    static final String RESOURCE_PATH = "/etc/acs-commons/qr-code/_jcr_content/config";

    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext());
        response = new MockSlingHttpServletResponse();

        requestResource = context.create().resource(RESOURCE_PATH, ImmutableMap.<String, Object>builder()
                .put("enabled", true)
                .build());
    }

    @Test
    public void doGet() throws Exception {
        request.setResource(requestResource);

        when(externalizer.publishLink(request.getResourceResolver(),
                "/content/externalize-me.html")).thenReturn("http://www.example.com/content/externalize-me.html");

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        requestPathInfo.setSuffix("/content/externalize-me.html");
        //requestPathInfo.setExtension("json");

        qrCodeServlet.doGet(request, response);

        JSONObject actual = new JSONObject(response.getOutputAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        assertEquals(true, actual.getBoolean("enabled"));
        assertEquals("http://www.example.com/content/externalize-me.html", actual.getString("publishURL"));
    }
}