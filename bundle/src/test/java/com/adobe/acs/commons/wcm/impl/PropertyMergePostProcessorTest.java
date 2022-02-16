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
package com.adobe.acs.commons.wcm.impl;

import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import java.util.ArrayList;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;

@RunWith(MockitoJUnitRunner.class)
public class PropertyMergePostProcessorTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @InjectMocks
    PropertyMergePostProcessor propertyMerge = new PropertyMergePostProcessor();

    @Mock
    Resource resource;

    @Mock
    ResourceResolver resourceResolver;

    ModifiableValueMap properties = new ModifiableValueMapDecorator(new HashMap<>());

    @Before
    public void setUp() throws Exception {
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
    }

    @Test
    public void testMerge_NoDuplicates_String() throws Exception {

        properties.put("cats", new String[]{"felix", "hobbes", "fluffy"});
        properties.put("dogs", new String[]{"snoopy", "ira", "fluffy"});
        properties.put("fish", "nemo");

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "animals",
                Arrays.asList("cats", "dogs", "fish"),
                String.class,
                false);

        Assert.assertArrayEquals(new String[]{"felix", "hobbes", "fluffy", "snoopy", "ira", "nemo"},
                properties.get("animals", String[].class));
    }

    @Test
    public void testMerge_NoDuplicates_Long() throws Exception {

        properties.put("odd", new Long[]{1L, 3L});
        properties.put("even", new Long[]{2L, 4L});
        properties.put("duplicates", new Long[]{1L, 2L, 3L, 4L});

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "longs",
                Arrays.asList("even", "odd", "duplicates"),
                Long.class,
                false);

        Assert.assertArrayEquals(new Long[]{2L, 4L, 1L, 3L},
                properties.get("longs", Long[].class));
    }

    @Test
    public void testMerge_NoDuplicates_Double() throws Exception {

        properties.put("tenths", new Double[]{1.1D, 1.2D});
        properties.put("hundredths", 3.01D);
        properties.put("duplicates", new Double[]{1.1D});

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "doubles",
                Arrays.asList("tenths", "hundredths", "duplicates"),
                Double.class,
                false);

        Assert.assertArrayEquals(new Double[]{1.1D, 1.2D, 3.01D},
                properties.get("doubles", Double[].class));
    }

    @Test
    public void testMerge_NoDuplicates_Boolean() throws Exception {

        properties.put("first", new Boolean[]{true, false, true});
        properties.put("second", true);

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "booleans",
                Arrays.asList("first", "second"),
                Boolean.class,
                false);

        Assert.assertArrayEquals(new Boolean[]{true, false},
                properties.get("booleans", Boolean[].class));
    }

    @Test
    public void testMerge_NoDuplicates_Calendar() throws Exception {

        Calendar january = Calendar.getInstance();
        january.set(2015, Calendar.JANUARY, 1);

        Calendar july = Calendar.getInstance();
        july.set(2015, Calendar.JULY, 4);

        Calendar september = Calendar.getInstance();
        september.set(2015, Calendar.SEPTEMBER, 16);

        properties.put("cold", new Calendar[]{january, september});
        properties.put("hot", new Calendar[]{july, september});

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "dates",
                Arrays.asList("cold", "hot"),
                Calendar.class,
                false);

        Assert.assertArrayEquals(new Calendar[]{january, september, july},
                properties.get("dates", Calendar[].class));
    }

    @Test
    public void testMerge_Duplicates_String() throws Exception {

        properties.put("cats", new String[]{"felix", "hobbes", "fluffy"});
        properties.put("dogs", new String[]{"snoopy", "ira", "fluffy"});
        properties.put("fish", "nemo");

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "animals",
                Arrays.asList("cats", "dogs", "fish"),
                String.class,
                true);

        Assert.assertArrayEquals(new String[]{"felix", "hobbes", "fluffy", "snoopy", "ira", "fluffy", "nemo"},
                properties.get("animals", String[].class));
    }

    @Test
    public void testMerge_Duplicates_Calendar() throws Exception {

        Calendar january = Calendar.getInstance();
        january.set(2015, Calendar.JANUARY, 1);

        Calendar july = Calendar.getInstance();
        july.set(2015, Calendar.JULY, 4);

        Calendar september = Calendar.getInstance();
        september.set(2015, Calendar.SEPTEMBER, 16);

        properties.put("cold", new Calendar[]{january, september});
        properties.put("hot", new Calendar[]{july, september});

        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        propertyMerge.merge(resource,
                "dates",
                Arrays.asList("cold", "hot"),
                Calendar.class,
                true);

        Assert.assertArrayEquals(new Calendar[]{january, september, july, september},
                properties.get("dates", Calendar[].class));
    }

    @Test
    public void testTagDetection() {
        Assert.assertTrue("Valid root tag detection", PropertyMergePostProcessor.looksLikeTag("Some_Root:"));
        Assert.assertTrue("Valid root tag detection", PropertyMergePostProcessor.looksLikeTag("Some_Root:Tag"));
        Assert.assertTrue("Valid sub tag detection", PropertyMergePostProcessor.looksLikeTag("Some_Root-123:Tag/Another_Tag456"));
        Assert.assertFalse("Invalid tag pattern", PropertyMergePostProcessor.looksLikeTag("Some_Root123"));
        Assert.assertFalse("Spaces check 1", PropertyMergePostProcessor.looksLikeTag("Some Root:Tag"));
        Assert.assertFalse("Spaces check 2", PropertyMergePostProcessor.looksLikeTag("Some_Root : Tag"));
        Assert.assertFalse("Spaces check 3", PropertyMergePostProcessor.looksLikeTag("Some_Root:Tag Name"));
        Assert.assertFalse("Spaces check 4", PropertyMergePostProcessor.looksLikeTag("Some_Root:Tag/Other Tag Name"));
    }

    @Test
    public void testPropertyPathAlignment() {
        // Test based on what is actually posted when bulk editing assets
        Assert.assertEquals("./asset-share-commons/en/public/pictures/stacey-rozells-288200.jpg/jcr:content/metadata/dam:tags-merged",
                PropertyMergePostProcessor.alignDestinationPath(
                        "./asset-share-commons/en/public/pictures/stacey-rozells-288200.jpg/jcr:content/metadata/dam:tag1",
                        "jcr:content/metadata/dam:tags-merged"));

        // In the event someone were posting to the asset for updating its metadata, this should still work
        Assert.assertEquals("jcr:content/metadata/dam:tags-merged",
                PropertyMergePostProcessor.alignDestinationPath(
                        "jcr:content/metadata/dam:tag1",
                        "jcr:content/metadata/dam:tags-merged"));

        // Should not break basic cases
        Assert.assertEquals("PathB",
                PropertyMergePostProcessor.alignDestinationPath(
                        "PathA",
                        "PathB"));
    }

    @Test
    public void testMergeAllTags() throws Exception {

        final TagManager mockTagManager = mock(TagManager.class);
        Tag fakeTag = mock(Tag.class);
        when(mockTagManager.resolve(any())).thenReturn(fakeTag);

        context.registerAdapter(ResourceResolver.class, TagManager.class, mockTagManager);

        ResourceResolver rr = context.resourceResolver();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(new HashMap<String, Object>() {
            {
                put("./asset/jcr:content/metadata/dam:tag1", new String[]{
                    "tag1:tag1a",
                    "tag1:tag1b"
                });
                put("./asset/jcr:content/metadata/dam:tag2", new String[]{
                    "tag2:tag2a",
                    "tag2:tag2b"
                });
                put(":" + PropertyMergePostProcessor.OPERATION_ALL_TAGS + "@PropertyMerge", "jcr:content/metadata/dam:combined-tags");
            }
        });

        Map<String, Object> emptyProperties = new HashMap<>();
        Resource content = rr.create(rr.resolve("/"), "content", emptyProperties);
        Resource dam = rr.create(content, "dam", emptyProperties);
        request.setResource(dam);
        Resource asset = rr.create(dam, "asset", emptyProperties);
        Resource jcrContent = rr.create(asset, "jcr:content", emptyProperties);
        Resource metadata = rr.create(jcrContent, "metadata", new HashMap<String, Object>() {
            {
                put("dam:tag1", new String[]{"tag1:tag1a", "tag1:tag1b"});
                put("dam:tag2", new String[]{"tag2:tag2a", "tag2:tag2b"});
            }
        });

        PropertyMergePostProcessor processor = new PropertyMergePostProcessor();
        List<Modification> changeLog = new ArrayList<>();
        processor.process(request, changeLog);
        Assert.assertFalse("Should have observed some changes", changeLog.isEmpty());
        String[] tags = metadata.getValueMap().get("dam:combined-tags", String[].class);
        Assert.assertArrayEquals(new String[]{"tag1:tag1a", "tag1:tag1b", "tag2:tag2a", "tag2:tag2b"}, tags);

    }
}
