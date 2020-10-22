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
        this.aemContext.create().resource("/content/mysite/en/mypage/jcr:content/parent3/parent2", "booleanProperty", true);
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
        assertTrue(model.getBooleanProperty());
        assertNull(model.getStringProperties());
    }

    @Test
    public void testModelValuesForRequestAdaptable() {
        TestParentResourceValueMapValueModel model = this.aemContext.request().adaptTo(TestParentResourceValueMapValueModel.class);
        assertNotNull(model);
        assertEquals("stringValue", model.getStringProperty());
        assertTrue(model.getBooleanProperty());
        assertNull(model.getStringProperties());
    }
}
