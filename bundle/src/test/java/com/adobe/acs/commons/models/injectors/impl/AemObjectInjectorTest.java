/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.models.injectors.impl;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.xss.XSSAPI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;

@RunWith(MockitoJUnitRunner.class)
public class AemObjectInjectorTest {


    @Mock
    private PageManager pageManager;
    @Mock
    private Designer designer;

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);
    
    
    
    @Before
    public final void setUp() throws Exception {
        AemObjectInjector aemObjectsInjector = new AemObjectInjector();
        context.registerService(aemObjectsInjector);
        
        context.registerService(PageManager.class,pageManager);
        context.registerService(Designer.class,designer);
        context.addModelsForClasses(TestResourceModel.class);

        // create a resource to have something we can adapt
        context.create().resource("/content/resource");
        
    }

    @Test
    public final void testResourceInjection() {

        Resource r = context.resourceResolver().getResource("/content/resource");
        TestResourceModel testResourceModel = r.adaptTo(TestResourceModel.class);

        assertNotNull(testResourceModel);
        assertNotNull(testResourceModel.getResource());
        assertNotNull(testResourceModel.getResourceResolver());
        assertNotNull(testResourceModel.getPageManager());
        assertNotNull(testResourceModel.getDesigner());
        assertNotNull(testResourceModel.getSession());
        // TODO: Tests for the remaining injectable objects
    }

    @Test
    public final void testSlingHttpServiceRequestInjection() {

        Resource r = context.resourceResolver().getResource("/content/resource");
        TestResourceModel testResourceModel = r.adaptTo(TestResourceModel.class);

        assertNotNull(testResourceModel);
        assertNotNull(testResourceModel.getResource());
        assertNotNull(testResourceModel.getResourceResolver());
        assertNotNull(testResourceModel.getPageManager());
        assertNotNull(testResourceModel.getDesigner());
        assertNotNull(testResourceModel.getSession());
        // TODO: Tests for the remaining injectable objects
    }

    // --- inner classes ---

    @Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
    public static class TestResourceModel {

        @Inject
        private Resource resource;
        @Inject
        private ResourceResolver resourceResolver;
        @Inject @Optional
        private ComponentContext componentContext;
        @Inject
        private PageManager pageManager;
        @Inject @Optional
        private Page currentPage;
        @Inject @Optional
        private Page resourcePage;
        @Inject @Optional
        private Designer designer;
        @Inject @Optional
        private Design currentDesign;
        @Inject @Optional
        private Design resourceDesign;
        @Inject @Optional
        private Style currentStyle;
        @Inject @Optional
        private Session session;
        @Inject @Optional
        private XSSAPI xssApi;
        @Inject @Optional
        private String namedSomethingElse;

        public Resource getResource() {
            return resource;
        }

        public ResourceResolver getResourceResolver() {
            return resourceResolver;
        }

        public ComponentContext getComponentContext() {
            return componentContext;
        }

        public PageManager getPageManager() {
            return pageManager;
        }

        public Page getCurrentPage() {
            return currentPage;
        }

        public Page getResourcePage() {
            return resourcePage;
        }

        public Designer getDesigner() {
            return designer;
        }

        public Design getCurrentDesign() {
            return currentDesign;
        }

        public Design getResourceDesign() {
            return resourceDesign;
        }

        public Style getCurrentStyle() {
            return currentStyle;
        }

        public Session getSession() {
            return session;
        }

        public XSSAPI getXssApi() {
            return xssApi;
        }
    }
}
