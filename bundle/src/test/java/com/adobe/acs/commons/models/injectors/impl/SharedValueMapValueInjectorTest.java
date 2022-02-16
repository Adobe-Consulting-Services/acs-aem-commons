/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.impl.SharedValueMapValueAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestSharedValueMapValueModel;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestSharedValueMapValueModelImpl;
import com.adobe.acs.commons.wcm.impl.PageRootProviderConfig;
import com.adobe.acs.commons.wcm.impl.PageRootProviderMultiImpl;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.adobe.acs.commons.wcm.properties.shared.impl.SharedComponentPropertiesBindingsValuesProvider;
import com.adobe.acs.commons.wcm.properties.shared.impl.SharedComponentPropertiesImpl;
import com.day.cq.wcm.api.NameConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SharedValueMapValueInjectorTest {
    public static final String STRING_PROP = "stringProp";
    public static final String STRING_PROP_2 = "stringProp2";
    public static final String STRING_PROP_3 = "stringProp3";
    public static final String LONG_PROP = "longProp";
    public static final String LONG_PROP_STR = "longPropStr";
    public static final String BOOL_PROP_TRUE = "boolPropTrue";
    public static final String BOOL_PROP_FALSE = "boolPropFalse";
    public static final String BOOL_PROP_TRUE_STR = "boolPropTrueStr";
    public static final String BOOL_PROP_FALSE_STR = "boolPropFalseStr";
    public static final String STRING_ARRAY_PROP = "stringArrayProp";
    public static final String LONG_ARRAY_PROP = "longArrayProp";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private ResourceResolver resourceResolver;

    private TestSharedValueMapValueModel model;

    private Resource modelResource;

    @Before
    public void setup() throws RepositoryException {
        this.context.registerInjectActivateService(new PageRootProviderConfig(), "page.root.path", "/content/mysite/[a-z]{2}");
        this.context.registerInjectActivateService(new PageRootProviderMultiImpl());
        this.context.registerInjectActivateService(new SharedComponentPropertiesImpl());
        this.context.registerInjectActivateService(new SharedValueMapValueAnnotationProcessorFactory());
        this.context.registerInjectActivateService(new SharedValueMapValueInjector());
        this.context.addModelsForClasses(TestSharedValueMapValueModelImpl.class);

        this.resourceResolver = this.context.resourceResolver();
        Session session = this.resourceResolver.adaptTo(Session.class);
        Node contentNode = session.getRootNode().addNode("content", JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
        Node siteNode = contentNode.addNode("mysite", NameConstants.NT_PAGE);
        Node homepageNode = siteNode.addNode("en", NameConstants.NT_PAGE);
        Node homepageContentNode = homepageNode.addNode(JcrConstants.JCR_CONTENT, "cq:PageContent");
        Node subpageNode = homepageNode.addNode("mypage", NameConstants.NT_PAGE);
        Node subpageCotentNode = subpageNode.addNode(JcrConstants.JCR_CONTENT, "cq:PageContent");
        Node componentNode = subpageCotentNode.addNode("mycomponent", JcrConstants.NT_UNSTRUCTURED);
        componentNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "mysite/components/content/customcomponent");
        componentNode.setProperty(STRING_PROP, "localValue");

        Node globalPropertiesNode = homepageContentNode.addNode(SharedComponentProperties.NN_GLOBAL_COMPONENT_PROPERTIES, JcrConstants.NT_UNSTRUCTURED);
        globalPropertiesNode.setProperty(STRING_PROP, "globalValue");
        globalPropertiesNode.setProperty(STRING_PROP_2, "globalValue2");
        globalPropertiesNode.setProperty(STRING_PROP_3, "globalValue3");
        globalPropertiesNode.setProperty(LONG_PROP, 123L);
        globalPropertiesNode.setProperty(LONG_PROP_STR, "234");
        globalPropertiesNode.setProperty(STRING_ARRAY_PROP, new String[] {"abc", "def"});


        Node sharedPropertiesNode = homepageContentNode.addNode(SharedComponentProperties.NN_SHARED_COMPONENT_PROPERTIES, JcrConstants.NT_UNSTRUCTURED)
                .addNode("mysite", JcrConstants.NT_UNSTRUCTURED)
                .addNode("components", JcrConstants.NT_UNSTRUCTURED)
                .addNode("content", JcrConstants.NT_UNSTRUCTURED)
                .addNode("customcomponent", JcrConstants.NT_UNSTRUCTURED);
        sharedPropertiesNode.setProperty(STRING_PROP, "sharedValue");
        sharedPropertiesNode.setProperty(STRING_PROP_2, "sharedValue2");
        sharedPropertiesNode.setProperty(BOOL_PROP_TRUE, true);
        sharedPropertiesNode.setProperty(BOOL_PROP_FALSE, false);
        sharedPropertiesNode.setProperty(BOOL_PROP_TRUE_STR, "true");
        sharedPropertiesNode.setProperty(BOOL_PROP_FALSE_STR, "false");
        sharedPropertiesNode.setProperty(LONG_ARRAY_PROP, new String[] {"345", "456"});

        modelResource = this.resourceResolver.getResource("/content/mysite/en/mypage/jcr:content/mycomponent");
        model = modelResource.adaptTo(TestSharedValueMapValueModel.class);
    }

    @Test
    public void testPropertyTypeGlobal() {
        assertEquals("globalValue", model.getGlobalStringProp());
    }

    @Test
    public void testPropertyTypeShared() {
        assertEquals("sharedValue", model.getSharedStringProp());
    }

    @Test
    public void testPropertyTypeMerged() {
        assertEquals("localValue", model.getMergedStringProp());
    }

    @Test
    public void testPropertyTypeDefault() {
        // Test when local, shared, and global are all set
        assertEquals("localValue", model.getStringProp());
        // Test when shared and global are only set
        assertEquals("sharedValue2", model.getStringProp2());
        // Test when global is only set
        assertEquals("globalValue3", model.getStringProp3());
    }

    @Test
    public void testPropertyTypeBoolean() {
        assertTrue(model.isBoolPropTrue());
        assertFalse(model.isBoolPropFalse());
        assertTrue(model.isBoolPropTrueFromString());
        assertFalse(model.isBoolPropFalseFromString());
    }

    @Test
    public void testPropertyTypeLong() {
        assertEquals(123L, model.getLongProp().longValue());
        assertEquals(234L, model.getLongPropFromString().longValue());
    }

    @Test
    public void testPropertyArray() {
        assertArrayEquals(new String[] {"abc", "def"}, model.getStringArrayProp());
    }

    @Test
    public void testPropertyCollection() {
        assertEquals(Arrays.asList("abc", "def"), model.getStringListProp());
        assertEquals(Arrays.asList("abc", "def"), model.getStringCollectionProp());
    }

    @Test
    public void testPropertyArrayWithTypeConversion() {
        assertArrayEquals(new Long[] {345L, 456L}, model.getLongArrayProp());
    }

    @Test
    public void testPropertyCollectionWithTypeConversion() {
        assertEquals(Arrays.asList(345L, 456L), model.getLongListProp());
        assertEquals(Arrays.asList(345L, 456L), model.getLongCollectionProp());
    }

    @Test
    public void testPropertyArrayFromNonArray() {
        assertArrayEquals(new Long[] {123L}, model.getLongArrayPropFromNonArray());
        assertEquals(Collections.singletonList(123L), model.getLongListPropFromNonArray());
    }

    @Test
    public void testModelAdaptedFromRequest() {
        MockSlingHttpServletRequest mockRequest = new MockSlingHttpServletRequest(this.resourceResolver, this.context.bundleContext());
        mockRequest.setResource(this.modelResource);
        TestSharedValueMapValueModel modelFromRequest = mockRequest.adaptTo(TestSharedValueMapValueModel.class);

        assertEquals("sharedValue2", modelFromRequest.getStringProp2());
    }

}
