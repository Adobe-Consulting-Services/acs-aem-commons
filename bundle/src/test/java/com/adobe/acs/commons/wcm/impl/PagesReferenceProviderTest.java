/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.reference.Reference;
import com.google.common.base.Function;

@RunWith(MockitoJUnitRunner.class)
public class PagesReferenceProviderTest {

    @Rule
    public SlingContext context = new SlingContext();

    private PagesReferenceProvider instance;

    @Mock
    private PageManager pageManager;

    @Before
    public void setUp() throws Exception {
        instance = new PagesReferenceProvider();
        instance.activate(Collections.emptyMap());
        context.registerAdapter(ResourceResolver.class, PageManager.class, new Function<ResourceResolver, PageManager>() {
            @Nullable
            @Override
            public PageManager apply(@Nullable ResourceResolver input) {
                return pageManager;
            }
        });

        context.load().json(getClass().getResourceAsStream("PagesReferenceProviderTest.json"), "/content/geometrixx");

        registerPage("/content/geometrixx/en", "geometrixx");
        registerPage("/content/geometrixx/en/toolbar", "geometrixx1");
        registerPage("/content/geometrixx/reftoself", "to self");
    }

    @Test
    public void testSingleReferenceToaPage() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/oneref/jcr:content"));
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("geometrixx (Page)", actual.get(0).getName());

    }

    @Test
    public void testNoReferenceToAnyPage() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/noref/jcr:content"));
        assertNotNull(actual);
        assertEquals(0, actual.size());
    }

    @Test
    public void testReferenceToMissingPage() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/badref/jcr:content"));
        assertNotNull(actual);
        assertEquals(0, actual.size());
    }

    @Test
    public void testSingleReferenceToManyPages() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/tworefs/jcr:content"));

        assertNotNull(actual);
        assertEquals(2, actual.size());

        assertArrayEquals(new String[] { "geometrixx (Page)", "geometrixx1 (Page)" }, toArrayOfNames(actual));
    }

    @Test
    public void testReferenceOnChildNode() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/tworefsChild/jcr:content"));

        assertNotNull(actual);
        assertEquals(2, actual.size());

        assertArrayEquals(new String[] { "geometrixx (Page)", "geometrixx1 (Page)" }, toArrayOfNames(actual));
    }

    @Test
    public void testManyReferenceToSinglePages() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/tworefsSamePage/jcr:content"));

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("geometrixx (Page)", actual.get(0).getName());

    }

    @Test
    public void testMultiValuedProp() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/mvRef/jcr:content"));

        assertNotNull(actual);
        assertEquals(2, actual.size());

        assertArrayEquals(new String[] { "geometrixx (Page)", "geometrixx1 (Page)" }, toArrayOfNames(actual));
    }

    @Test
    public void testMultiValuedPropWithOther() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/mvRefSamePage/jcr:content"));

        assertNotNull(actual);
        assertEquals(2, actual.size());

        assertArrayEquals(new String[] { "geometrixx (Page)", "geometrixx1 (Page)" }, toArrayOfNames(actual));
    }

    @Test
    public void testMultipleReferencesReferenceToPages() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/commaSeparated/jcr:content"));

        assertNotNull(actual);
        assertEquals(2, actual.size());

        assertArrayEquals(new String[] { "geometrixx (Page)", "geometrixx1 (Page)" }, toArrayOfNames(actual));

    }

    @Test
    public void testRefToSelf() throws Exception {
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/reftoself/jcr:content"));

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("geometrixx (Page)", actual.get(0).getName());

    }

    @Test
    public void testPageReferenceResourcePath() throws Exception {
        // The references resource should point to the cq:Page and not the [cq:Pages]/jcr:content per https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues/1283
        List<Reference> actual = instance.findReferences(context.resourceResolver().getResource("/content/geometrixx/oneref/jcr:content"));
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("/content/geometrixx/en", actual.get(0).getResource().getPath());
    }

    private Page registerPage(String path, String name) {
        Page result = mock(Page.class, path);
        when(pageManager.getContainingPage(path)).thenReturn(result);
        when(result.getName()).thenReturn(name);
        when(result.getLastModified()).thenReturn(Calendar.getInstance());
        when(result.getContentResource()).then(i -> context.resourceResolver().getResource(path + "/jcr:content"));
        return result;
    }

    private Object[] toArrayOfNames(List<Reference> actual) {
        return actual.stream().map(p -> p.getName()).sorted().collect(Collectors.toList()).toArray();
    }
}
