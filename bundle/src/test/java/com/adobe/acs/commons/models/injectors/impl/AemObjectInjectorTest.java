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

import org.apache.sling.xss.XSSAPI;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.models.spi.Injector;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import javax.jcr.Session;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AemObjectInjectorTest {

    @Mock
    private Resource resource;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private Session session;
    @Mock
    private PageManager pageManager;
    @Mock
    private Designer designer;

    private TestModelAdapterFactory factory;
    
    
    /**
     * TODO: It would definitly make sense to convert this test to use Sling-Mocks instead
     * of directly working on a ModelAdapterFactory.
     * 
     */

    @Before
    public final void setUp() throws Exception {
        AemObjectInjector aemObjectsInjector = new AemObjectInjector();
        factory = new TestModelAdapterFactory();

        factory.bindInjector(aemObjectsInjector, Collections.<String, Object> singletonMap(Constants.SERVICE_ID, 1L));
    }

    @Test
    @Ignore
    public final void testResourceInjection() {
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(resourceResolver.adaptTo(Designer.class)).thenReturn(designer);

        TestResourceModel testResourceModel = factory.getAdapter(resource, TestResourceModel.class);

        assertNotNull(testResourceModel);
        assertNotNull(testResourceModel.getResource());
        assertNotNull(testResourceModel.getResourceResolver());
        assertNotNull(testResourceModel.getPageManager());
        assertNotNull(testResourceModel.getDesigner());
        assertNotNull(testResourceModel.getSession());
        // TODO: Tests for the remaining injectable objects
    }

    @Test
    @Ignore
    public final void testSlingHttpServiceRequestInjection() {
        when(request.getResource()).thenReturn(resource);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(resourceResolver.adaptTo(Designer.class)).thenReturn(designer);

        TestResourceModel testResourceModel = factory.getAdapter(request, TestResourceModel.class);

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

    // makes activate() and bindInjector() accessible
    private class TestModelAdapterFactory extends ModelAdapterFactory {

        public TestModelAdapterFactory() {
            super();

            org.osgi.service.component.ComponentContext componentCtx = mock(org.osgi.service.component.ComponentContext.class);
            BundleContext bundleContext = mock(BundleContext.class);
            when(componentCtx.getBundleContext()).thenReturn(bundleContext);
            when(componentCtx.getProperties()).thenReturn(new Hashtable());

            activate(componentCtx);
        }

        @Override
        public void bindInjector(Injector injector, Map<String, Object> props) {
            super.bindInjector(injector, props);
        }

    }
}
