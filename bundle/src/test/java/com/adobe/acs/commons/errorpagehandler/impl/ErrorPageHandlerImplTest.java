/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.errorpagehandler.impl;

import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ErrorPageHandlerImplTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private MockSlingHttpServletRequest request;
    private ResourceResolver resourceResolver;

    @Before
    public void setup() {
        context.load().json(getClass().getResourceAsStream("ErrorPageHandlerImplTest.json"), "/content/project");
        resourceResolver = context.resourceResolver();
        request = context.request();
    }
    
    /**
     * Test {@link ErrorPageHandlerImpl#findErrorPage(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.resource.Resource)}
     * with a page without {@code jcr:content} node.
     */
    @Test
    public void testFindErrorPage_withoutContent() {
        assertEquals("/content/project/test/error-pages.html", new ErrorPageHandlerImpl().findErrorPage(request, resourceResolver.getResource("/content/project/test/page-without-content")));
    }

    /**
     * Test {@link ErrorPageHandlerImpl#findErrorPage(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.resource.Resource)}
     * with a page with direct configuration.
     */
    @Test
    public void testFindErrorPage_withDirectConfig() {
        assertEquals("/content/project/test/error-pages2.html", new ErrorPageHandlerImpl().findErrorPage(request, resourceResolver.getResource("/content/project/test/page-with-config")));
    }
    
    @Test
    public void testFindErrorPage_subResource() {
        assertEquals("/content/project/test/error-pages.html", new ErrorPageHandlerImpl().findErrorPage(request, new NonExistingResource(resourceResolver, "/content/project/test/jcr:content/root/non-existing-resource")));
    }

    @Test
    public void testFindErrorPage_nonExistingPage() {
        assertEquals("/content/project/test/error-pages.html", new ErrorPageHandlerImpl().findErrorPage(request, new NonExistingResource(resourceResolver, "/content/project/test/non-existing-page")));
    }

    @Test
    public void testFindErrorPage_nonExistingPageSubResource() {
        assertEquals("/content/project/test/error-pages.html", new ErrorPageHandlerImpl().findErrorPage(request, new NonExistingResource(resourceResolver, "/content/project/test/non-existing-page/jcr:content/test1/test2")));
    }

    @Test
    public void testFindErrorPage_nonExistingPageWithoutExtension() {
        assertEquals("/content/project/test/error-pages.html", new ErrorPageHandlerImpl().findErrorPage(request, new NonExistingResource(resourceResolver, "/content/project/non-existing-page")));
    }

    @Test
    public void testFindErrorPage_JcrContent0() {
        assertEquals("/content/project/test/error-pages.html",
                new ErrorPageHandlerImpl().findErrorPage(request,
                        new NonExistingResource(resourceResolver, "/content/project/jcr:content/non-existing")));
    }
}