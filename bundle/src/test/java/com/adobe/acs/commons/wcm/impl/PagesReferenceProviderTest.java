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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.reference.Reference;

@RunWith(MockitoJUnitRunner.class)
public class PagesReferenceProviderTest {

    @InjectMocks
    private PagesReferenceProvider instance = new PagesReferenceProvider();

    @Mock
    private Resource resource;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource res;
    @Mock
    private Resource res1;

    @Mock
    private Page referredpage;

    @Mock
    private Page referredpage1;

    @Mock
    private PageManager manager;

    @Mock
    private Page page;

    @Mock
    private Iterator<Resource> iter;

    @Before
    public void setUp() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        String path = "/content/geometrixx/en";
        map.put("path", path);
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resolver.adaptTo(PageManager.class)).thenReturn(manager);
        when(manager.getContainingPage(path)).thenReturn(referredpage);
        when(referredpage.getPath()).thenReturn(path);
        when(resource.listChildren()).thenReturn(iter);
        when(iter.hasNext()).thenReturn(false);
        when(resolver.getResource(path)).thenReturn(res);
        when(res.adaptTo(Page.class)).thenReturn(referredpage);
        when(referredpage.getName()).thenReturn("geometrixx");
        Calendar cal = GregorianCalendar.getInstance();
        when(referredpage.getLastModified()).thenReturn(cal);
    }

    @Test
    public void testSingleReferenceToaPage() throws Exception {
        List<Reference> actual = instance.findReferences(resource);
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("geometrixx (Page)", actual.get(0).getName());

    }

    @Test
    public void testNoReferenceToAnyPage() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        String path = "/content/ge1ometrixx/en";
        map.put("path", path);
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resolver.getResource(path)).thenReturn(res);
        when(res.adaptTo(Page.class)).thenReturn(null);
        when(referredpage.getName()).thenReturn("geometrixx");
        Calendar cal = GregorianCalendar.getInstance();
        when(referredpage.getLastModified()).thenReturn(cal);

        List<Reference> actual = instance.findReferences(resource);
        assertNotNull(actual);
        assertEquals(0, actual.size());
    }

    @Test
    public void testSingleReferenceToManyPages() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        String path = "/content/geometrixx/en";
        String path1 = "/content/geometrixx/en/toolbar";
        map.put("path", path);
        map.put("path1", path1);
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resolver.getResource(path)).thenReturn(res);
        when(resolver.getResource(path1)).thenReturn(res1);
        when(res.adaptTo(Page.class)).thenReturn(referredpage);
        when(res1.adaptTo(Page.class)).thenReturn(referredpage1);
        when(referredpage.getName()).thenReturn("geometrixx");
        when(referredpage1.getName()).thenReturn("geometrixx1");
        when(referredpage1.getPath()).thenReturn(path1);
        when(manager.getContainingPage(path1)).thenReturn(referredpage1);
        Calendar cal = GregorianCalendar.getInstance();
        when(referredpage.getLastModified()).thenReturn(cal);
        when(referredpage1.getLastModified()).thenReturn(cal);
        List<Reference> actual = instance.findReferences(resource);

        assertNotNull(actual);
        assertEquals(2, actual.size());

        boolean geometrixxFound = false;
        boolean geometrixxOneFound = false;
        for (Reference ref : actual) {
            if (ref.getName().equals("geometrixx (Page)")) {
                geometrixxFound = true;
            } else if (ref.getName().equals("geometrixx1 (Page)")) {
                geometrixxOneFound = true;
            }
        }

        assertTrue(geometrixxFound);
        assertTrue(geometrixxOneFound);

    }

    @Test
    public void testManyReferenceToSinglePages() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        String path = "/content/geometrixx/en";
        String path1 = "/content/geometrixx/en";
        map.put("path", path);
        map.put("path1", path1);
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resolver.getResource(path)).thenReturn(res);
        when(resolver.getResource(path1)).thenReturn(res1);
        when(res.adaptTo(Page.class)).thenReturn(referredpage);
        when(res1.adaptTo(Page.class)).thenReturn(referredpage);
        when(referredpage.getName()).thenReturn("geometrixx");
        when(referredpage1.getName()).thenReturn("geometrixx");
        when(referredpage1.getPath()).thenReturn(path1);
        when(manager.getContainingPage(path1)).thenReturn(referredpage1);
        Calendar cal = GregorianCalendar.getInstance();
        when(referredpage.getLastModified()).thenReturn(cal);
        when(referredpage1.getLastModified()).thenReturn(cal);
        List<Reference> actual = instance.findReferences(resource);

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("geometrixx (Page)", actual.get(0).getName());

    }

    @Test
    public void testMultipleReferencesReferenceToPages() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        String path = "\"/content/geometrixx/en\",\"/content/geometrixx/en/toolbar\"";
        String path1 = "/content/geometrixx/en/toolbar";
        map.put("path", path);
        // map.put("path1", path1);
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resolver.getResource(path)).thenReturn(res);
        when(resolver.getResource(path1)).thenReturn(res1);
        when(res.adaptTo(Page.class)).thenReturn(referredpage);
        when(res1.adaptTo(Page.class)).thenReturn(referredpage1);
        when(referredpage.getName()).thenReturn("geometrixx");
        when(referredpage1.getName()).thenReturn("geometrixx1");
        when(referredpage1.getPath()).thenReturn(path1);
        when(manager.getContainingPage(path1)).thenReturn(referredpage1);
        Calendar cal = GregorianCalendar.getInstance();
        when(referredpage.getLastModified()).thenReturn(cal);
        when(referredpage1.getLastModified()).thenReturn(cal);
        List<Reference> actual = instance.findReferences(resource);

        assertNotNull(actual);
        assertEquals(2, actual.size());

        boolean geometrixxFound = false;
        boolean geometrixxOneFound = false;
        for (Reference ref : actual) {
            if (ref.getName().equals("geometrixx (Page)")) {
                geometrixxFound = true;
            } else if (ref.getName().equals("geometrixx1 (Page)")) {
                geometrixxOneFound = true;
            }
        }

        assertTrue(geometrixxFound);
        assertTrue(geometrixxOneFound);

    }
}
