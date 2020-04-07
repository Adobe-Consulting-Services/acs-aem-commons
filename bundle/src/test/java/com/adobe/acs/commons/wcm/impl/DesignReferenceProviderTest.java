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
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.reference.Reference;

@RunWith(MockitoJUnitRunner.class)
public class DesignReferenceProviderTest {

    private DesignReferenceProvider instance;

    @Mock
    private Page page;

    @Mock
    private Design design;

    @Mock
    private Designer designer;

    @Mock
    private PageManager pageManager;

    @Mock
    private Resource resource;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource designResource;

    @Before
    public void setUp() throws Exception {
        instance = new DesignReferenceProvider();
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(resolver.adaptTo(Designer.class)).thenReturn(designer);

        when(pageManager.getContainingPage(resource)).thenReturn(page);
        
        when(design.getContentResource()).thenReturn(designResource);
        when(design.getId()).thenReturn("test");
        Calendar cal = Calendar.getInstance();
        when(design.getLastModified()).thenReturn(cal);

    }

    @Test
    public void testfindReferencesWithDesignPathinResource() {
        when(designer.getDesign(page)).thenReturn(design);

        List<Reference> ref = instance.findReferences(resource);
        assertNotNull(ref);
        assertEquals(1, ref.size());
        assertEquals(designResource, ref.get(0).getResource());
        assertEquals("designpage", ref.get(0).getType());
        assertEquals("test (Design)", ref.get(0).getName());
    }

    @Test
    public void testfindReferencesWithNoDesignPathinResource() {
        List<Reference> ref = instance.findReferences(resource);
        assertNotNull(ref);
        assertEquals(0, ref.size());
    }

}