/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.impl.ParentResourceValueMapValueAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestParentResourceValueMapValueModel;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestParentResourceValueMapValueModelImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParentResourceValueMapValueInjectorTest {

    @Rule
    public final AemContext aemContext = new AemContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setUp() throws Exception {
        this.aemContext.registerInjectActivateService(new ParentResourceValueMapValueAnnotationProcessorFactory());
        this.aemContext.registerInjectActivateService(new ParentResourceValueMapValueInjector());
        this.aemContext.create().page("/content/mysite/en/mypage");
        this.aemContext.create().resource("/content/mysite/en/mypage/jcr:content/parent3", "prop", "val3");
        this.aemContext.create().resource("/content/mysite/en/mypage/jcr:content/parent3/parent2", "booleanProperty", true, "stringLevel2Property", "stringLevel2Value", "jcr:title","Parent 2 Resource Title");
        this.aemContext.create().resource("/content/mysite/en/mypage/jcr:content/parent3/parent2/parent1", "stringProperty", "stringValue");
        this.aemContext.create().resource("/content/mysite/en/mypage/jcr:content/parent3/parent2/parent1/mycomponent", "property1", "value1", "property2", "value2");
        this.aemContext.currentResource("/content/mysite/en/mypage/jcr:content/parent3/parent2/parent1/mycomponent");
        this.aemContext.addModelsForClasses(TestParentResourceValueMapValueModelImpl.class);
    }

    @Test
    public void testModelValuesForResourceAdaptable() {
        TestParentResourceValueMapValueModel model = this.aemContext.currentResource().adaptTo(TestParentResourceValueMapValueModel.class);
        assertNotNull(model);
        assertEquals("stringValue", model.getStringProperty());
        // Return null if the max-level is specified as 1 but the property is available at level 2
        assertNull(model.getStringLevel2Property());
        // Testing with name parameter of the annotation
        assertEquals("Parent 2 Resource Title", model.getTitle());
        // Returns the property by iterating through all the parent resources if the max-level is not specified and set to default (-1)
        assertTrue(model.getBooleanProperty());
        // Returns null if the property is not found
        assertNull(model.getStringProperties());
    }

    @Test
    public void testModelValuesForRequestAdaptable() {
        TestParentResourceValueMapValueModel model = this.aemContext.request().adaptTo(TestParentResourceValueMapValueModel.class);
        assertNotNull(model);
        assertEquals("stringValue", model.getStringProperty());
        assertNull(model.getStringLevel2Property());
        assertEquals("Parent 2 Resource Title", model.getTitle());
        assertTrue(model.getBooleanProperty());
        assertNull(model.getStringProperties());
    }
}
