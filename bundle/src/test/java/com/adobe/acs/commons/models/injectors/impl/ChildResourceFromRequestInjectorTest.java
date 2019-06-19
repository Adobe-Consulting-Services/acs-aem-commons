/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.impl.ChildResourceFromRequestAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelChildResourceFromRequest;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestModelChildResourceFromRequestChildImpl;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestModelChildResourceFromRequestImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.RepositoryException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ChildResourceFromRequestInjectorTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setup() throws RepositoryException {
        this.context.registerInjectActivateService(new ChildResourceFromRequestAnnotationProcessorFactory());
        this.context.registerInjectActivateService(new ChildResourceFromRequestInjector());
        this.context.addModelsForClasses(TestModelChildResourceFromRequestImpl.class);
        this.context.addModelsForClasses(TestModelChildResourceFromRequestChildImpl.class);

        this.context.create().resource("/content/child", "prop", "val1");
        this.context.create().resource("/content/childList/1", "prop", "val2");
        this.context.create().resource("/content/childList/2", "prop", "val3");

        this.context.currentResource("/content");
    }

    @Test
    public void testInjectedChildModelFromRequest() {
        TestModelChildResourceFromRequest testModel = this.context.request().adaptTo(TestModelChildResourceFromRequest.class);
        assertEquals("val1", testModel.getChildModel().getProp());
        assertEquals("/content/child", testModel.getChildModel().getResource().getPath());
        assertEquals("/content/child", testModel.getChildModel().getRequest().getResource().getPath());
    }

    @Test
    public void testInjectedChildModelFromResource() {
        TestModelChildResourceFromRequest testModel = this.context.currentResource().adaptTo(TestModelChildResourceFromRequest.class);
        assertEquals("val1", testModel.getChildModel().getProp());
        assertEquals("/content/child", testModel.getChildModel().getResource().getPath());
        assertNull("/content/child", testModel.getChildModel().getRequest());
    }

    @Test
    public void testInjectedChildModelListFromRequest() {
        TestModelChildResourceFromRequest testModel = this.context.request().adaptTo(TestModelChildResourceFromRequest.class);

        assertEquals(2, testModel.getChildModelList().size());

        assertEquals("/content/childList/1", testModel.getChildModelList().get(0).getResource().getPath());
        assertEquals("/content/childList/1", testModel.getChildModelList().get(0).getRequest().getResource().getPath());
        assertEquals("val2", testModel.getChildModelList().get(0).getProp());

        assertEquals("/content/childList/2", testModel.getChildModelList().get(1).getResource().getPath());
        assertEquals("/content/childList/2", testModel.getChildModelList().get(1).getRequest().getResource().getPath());
        assertEquals("val3", testModel.getChildModelList().get(1).getProp());
    }

    @Test
    public void testInjectedChildModelListFromResource() {
        TestModelChildResourceFromRequest testModel = this.context.currentResource().adaptTo(TestModelChildResourceFromRequest.class);

        assertEquals(2, testModel.getChildModelList().size());

        assertEquals("/content/childList/1", testModel.getChildModelList().get(0).getResource().getPath());
        assertNull(testModel.getChildModelList().get(0).getRequest());
        assertEquals("val2", testModel.getChildModelList().get(0).getProp());

        assertEquals("/content/childList/2", testModel.getChildModelList().get(1).getResource().getPath());
        assertNull(testModel.getChildModelList().get(1).getRequest());
        assertEquals("val3", testModel.getChildModelList().get(1).getProp());
    }

    @Test
    public void testInjectedChildResourceFromRequest() {
        TestModelChildResourceFromRequest testModel = this.context.request().adaptTo(TestModelChildResourceFromRequest.class);
        assertEquals("val1", testModel.getChildResource().getValueMap().get("prop", String.class));
    }

    @Test
    public void testInjectedChildResourceFromResource() {
        TestModelChildResourceFromRequest testModel = this.context.currentResource().adaptTo(TestModelChildResourceFromRequest.class);
        assertEquals("val1", testModel.getChildResource().getValueMap().get("prop", String.class));
    }

    @Test
    public void testInjectedChildResourceListFromRequest() {
        TestModelChildResourceFromRequest testModel = this.context.request().adaptTo(TestModelChildResourceFromRequest.class);
        assertEquals(2, testModel.getChildResourceList().size());
        assertEquals("val2", testModel.getChildResourceList().get(0).getValueMap().get("prop", String.class));
        assertEquals("val3", testModel.getChildResourceList().get(1).getValueMap().get("prop", String.class));
    }

    @Test
    public void testInjectedChildResourceListFromResource() {
        TestModelChildResourceFromRequest testModel = this.context.currentResource().adaptTo(TestModelChildResourceFromRequest.class);
        assertEquals(2, testModel.getChildResourceList().size());
        assertEquals("val2", testModel.getChildResourceList().get(0).getValueMap().get("prop", String.class));
        assertEquals("val3", testModel.getChildResourceList().get(1).getValueMap().get("prop", String.class));
    }
}
