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
package com.adobe.acs.commons.wcm.vanity.impl;

import com.adobe.acs.commons.wcm.vanity.VanityURLService;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("checkstyle:abbreviationaswordinname")
@RunWith(MockitoJUnitRunner.class)
public class VanityURLServiceImplTest {

    @Rule
    public final AemContext ctx = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Mock
    RequestDispatcher requestDispatcher;

    VanityURLServiceImpl vanityURLService;

    @Before
    public void setUp() throws Exception {
        ctx.request().setRequestDispatcherFactory(new MockRequestDispatcherFactory() {
            @Override
            public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
                return requestDispatcher;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
                return requestDispatcher;
            }
        });

        ctx.registerInjectActivateService(new VanityURLServiceImpl());

        vanityURLService = (VanityURLServiceImpl) ctx.getService(VanityURLService.class);
    }

    @Test
    public void nonExistingResourceBehavior() throws Exception {
        Resource nonExisting = ctx.resourceResolver().resolve("/dev/null");
        assertEquals("/dev/null", nonExisting.getPath());
    }

    @Test
    public void dispatch_NoMapping() throws Exception {
        ctx.request().setServletPath("/my-vanity");

        assertFalse(vanityURLService.dispatch(ctx.request(), ctx.response()));
        verify(requestDispatcher, times(0)).forward(any(ExtensionlessRequestWrapper.class), eq(ctx.response()));
    }

    @Test
    public void dispatch_Loop() throws Exception {
        ctx.request().setAttribute("acs-aem-commons__vanity-check-loop-detection", true);

        assertFalse(vanityURLService.dispatch(ctx.request(), ctx.response()));
        verify(requestDispatcher, times(0)).forward(any(ExtensionlessRequestWrapper.class), eq(ctx.response()));
    }

    @Test
    public void getPathScope() throws Exception {
        assertEquals("/content/us/en", vanityURLService.getPathScope("/content/us/en/my-vanity", "/my-vanity"));
        assertEquals("/content/us/en", vanityURLService.getPathScope("/content/us/en/my-vanity/path", "https://test.com:443/my-vanity/path"));
        assertEquals("/content/us/en/my-vanity", vanityURLService.getPathScope("/content/us/en/my-vanity", "::::"));
    }

    @Test
    public void isVanityPath() throws Exception {
        ctx.build().resource("/foo",
                "jcr:primaryType", "sling:redirect",
                "sling:target", "/content/bar");

        assertTrue(vanityURLService.isVanityPath("/content", "/foo", ctx.request()));
    }

    @Test
    public void isVanityPath_OutsideOfPathScope() throws Exception {
        ctx.build().resource("/foo",
                "jcr:primaryType", "sling:redirect",
                "sling:target", "/bar");

        assertFalse(vanityURLService.isVanityPath("/content", "/foo", ctx.request()));
    }

    @Test
    public void isVanityPath_NotRedirectResource() throws Exception {
        // Redirect resources
        ctx.build().resource("/foo",
                "jcr:primaryType", "nt:unstructured");

        assertFalse(vanityURLService.isVanityPath("/content", "/foo", ctx.request()));
    }
}