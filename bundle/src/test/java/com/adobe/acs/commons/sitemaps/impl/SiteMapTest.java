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

import static junit.framework.Assert.assertNotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
        SiteMap siteMap = new SiteMap(page, filter, considerPageFilter);
        
        Page[] childPages = new Page[1];
        childPages[0] = pageChild1;
        Iterator<Page> pages = Arrays.asList(childPages).iterator();
        when(page.listChildren()).thenReturn(pages);
        when(pageChild1.listChildren()).thenReturn(new ArrayList<Page>().iterator());
       
        Map<String, Object> map =  new HashMap<String, Object>();
        map.put("priority", "0.5");
        ValueMap map1 = new ValueMapDecorator(map);
        when(page.getProperties()).thenReturn(map1);
        when(pageChild1.getProperties()).thenReturn(map1);
        Calendar cal =GregorianCalendar.getInstance();
        when(page.getLastModified()).thenReturn(cal);
        when(pageChild1.getLastModified()).thenReturn(cal);
        when(page.getPath()).thenReturn("/content/geometrixx/en");
        when(pageChild1.getPath()).thenReturn("/content/geometrixx/en/social");
        Iterator<SiteMap.LinkElement> linksIter =  siteMap.iterator();
        SiteMap.LinkElement[] ale = new SiteMap.LinkElement[2];
        ale[0] = new SiteMap.LinkElement("/content/geometrixx/en", getDateAsString(cal.getTime(),"yyyy-MM-dd'T'hh:mm:ss XXX"), "daily", "0.5");
        ale[1] = new SiteMap.LinkElement("/content/geometrixx/en/social", getDateAsString(cal.getTime(),"yyyy-MM-dd'T'hh:mm:ss XXX"), "daily", "0.5");
        Iterator<SiteMap.LinkElement> expected = Arrays.asList(ale).iterator();
       assertNotNull(linksIter);
    }
    private String getDateAsString(Date date,String format) {
        String dateStr = "";
         try {
             SimpleDateFormat sdf = new SimpleDateFormat(format);
           
             dateStr = sdf.format(date);
         } catch (Exception e) {
            
         }
         return dateStr;
     }
}
