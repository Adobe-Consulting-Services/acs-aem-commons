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
package com.adobe.acs.commons.models.injectors.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.function.Function;

import javax.inject.Inject;
import javax.jcr.Session;

import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.commons.Externalizer;
import com.day.cq.search.QueryBuilder;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
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
import org.mockito.junit.MockitoJUnitRunner;

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
    @Mock
    private TagManager tagManager;

    @Mock
    private AssetManager assetManager;

    @Mock
    private com.day.cq.dam.api.AssetManager oldAssetManager;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private ContentPolicyManager contentPolicyManager;

    @Mock
    private Externalizer externalizer;

    @Mock
    private Page resourcePage;

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);


    @Before
    public final void setUp() throws Exception {
        AemObjectInjector aemObjectsInjector = new AemObjectInjector();
        context.registerService(aemObjectsInjector);


        Function adaptHandler = input -> pageManager;
        context.registerAdapter(ResourceResolver.class, PageManager.class, adaptHandler);
        context.registerService(PageManager.class,pageManager);
        context.registerService(Designer.class,designer);
        context.registerService(TagManager.class, tagManager);
        context.registerService(AssetManager.class, assetManager);
        context.registerService(com.day.cq.dam.api.AssetManager.class, oldAssetManager);
        context.registerService(QueryBuilder.class, queryBuilder);
        context.registerService(ContentPolicyManager.class, contentPolicyManager);
        context.registerService(Externalizer.class, externalizer);
        context.addModelsForClasses(TestResourceModel.class);


        // create a resource to have something we can adapt
        context.create().resource("/content/resource");
        when(resourcePage.getLanguage(false)).thenReturn(Locale.ENGLISH);
        when(pageManager.getContainingPage(any(Resource.class))).thenReturn(resourcePage);
    }

    @Test
    public final void testResourceInjection() {

        Resource r = context.resourceResolver().getResource("/content/resource");
        TestResourceModel testResourceModel = r.adaptTo(TestResourceModel.class);

        assertNotNull(testResourceModel);
        assertNotNull(testResourceModel.getResource());
        assertNotNull(testResourceModel.getResourceResolver());
        assertNotNull(testResourceModel.getPageManager());
        assertNotNull(testResourceModel.getTagManager());
        assertNotNull(testResourceModel.getDesigner());
        assertNotNull(testResourceModel.getLocale());
        assertNotNull(testResourceModel.getAssetManager());
        assertNotNull(testResourceModel.getOldAssetManager());
        assertNotNull(testResourceModel.getExternalizer());
        assertNotNull(testResourceModel.getContentPolicyManager());
        assertNotNull(testResourceModel.getQueryBuilder());

        assertEquals(Locale.ENGLISH, testResourceModel.getLocale());

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
        assertNotNull(testResourceModel.getTagManager());
        assertNotNull(testResourceModel.getDesigner());
        assertNotNull(testResourceModel.getLocale());
        assertNotNull(testResourceModel.getAssetManager());
        assertNotNull(testResourceModel.getOldAssetManager());
        assertNotNull(testResourceModel.getExternalizer());
        assertNotNull(testResourceModel.getContentPolicyManager());
        assertNotNull(testResourceModel.getQueryBuilder());
        assertEquals(Locale.ENGLISH, testResourceModel.getLocale());
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
        private TagManager tagManager;

        @Inject @Optional
        private String namedSomethingElse;

        @Inject @Optional
        private Locale locale;

        @Inject @Optional
        private AssetManager  assetManager;

        @Inject @Optional
        private com.day.cq.dam.api.AssetManager oldAssetManager;

        @Inject @Optional
        private Externalizer externalizer;

        @Inject @Optional
        private QueryBuilder queryBuilder;

        @Inject @Optional
        private ContentPolicyManager contentPolicyManager;

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

        public TagManager getTagManager() {
            return tagManager;
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

        public Locale getLocale() {
            return locale;
        }

        public AssetManager getAssetManager() {
            return assetManager;
        }

        public com.day.cq.dam.api.AssetManager getOldAssetManager() {
            return oldAssetManager;
        }

        public Externalizer getExternalizer() {
            return externalizer;
        }

        public QueryBuilder getQueryBuilder() {
            return queryBuilder;
        }

        public ContentPolicyManager getContentPolicyManager() {
            return contentPolicyManager;
        }
    }
}
