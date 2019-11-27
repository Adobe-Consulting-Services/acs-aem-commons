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
package com.adobe.acs.commons.synth.children;

import org.apache.commons.collections.IteratorUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChildrenAsPropertyResourceTest {

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    ModifiableValueMap valueMap;

    JSONObject unsortedJSON = new JSONObject();

    JSONObject sortedJSON = new JSONObject();

    Map<String, Object> entry1 = new HashMap<String, Object>();

    Map<String, Object> entry2 = new HashMap<String, Object>();

    Map<String, Object> entry3 = new HashMap<String, Object>();

    Map<String, Object> entry100 = new HashMap<String, Object>();

    ChildrenAsPropertyResource childrenAsPropertyResource;

    @Before
    public void setUp() throws Exception {
        valueMap = new ModifiableValueMapDecorator(new HashMap<String, Object>());

        when(resource.getPath()).thenReturn("/content/test");
        when(resource.getValueMap()).thenReturn(valueMap);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(valueMap);
        when(resource.getResourceResolver()).thenReturn(resourceResolver);

        entry1.put("name", "dog");
        entry1.put("sound", "woof");
        entry1.put("jcr:primaryType", "nt:unstructured");

        entry2.put("name", "cat");
        entry2.put("sound", "meow");
        entry1.put("jcr:primaryType", "nt:unstructured");

        entry3.put("name", "fish");
        entry3.put("sound", "...");
        entry1.put("jcr:primaryType", "nt:unstructured");

        entry100.put("name", "dog");
        entry100.put("sound", "woof");
        entry100.put("double", 20.002D);
        entry100.put("long", 2000L);
        entry100.put("bigdecimal", "10000.000001");
        entry100.put("bigdecimal2", "20000.000002");
        entry100.put("integer", 100);
        entry100.put("integerAsLong", 100L);
        entry100.put("integerAsDouble", 100D);

        Calendar cal = Calendar.getInstance();
        cal.set(2001, 1, 1, 1, 1, 1);
        entry100.put("date", cal.getTime());
        entry100.put("calendar", cal);
        entry100.put("boolean", true);
        entry100.put("booleanAsString", "true");

        entry100.put("strArray", new String[]{"one", "two"});
        entry1.put("jcr:primaryType", "nt:unstructured");

        unsortedJSON.put("entry-2", new JSONObject(entry2));
        unsortedJSON.put("entry-1", new JSONObject(entry1));
        unsortedJSON.put("entry-3", new JSONObject(entry3));

        sortedJSON.put("entry-1", new JSONObject(entry1));
        sortedJSON.put("entry-2", new JSONObject(entry2));
        sortedJSON.put("entry-3", new JSONObject(entry3));
    }

    @Test
    public void testSerialization() throws Exception {
        childrenAsPropertyResource =
            new ChildrenAsPropertyResource(resource, "animals");

        childrenAsPropertyResource.create("entry-100", "nt:unstructured", entry100);

        List<Resource> actuals = IteratorUtils.toList(childrenAsPropertyResource.listChildren());

        ValueMap actual = actuals.get(0).getValueMap();
        ValueMap expected = new ValueMapDecorator(entry100);

        Assert.assertEquals(expected.get("name", String.class), actual.get("name", String.class));
        Assert.assertEquals(expected.get("double", String.class), actual.get("double", String.class));
        Assert.assertEquals(expected.get("long", Double.class), actual.get("long", Double.class));
        Assert.assertEquals(expected.get("integer", Integer.class), actual.get("integer", Integer.class));
        Assert.assertEquals(expected.get("integerAsLong", Integer.class), actual.get("integerAsLong", Integer.class));
        Assert.assertEquals(expected.get("boolean", Boolean.class), actual.get("boolean", Boolean.class));
        Assert.assertEquals(expected.get("booleanAsString", Boolean.class), actual.get("booleanAsString", Boolean.class));
        Assert.assertEquals(expected.get("date", Date.class), actual.get("date", Date.class));
        Assert.assertEquals(expected.get("calendar", Calendar.class), actual.get("calendar", Calendar.class));

        // Special expectation setting as ValueMaps do not handle these types are expected by the implementation
        Assert.assertEquals((Integer)expected.get("integerAsDouble", Double.class).intValue(), actual.get("integerAsDouble", Integer.class));
        Assert.assertEquals(new BigDecimal(expected.get("bigdecimal", String.class)), actual.get("bigdecimal", BigDecimal.class));
        Assert.assertEquals(new BigDecimal(expected.get("bigdecimal2", String.class)), actual.get("bigdecimal2", BigDecimal.class));

        Assert.assertNotNull(actual.get("date", Calendar.class));
        Assert.assertEquals(expected.get("calendar", Calendar.class), actual.get("date", Calendar.class));

        Assert.assertNotNull(actual.get("calendar", Date.class));
        Assert.assertEquals(expected.get("date", Date.class), actual.get("calendar", Date.class));
    }

    @Test
    public void testRemove() throws Exception {
        valueMap.put("animals", sortedJSON.toString());

        List<Resource> expected = new ArrayList<Resource>();
        expected.add(new SyntheticChildAsPropertyResource(resource, "entry-1", entry1));
        expected.add(new SyntheticChildAsPropertyResource(resource, "entry-3", entry3));

        childrenAsPropertyResource =
                new ChildrenAsPropertyResource(resource, "animals");

        List<Resource> actuals = IteratorUtils.toList(childrenAsPropertyResource.listChildren());
        Assert.assertEquals(expected.size() + 1, actuals.size());

        childrenAsPropertyResource.delete("entry-2");

        actuals = IteratorUtils .toList(childrenAsPropertyResource.listChildren());

        Assert.assertEquals(expected.size(), actuals.size());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i).getName(), actuals.get(i).getName());
        }
    }

    @Test
    public void testAdd_Unsorted() throws Exception {
        valueMap.put("animals", unsortedJSON.toString());

        ValueMap properties = new ValueMapDecorator(new HashMap<String, Object>());
        properties.put("name", "hyena");
        properties.put("sound", "lolz");
        properties.put("jcr:primaryType", "nt:unstructured");

        JSONObject expectedJSON = new JSONObject(unsortedJSON.toString());
        expectedJSON.put("entry-4", new JSONObject(properties));

        childrenAsPropertyResource =
                new ChildrenAsPropertyResource(resource, "animals");

        childrenAsPropertyResource.create("entry-4", "nt:unstructured", properties);
        childrenAsPropertyResource.persist();

        String actual = resource.getValueMap().get("animals", String.class);
        String expected = expectedJSON.toString();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAdd_Sorted() throws Exception {
        valueMap.put("animals", unsortedJSON.toString());

        ValueMap properties = new ValueMapDecorator(new HashMap<String, Object>());
        properties.put("name", "hyena");
        properties.put("sound", "lolz");
        properties.put("jcr:primaryType", "nt:unstructured");

        JSONObject expectedJSON = new JSONObject(sortedJSON.toString());
        expectedJSON.put("entry-4", new JSONObject(properties));

        childrenAsPropertyResource =
                new ChildrenAsPropertyResource(resource, "animals",
                        ChildrenAsPropertyResource.RESOURCE_NAME_COMPARATOR);

        childrenAsPropertyResource.create("entry-4", "nt:unstructured", properties);
        childrenAsPropertyResource.persist();

        String actual = resource.getValueMap().get("animals", String.class);
        String expected = expectedJSON.toString();

        Assert.assertEquals(expected, actual);
    }


    @Test
    public void testGet_Unsorted() throws Exception {
        valueMap.put("animals", sortedJSON.toString());

        List<Resource> expected = new ArrayList<Resource>();
        expected.add(new SyntheticChildAsPropertyResource(resource, "entry-1", new ValueMapDecorator(entry1)));
        expected.add(new SyntheticChildAsPropertyResource(resource, "entry-2", new ValueMapDecorator(entry2)));
        expected.add(new SyntheticChildAsPropertyResource(resource, "entry-3", new ValueMapDecorator(entry3)));

        ChildrenAsPropertyResource childrenAsPropertyResource =
                new ChildrenAsPropertyResource(resource, "animals");

        List<Resource> actual = IteratorUtils.toList(childrenAsPropertyResource.listChildren());

        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i).getName(), actual.get(i).getName());
        }
    }

    @Test
    public void testGet_Sorted() throws Exception {
        valueMap.put("animals", unsortedJSON.toString());

        List<Resource> expected = new ArrayList<Resource>();
        expected.add(new SyntheticChildAsPropertyResource(resource, "entry-1", new ValueMapDecorator(entry1)));
        expected.add(new SyntheticChildAsPropertyResource(resource, "entry-2", new ValueMapDecorator(entry2)));
        expected.add(new SyntheticChildAsPropertyResource(resource, "entry-3", new ValueMapDecorator(entry3)));

        childrenAsPropertyResource =
                new ChildrenAsPropertyResource(resource, "animals",
                        ChildrenAsPropertyResource.RESOURCE_NAME_COMPARATOR);;

        List<Resource> actual = IteratorUtils.toList(childrenAsPropertyResource.listChildren());

        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i).getName(), actual.get(i).getName());
        }
    }
}