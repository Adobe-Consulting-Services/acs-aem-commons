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
package com.adobe.acs.commons.errorpagehandler.impl;

import com.adobe.acs.commons.errorpagehandler.ErrorPageHandlerService;
import com.adobe.acs.commons.wcm.vanity.VanityURLService;
import com.adobe.acs.commons.wcm.vanity.impl.VanityURLServiceImpl;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ErrorPageHandlerImplTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Mock
    Authenticator authenticator;

    private MockSlingHttpServletRequest request;
    private ResourceResolver resourceResolver;

    @Before
    public void setup() {
        context.load().json(getClass().getResourceAsStream("ErrorPageHandlerImplTest.json"), "/content/project");
        resourceResolver = context.resourceResolver();
        request = context.request();


        context.registerService(VanityURLService.class, new VanityURLServiceImpl());
        context.registerService(Authenticator.class, authenticator);
    }

    /**
     * Test
     * {@link ErrorPageHandlerImpl#findErrorPage(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.resource.Resource)}
     * with a page without {@code jcr:content} node.
     */
    @Test
    public void testFindErrorPage_withoutContent() {
        assertEquals("/content/project/test/error-pages.html", new ErrorPageHandlerImpl().findErrorPage(request,
                resourceResolver.getResource("/content/project/test/page-without-content")));
    }

    /**
     * Test
     * {@link ErrorPageHandlerImpl#findErrorPage(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.resource.Resource)}
     * with a page with direct configuration.
     */
    @Test
    public void testFindErrorPage_withDirectConfig() {
        assertEquals("/content/project/test/error-pages2.html", new ErrorPageHandlerImpl().findErrorPage(request,
                resourceResolver.getResource("/content/project/test/page-with-config")));
    }

    @Test
    public void testFindErrorPage_subResource() {
        assertEquals("/content/project/test/error-pages.html",
                new ErrorPageHandlerImpl().findErrorPage(request, new NonExistingResource(resourceResolver,
                        "/content/project/test/jcr:content/root/non-existing-resource")));
    }

    @Test
    public void testFindErrorPage_nonExistingPage() {
        assertEquals("/content/project/test/error-pages.html", new ErrorPageHandlerImpl().findErrorPage(request,
                new NonExistingResource(resourceResolver, "/content/project/test/non-existing-page")));
    }

    @Test
    public void testFindErrorPage_nonExistingPageSubResource() {
        assertEquals("/content/project/test/error-pages.html",
                new ErrorPageHandlerImpl().findErrorPage(request, new NonExistingResource(resourceResolver,
                        "/content/project/test/non-existing-page/jcr:content/test1/test2")));
    }

    @Test
    public void testFindErrorPage_nonExistingPageWithoutExtension() {
        assertEquals("/content/project/test/error-pages.html", new ErrorPageHandlerImpl().findErrorPage(request,
                new NonExistingResource(resourceResolver, "/content/project/non-existing-page")));
    }

    @Test
    public void testFindErrorPage_JcrContent0() {
        assertEquals("/content/project/test/error-pages.html",
                new ErrorPageHandlerImpl().findErrorPage(request,
                        new NonExistingResource(resourceResolver, "/content/project/jcr:content/non-existing")));
    }

    @Test
    public void testResetRequestAndResponse() {
        context.response().setStatus(200);

        context.request().setAttribute("com.day.cq.widget.HtmlLibraryManager.included", "Some prior clientlibs");
        context.request().setAttribute("com.adobe.granite.ui.clientlibs.HtmlLibraryManager.included",
                "Some prior clientlibs");
        context.request().setAttribute("com.day.cq.wcm.componentcontext", "some prior component context");

        new ErrorPageHandlerImpl().resetRequestAndResponse(context.request(), context.response(), 500);

        assertEquals("true", context.response().getHeader("x-aem-error-pass"));
        assertEquals(500, context.response().getStatus());
        assertEquals(0,
                ((HashSet<String>) context.request().getAttribute("com.day.cq.widget.HtmlLibraryManager.included"))
                        .size());
        assertEquals(0, ((HashSet<String>) context.request()
                .getAttribute("com.adobe.granite.ui.clientlibs.HtmlLibraryManager.included")).size());
        assertNull(context.request().getAttribute("com.day.cq.wcm.componentcontext"));
    }

    @Test
    public void testUriExclusion_defaults_true() {
        context.registerInjectActivateService(new ErrorPageHandlerImpl());
        ErrorPageHandlerImpl errorPageHandlerService = (ErrorPageHandlerImpl) context.getService(ErrorPageHandlerService.class);

        context.request().setPathInfo("/content");

        assertTrue(errorPageHandlerService.shouldRequestUseErrorPageHandler(context.request()));
    }

    @Test
    public void testUriExclusion_defaults_site_false() {
        context.registerInjectActivateService(new ErrorPageHandlerImpl());
        ErrorPageHandlerImpl errorPageHandlerService = (ErrorPageHandlerImpl) context.getService(ErrorPageHandlerService.class);

        context.request().setPathInfo("/content/test-site/home.html");

        assertFalse(errorPageHandlerService.shouldRequestUseErrorPageHandler(context.request()));
    }

    @Test
    public void testUriExclusion_defaults_dam_false() {
        context.registerInjectActivateService(new ErrorPageHandlerImpl());
        ErrorPageHandlerImpl errorPageHandlerService = (ErrorPageHandlerImpl) context.getService(ErrorPageHandlerService.class);

        context.request().setPathInfo("/content/dam/test/file.png");

        assertFalse(errorPageHandlerService.shouldRequestUseErrorPageHandler(context.request()));

    }

    @Test
    public void testUrihExclusion_true() {
        context.registerInjectActivateService(new ErrorPageHandlerImpl(), "error-page.uri-exclusions", new String[]{"^/whatever(/.*)?"});
        ErrorPageHandlerImpl errorPageHandlerService = (ErrorPageHandlerImpl) context.getService(ErrorPageHandlerService.class);

        context.request().setPathInfo("/content");

        assertTrue(errorPageHandlerService.shouldRequestUseErrorPageHandler(context.request()));
    }

    @Test
    public void testUriExclusion_false() {
        context.registerInjectActivateService(new ErrorPageHandlerImpl(), "error-page.uri-exclusions", new String[]{"^/whatever(/.*)?", "^/content/(.*)/foo/(.*)?"});
        ErrorPageHandlerImpl errorPageHandlerService = (ErrorPageHandlerImpl) context.getService(ErrorPageHandlerService.class);

        context.request().setPathInfo("/content/whatever/foo/bar.html");
        assertFalse(errorPageHandlerService.shouldRequestUseErrorPageHandler(context.request()));
    }
}