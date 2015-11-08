package com.adobe.acs.commons.wcm.impl;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PropertyMergePostProcessorTest {

    @InjectMocks
    PropertyMergePostProcessor propertyMerge = new PropertyMergePostProcessor();

    @Mock
    Resource resource;

    @Mock
    ResourceResolver resourceResolver;

    ModifiableValueMap properties = new ModifiableValueMapDecorator(new HashMap<String, Object>());

    @Before
    public void setUp() throws Exception {
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        doNothing().when(resourceResolver).commit();
    }

    @Test
    public void testMerge_NoDuplicates_String() throws Exception {

        properties.put("cats", new String[]{ "felix", "hobbes", "fluffy" });
        properties.put("dogs", new String[]{ "snoopy", "ira", "fluffy" });
        properties.put("fish", "nemo");

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "animals",
                Arrays.asList("cats", "dogs", "fish"),
                String.class,
                false);

        Assert.assertArrayEquals(new String[]{ "felix", "hobbes", "fluffy", "snoopy", "ira", "nemo" },
                properties.get("animals", String[].class));
    }

    @Test
    public void testMerge_NoDuplicates_Long() throws Exception {

        properties.put("odd", new Long[]{ 1L, 3L });
        properties.put("even", new Long[]{ 2L, 4L });
        properties.put("duplicates", new Long[]{ 1L, 2L, 3L, 4L });


        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "longs",
                Arrays.asList("even", "odd", "duplicates"),
                Long.class, 
                false);

        Assert.assertArrayEquals(new Long[]{ 2L, 4L, 1L, 3L },
                properties.get("longs", Long[].class));
    }

    @Test
    public void testMerge_NoDuplicates_Double() throws Exception {

        properties.put("tenths", new Double[]{ 1.1D, 1.2D });
        properties.put("hundredths", 3.01D);
        properties.put("duplicates", new Double[]{ 1.1D });


        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "doubles",
                Arrays.asList("tenths", "hundredths", "duplicates"),
                Double.class,
                false);

        Assert.assertArrayEquals(new Double[]{ 1.1D, 1.2D, 3.01D },
                properties.get("doubles", Double[].class));
    }

    @Test
    public void testMerge_NoDuplicates_Boolean() throws Exception {

        properties.put("first", new Boolean[]{ true, false, true });
        properties.put("second", true);


        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "booleans",
                Arrays.asList("first", "second"),
                Boolean.class, 
                false);

        Assert.assertArrayEquals(new Boolean[]{ true, false },
                properties.get("booleans", Boolean[].class));
    }

    @Test
    public void testMerge_NoDuplicates_Calendar() throws Exception {

        Calendar january =  Calendar.getInstance();
        january.set(2015, Calendar.JANUARY, 1);

        Calendar july =  Calendar.getInstance();
        july.set(2015, Calendar.JULY, 4);

        Calendar september =  Calendar.getInstance();
        september.set(2015, Calendar.SEPTEMBER, 16);

        properties.put("cold", new Calendar[] { january, september });
        properties.put("hot", new Calendar[]{ july, september });

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "dates",
                Arrays.asList("cold", "hot"),
                Calendar.class,
                false);

        Assert.assertArrayEquals(new Calendar[]{ january, september, july },
                properties.get("dates", Calendar[].class));
    }

    @Test
    public void testMerge_Duplicates_String() throws Exception {

        properties.put("cats", new String[]{ "felix", "hobbes", "fluffy" });
        properties.put("dogs", new String[]{ "snoopy", "ira", "fluffy" });
        properties.put("fish", "nemo");

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "animals",
                Arrays.asList("cats", "dogs", "fish"),
                String.class,
                true);

        Assert.assertArrayEquals(new String[]{ "felix", "hobbes", "fluffy", "snoopy", "ira", "fluffy", "nemo" },
                properties.get("animals", String[].class));
    }

    @Test
    public void testMerge_Duplicates_Calendar() throws Exception {

        Calendar january =  Calendar.getInstance();
        january.set(2015, Calendar.JANUARY, 1);

        Calendar july =  Calendar.getInstance();
        july.set(2015, Calendar.JULY, 4);

        Calendar september =  Calendar.getInstance();
        september.set(2015, Calendar.SEPTEMBER, 16);

        properties.put("cold", new Calendar[] { january, september });
        properties.put("hot", new Calendar[]{ july, september });

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "dates",
                Arrays.asList("cold", "hot"),
                Calendar.class,
                true);

        Assert.assertArrayEquals(new Calendar[]{ january, september, july, september },
                properties.get("dates", Calendar[].class));
    }
}


