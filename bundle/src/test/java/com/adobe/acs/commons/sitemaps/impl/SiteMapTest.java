/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.sitemaps.impl;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static junit.framework.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;

public class SiteMapTest {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
       
    }

    @Test
    public void testIterator() throws Exception {
        final Page page = mock(Page.class);
        final Page pageChild1 = mock(Page.class);
        PageFilter filter = new PageFilter();
        boolean considerPageFilter = false;
        boolean donotConsiderPageFilter = true;
        SiteMap siteMap = new SiteMap(page, filter, considerPageFilter);
        
        Page[] childPages = new Page[1];
        childPages[0] = pageChild1;
        Iterator<Page> pages = Arrays.asList(childPages).iterator();
        when(page.listChildren()).thenReturn(pages);
        when(pageChild1.listChildren()).thenReturn(new ArrayList<Page>().iterator());
       
        Map<String, Object> map =  new HashMap<String, Object>();
        map.put("priority", "0.5");
        ValueMap map1 = new ValueMapDecorator(map);
        when(page.adaptTo(ValueMap.class)).thenReturn(map1);
        when(pageChild1.adaptTo(ValueMap.class)).thenReturn(map1);
        when(page.getLastModified()).thenReturn(GregorianCalendar.getInstance());
        when(pageChild1.getLastModified()).thenReturn(GregorianCalendar.getInstance());
        when(page.getPath()).thenReturn("/content/geometrixx/en");
        when(pageChild1.getPath()).thenReturn("/content/geometrixx/en/social");
        Iterator<SiteMap.LinkElement> linksIter =  siteMap.iterator();
       assertNotNull(linksIter);
       
    }
}
