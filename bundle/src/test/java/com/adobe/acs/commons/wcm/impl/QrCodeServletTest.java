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
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

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

    String RESOURCE_PATH = "/etc/acs-commons/qr-code/_jcr_content/config";

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