package com.adobe.acs.commons.wcm.vanity.impl;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.RequestDispatcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VanityURLServiceImplTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    public MockSlingHttpServletRequest request;
    public MockSlingHttpServletResponse response;

    @Mock
    RequestDispatcher requestDispatcher;

    @InjectMocks
    VanityURLServiceImpl vanityURLService = new VanityURLServiceImpl();


    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext());
        response = new MockSlingHttpServletResponse();

        context.build().resource("/content")
                .resource("sample")
                .resource("vanity", "sling:vanityPath", "/my-vanity");
    }

    @Test
    public void dispatch_NoMapping() throws Exception {
        request.setServletPath("/my-vanity");

        assertFalse(vanityURLService.dispatch(request, response));
        verify(requestDispatcher, times(0)).forward(any(ExtensionlessRequestWrapper.class), eq(response));
    }

    @Test
    public void dispatch_Loop() throws Exception {
        request.setAttribute("acs-aem-commons__vanity-check-loop-detection", true);

        assertFalse(vanityURLService.dispatch(request, response));
        verify(requestDispatcher, times(0)).forward(any(ExtensionlessRequestWrapper.class), eq(response));
    }

    @Test
    public void isVanityPath() throws Exception {
        context.build().resource("/foo",
                "jcr:primaryType", "sling:redirect",
                            "sling:target", "/content/bar");

        assertTrue(vanityURLService.isVanityPath("/content", "/foo", request));
    }

    @Test
    public void isVanityPath_OutsideOfPathScope() throws Exception {
        context.build().resource("/foo",
                "jcr:primaryType", "sling:redirect",
                "sling:target", "/bar");

        assertFalse(vanityURLService.isVanityPath("/content", "/foo", request));
    }

    @Test
    public void isVanityPath_NotRedirectResource() throws Exception {
        // Redirect resources
        context.build().resource("/foo",
                "jcr:primaryType", "nt:unstructured");

        assertFalse(vanityURLService.isVanityPath("/content", "/foo", request));
    }
}