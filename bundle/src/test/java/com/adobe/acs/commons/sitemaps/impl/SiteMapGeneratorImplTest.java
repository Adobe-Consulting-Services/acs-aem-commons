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
package com.adobe.acs.commons.sitemaps.impl;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;

import com.adobe.acs.commons.sitemaps.SiteMapGenerator;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class SiteMapGeneratorImplTest {
    @Mock
    private Externalizer externalizer;
    @InjectMocks
    private SiteMapGenerator siteMapGenerator = new SiteMapGeneratorImpl();
    
    @Test
    public void testGetSiteMap() throws Exception
    {
        final ResourceResolver resolver = mock(ResourceResolver.class);
        PageManager manager = mock(PageManager.class);
        when(resolver.adaptTo(PageManager.class)).thenReturn(manager);
        Page page = mock(Page.class);
        when(manager.getPage("/content/geometrixx/en")).thenReturn(page);
        final Page pageChild1 = mock(Page.class);
        Page[] childPages = new Page[1];
        childPages[0] = pageChild1;
        Iterator<Page> pages = Arrays.asList(childPages).iterator();
        when(page.listChildren()).thenReturn(pages);
        when(pageChild1.listChildren()).thenReturn(new ArrayList<Page>().iterator());
        Map<String, Object> map =  new HashMap<String, Object>();
        map.put("priority", "0.5");
        ValueMap map1 = new ValueMapDecorator(map);
        Calendar cal = GregorianCalendar.getInstance();
        when(page.getProperties()).thenReturn(map1);
        when(pageChild1.getProperties()).thenReturn(map1);
        when(page.getLastModified()).thenReturn(cal);
        when(pageChild1.getLastModified()).thenReturn(cal);
        when(page.getPath()).thenReturn("/content/geometrixx/en");
        when(pageChild1.getPath()).thenReturn("/content/geometrixx/en/social");
    
        when(externalizer.externalLink(resolver,"localhost","/content/geometrixx/en.html")).thenReturn("http://localhost/content/geometrixx/en.html");
        when(externalizer.externalLink(resolver,"localhost","/content/geometrixx/en/social.html")).thenReturn("http://localhost/content/geometrixx/en/social.html");
        Document sitemapDocument = siteMapGenerator.getSiteMap(resolver);
        String expected ="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\"><url><loc>http://localhost/content/geometrixx/en.html</loc><lastmod>"+getDateAsString(cal.getTime(),"yyyy-MM-dd'T'hh:mm:ss XXX")+"</lastmod><changefreq>daily</changefreq><priority>0.5</priority></url><url><loc>http://localhost/content/geometrixx/en/social.html</loc><lastmod>"+getDateAsString(cal.getTime(),"yyyy-MM-dd'T'hh:mm:ss XXX")+"</lastmod><changefreq>daily</changefreq><priority>0.5</priority></url></urlset>";
      
        assertEquals(expected, getStringFromDocument(sitemapDocument).trim());
        
    }
    
  
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        reset(externalizer);
        
    }
    public String getStringFromDocument(Document doc)
    {
        try
        {
           DOMSource domSource = new DOMSource(doc);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           transformer.transform(domSource, result);
           return writer.toString();
        }
        catch(TransformerException ex)
        {
           ex.printStackTrace();
           return null;
        }
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
