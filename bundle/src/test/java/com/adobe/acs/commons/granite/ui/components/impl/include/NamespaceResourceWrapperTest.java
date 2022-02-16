/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.granite.ui.components.impl.include;

import com.adobe.granite.ui.components.ExpressionResolver;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.adobe.acs.commons.granite.ui.components.impl.include.IncludeDecoratorFilterImpl.REQ_ATTR_NAMESPACE;
import static com.adobe.acs.commons.granite.ui.components.impl.include.IncludeDecoratorFilterImpl.PREFIX;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NamespaceResourceWrapperTest {

    NamespaceResourceWrapper systemUnderTest;

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Mock
    private ExpressionResolver expressionResolver;

    private String[] properties = new String[]{"name"};

    @Before
    public void setUp() throws Exception {

        InputStream inputStream = getClass().getResourceAsStream("namespace-wrapper-test.json");
        context.load().json(inputStream, "/apps/component");
        context.currentResource("/apps/component/items/column/items");

        //this is to pass the parent.listChildren code (with granit:hide check) from the parent class (FilteringResourceWrapper)
        when(expressionResolver.resolve(anyString(), any(Locale.class), any(Class.class), any(SlingHttpServletRequest.class))).thenReturn(false);

    }

    @Test
    public void test() {

        Map<String,Object> parameters = new HashMap<>();
        parameters.put("numberFieldDefaultValue", 125L);
        parameters.put("doubleFieldDefaultValue", 11.34d);
        parameters.put("hideAField", Boolean.TRUE);
        parameters.put("fieldLabelText", "Some Text from parameters");

        setParameters(parameters);

        systemUnderTest = new NamespaceResourceWrapper(context.currentResource(), expressionResolver, context.request(), properties);




        Resource someDoubleField = systemUnderTest.getChild("someDoubleField");
        Double doubleDefaultValue = someDoubleField.getValueMap().get("defaultValue", Double.class);
        assertEquals(11.34d, doubleDefaultValue.doubleValue(), 0);

        Resource someNumberField = systemUnderTest.getChild("someNumberField");
        Long longDefaultValue = someNumberField.getValueMap().get("defaultValue", Long.class);
        assertEquals(125L, longDefaultValue.longValue(), 0);

        Resource hideMeField = systemUnderTest.getChild("hideMe");
        assertNull(hideMeField);

        Resource regularTextField = systemUnderTest.getChild("someRegularField");
        String fieldLabelValue = regularTextField.getValueMap().get("fieldLabel", "");
        assertEquals("Some Text from parameters", fieldLabelValue);
    }



    @Test
    public void test_default_values() {

        systemUnderTest = new NamespaceResourceWrapper(context.currentResource(), expressionResolver, context.request(),properties);

        Resource someDoubleField = systemUnderTest.getChild("someDoubleField");
        Double doubleDefaultValue = someDoubleField.getValueMap().get("defaultValue", Double.class);
        assertEquals(20.22d, doubleDefaultValue.doubleValue(), 0);

        Resource someNumberField = systemUnderTest.getChild("someNumberField");
        Long longDefaultValue = someNumberField.getValueMap().get("defaultValue", Long.class);
        assertEquals(20L, longDefaultValue.longValue(), 0);

        Resource hideMeField = systemUnderTest.getChild("hideMe");
        assertNotNull(hideMeField);

        Resource regularTextField = systemUnderTest.getChild("someRegularField");
        String fieldLabelValue = regularTextField.getValueMap().get("fieldLabel", "");
        assertEquals("defaultText", fieldLabelValue);
    }


    @Test
    public void test_namespacing() {

        context.request().setAttribute(REQ_ATTR_NAMESPACE, "block1");
        systemUnderTest = new NamespaceResourceWrapper(context.currentResource(), expressionResolver, context.request(),properties);

        Resource someDoubleField = systemUnderTest.getChild("someDoubleField");

        assertEquals("./block1/doubleField", someDoubleField.getValueMap().get("name", String.class));

    }


    private void setParameters(Map<String,Object> parameters){
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.request().setAttribute(PREFIX + entry.getKey(), entry.getValue());
        }
    }

}
