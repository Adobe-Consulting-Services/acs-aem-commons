package com.adobe.acs.commons.wcm.vanity.impl;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionlessRequestWrapperTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    public MockSlingHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext());

        context.build().resource("/content")
                .resource("null-extension","sling:status", "302");

        context.build().resource("/content")
                .resource("has-extension");
    }

    @Test
    public void getRequestPathInfo_NullExtension() throws Exception {
        request.setResource(context.resourceResolver().getResource("/content/null-extension"));

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo)request.getRequestPathInfo();
        requestPathInfo.setExtension("xyz");

        ExtensionlessRequestWrapper wrapper = new ExtensionlessRequestWrapper(request);
        assertNull(wrapper.getRequestPathInfo().getExtension());
    }


    @Test
    public void getRequestPathInfo_HasExtension() throws Exception {
        request.setResource(context.resourceResolver().getResource("/content/has-extension"));

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo)request.getRequestPathInfo();
        requestPathInfo.setExtension("xyz");

        ExtensionlessRequestWrapper wrapper = new ExtensionlessRequestWrapper(request);
        assertEquals("xyz", wrapper.getRequestPathInfo().getExtension());
    }
}