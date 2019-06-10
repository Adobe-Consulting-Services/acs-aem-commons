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
package com.adobe.acs.commons.properties.impl;

import java.util.HashMap;
import java.util.Map;

import com.adobe.acs.commons.properties.PropertyAggregatorService;
import com.day.cq.wcm.api.Page;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PropertyAggregatorServiceImplTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    private PropertyAggregatorService service;

    @Before
    public void setup() {
        context.load().json(getClass().getResourceAsStream("PropertyAggregatorServiceContent.json"), "/content/we-retail/language-masters/en/experience");
    }

    @Test
    public void testAggregationOfPageResource() {
        service = defaultService();

        Resource lofoten = context.resourceResolver().getResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten");
        Map<String, Object> properties = service.getProperties(lofoten);
        Map<String, Object> expected = defaultPropertyMap();
        assertEquals(expected, properties);
    }

    @Test
    public void testAggregationOfComponentOnPage() {
        service = defaultService();

        Resource lofoten = context.resourceResolver().getResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/hero_image");
        Map<String, Object> properties = service.getProperties(lofoten);
        Map<String, Object> expected = defaultPropertyMap();
        assertEquals(expected, properties);
    }


    @Test
    public void testAggregationOfPage() {
        service = defaultService();

        Resource lofoten = context.resourceResolver().getResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten");
        Page lofotenPage = lofoten.adaptTo(Page.class);
        Map<String, Object> properties = service.getProperties(lofotenPage);
        Map<String, Object> expected = defaultPropertyMap();
        assertEquals(expected, properties);
    }

    @Test
    public void testContentInheritanceOverride() {
        service = defaultService();

        Resource lofoten = context.resourceResolver().getResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content");
        ModifiableValueMap modifiableValueMap = lofoten.adaptTo(ModifiableValueMap.class);
        // Put in the 'inheritedProperty' value onto the lofoten node itself to be pulled instead of inheriting
        modifiableValueMap.put("inheritedProperty", "newValue");
        Map<String, Object> properties = service.getProperties(lofoten);
        Map<String, Object> expected = defaultPropertyMap();
        // Remove old inherited value from default expected
        expected.remove("inherited_page_properties.inheritedProperty");
        // Add overridden value to expected
        expected.put("page_properties.inheritedProperty", "newValue");
        assertEquals(expected, properties);
    }

    @Test
    public void testPropertyExclusion() {
        Map<String, Object> config = defaultConfigMap();
        config.put("exclude.list", new String[]{"cq:(.*)", "jcr:(.*)"});
        service = context.registerInjectActivateService(new PropertyAggregatorServiceImpl(), config);

        Resource lofoten = context.resourceResolver().getResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten");
        Map<String, Object> properties = service.getProperties(lofoten);
        Map<String, Object> expected = new HashMap<>();
        expected.put("inherited_page_properties.inheritedProperty", "inheritedValue");
        expected.put("page_properties.sling:resourceType", "weretail/components/structure/page");
        assertEquals(expected, properties);
    }

    @Test
    public void testAdditionalData() {
        Map<String, Object> config = defaultConfigMap();
        config.put("additional.data", "test_model|com.adobe.acs.commons.properties.impl.PropertyAggregatorTestModel");
        service = context.registerInjectActivateService(new PropertyAggregatorServiceImpl(), config);

        context.addModelsForClasses(PropertyAggregatorTestModel.class);
        Resource lofoten = context.resourceResolver().getResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten");
        Map<String, Object> properties = service.getProperties(lofoten);
        Map<String, Object> expected = defaultPropertyMap();
        // Adding expected sling model added property
        expected.put("test_model.title", "Arctic Surfing In Lofoten Test");
        assertEquals(expected, properties);
    }

    private Map<String, Object> defaultPropertyMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("inherited_page_properties.inheritedProperty", "inheritedValue");
        map.put("page_properties.jcr:primaryType", "cq:PageContent");
        map.put("page_properties.jcr:title", "Arctic Surfing In Lofoten");
        map.put("page_properties.sling:resourceType", "weretail/components/structure/page");
        map.put("page_properties.jcr:createdBy", "admin");
        return map;
    }

    private Map<String, Object> defaultConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("exclude.list", "cq:(.*)");
        map.put("additional.data", "");
        return map;
    }

    private PropertyAggregatorService defaultService() {
        Map<String, Object> config = defaultConfigMap();
        return context.registerInjectActivateService(new PropertyAggregatorServiceImpl(), config);
    }
}
