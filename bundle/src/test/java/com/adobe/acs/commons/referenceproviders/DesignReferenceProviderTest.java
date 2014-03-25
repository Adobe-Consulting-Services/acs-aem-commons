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
package com.adobe.acs.commons.referenceproviders;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
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
import com.day.cq.wcm.api.reference.Reference;
 
 @RunWith(MockitoJUnitRunner.class)
 public class DesignReferenceProviderTest {
     @InjectMocks
     private DesignReferenceProvider instance;
     
     @Mock
     private Resource resource;
     
     
     @Mock
     private ResourceResolver resolver;
     
     @Mock
     private Resource designResource;
     
     @Mock
     private Page designPage;
     
     @Before
     public void setUp() throws Exception {
         instance = new DesignReferenceProvider();
         Map<String, Object> map = new HashMap<String, Object>();
         map.put("cq:designPath", "/etc/designs/geometrixx");
         ValueMap vm =  new ValueMapDecorator(map);
         when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
         when(resource.getResourceResolver()).thenReturn(resolver);
         when(resolver.getResource("/etc/designs/geometrixx")).thenReturn(designResource);
         when(designResource.adaptTo(Page.class)).thenReturn(designPage);
         Calendar cal = GregorianCalendar.getInstance();
         when(designPage.getLastModified()).thenReturn(cal);
         
     }
     
     @Test
     public void testfindReferencesWithDesignPathinResource() {
        
        List<Reference> ref = instance.findReferences(resource);
        assertEquals(1, ref.size());
        assertEquals(false, ref==null);
      }
     @Test
     public void testfindReferencesWithNoDesignPathinResource() {
         Map<String, Object> map = new HashMap<String, Object>();
         map.put("cq:designPath", "");
         ValueMap vm =  new ValueMapDecorator(map);
         when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
        List<Reference> ref = instance.findReferences(resource);
        assertEquals(0, ref.size());
        assertEquals(false, ref==null);
      }
     
 }