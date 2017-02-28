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

import static org.junit.Assert.assertEquals;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ErrorPageHandlerImplTest {

    @Rule
    public final SlingContext context = new SlingContext();
    
	private MockSlingHttpServletRequest request;
	private ResourceResolver resourceResolver;

    @Before
    public void setup() {
    	context.load().json(getClass().getResourceAsStream("ErrorPageHandlerImplTest.json"), "/content/project");
    	resourceResolver = context.resourceResolver();
    	request = new MockSlingHttpServletRequest(resourceResolver);
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

}