/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.rewriter.impl;

import junit.framework.TestCase;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResourceResolverMapTransformerFactoryTest extends TestCase {
    @Test
    public void activate() throws Exception {
    }

    @Mock
    SlingHttpServletRequest request;
    
    @Mock
    ResourceResolver resourceResolver;
    
    @Mock
    private ContentHandler handler;
    
    @Mock
    ProcessingContext processingContext;

    @Captor
    private ArgumentCaptor<Attributes> attributesCaptor;
    
    @Before
    public void setUp() throws Exception {
        when(processingContext.getRequest()).thenReturn(request);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
    }

    @Test
    public void testRebuildAttributes() throws Exception {
        when(resourceResolver.map(request, "/content/site/en/jcr:content/img.png")).thenReturn("/en/jcr:content/img.png");

        final Map<String, Object> config = new HashMap<String, Object>();
        config.put("attributes", new String[]{"img:src"});

        ResourceResolverMapTransformerFactory factory = new ResourceResolverMapTransformerFactory();

        factory.activate(config);
        Transformer transformer = factory.createTransformer();
        transformer.init(processingContext, null);
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "src", null, "CDATA", "/content/site/en/jcr:content/img.png");

        /* Execute */
        
        transformer.startElement(null, "img", null, in);
        
        /* Verify */
        
        verify(handler, only()).startElement(isNull(), eq("img"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("/en/jcr:content/img.png", out.getValue(0));
    }



    @Test
    public void testRebuildAttributes_NegativeScenario() throws Exception {
        final Map<String, Object> config = new HashMap<String, Object>();
        config.put("attributes", new String[]{"img:data-src,src"});

        ResourceResolverMapTransformerFactory factory = new ResourceResolverMapTransformerFactory();

        factory.activate(config);
        Transformer transformer = factory.createTransformer();
        transformer.init(processingContext, null);
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "data-uri", null, "CDATA", "/img.png");

        /* Execute */

        transformer.startElement(null, "img", null, in);
        
        /* Verify */

        verify(handler, only()).startElement(isNull(), eq("img"), isNull(),
                attributesCaptor.capture());
        verifyNoInteractions(resourceResolver);
    }

    @Test
    public void testRebuildAttributes_DoubleEncodingScenario() throws Exception {
        when(resourceResolver.map(request, "/content/site/en/jcr:content/img test.png")).thenReturn("/en/jcr:content/img%20test.png");

        final Map<String, Object> config = new HashMap<String, Object>();
        config.put("attributes", new String[]{"img:src"});

        ResourceResolverMapTransformerFactory factory = new ResourceResolverMapTransformerFactory();

        factory.activate(config);
        Transformer transformer = factory.createTransformer();
        transformer.init(processingContext, null);
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "src", null, "CDATA", "/content/site/en/jcr:content/img%20test.png");

        /* Execute */

        transformer.startElement(null, "img", null, in);

        /* Verify */

        verify(handler, only()).startElement(isNull(), eq("img"), isNull(),
                                             attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("/en/jcr:content/img%20test.png", out.getValue(0));
    }

    @Test
    public void testActivate_Array() throws NoSuchFieldException, IllegalAccessException {
        ResourceResolverMapTransformerFactory factory = new ResourceResolverMapTransformerFactory();

        Field field = factory.getClass().getDeclaredField("attributes");
        field.setAccessible(true);

        Map<String, Object> config = new HashMap<String, Object>();
        config.put("attributes", new String[]{"a:b", "c:d"});

        factory.activate(config);

        assertEquals(2, ((Map)field.get(factory)).size());
    }

    @Test
    public void testActivate_String() throws NoSuchFieldException, IllegalAccessException {
        ResourceResolverMapTransformerFactory factory = new ResourceResolverMapTransformerFactory();

        Field field = factory.getClass().getDeclaredField("attributes");
        field.setAccessible(true);

        Map<String, Object> config = new HashMap<String, Object>();
        config.put("attributes", "a:b,c:d");

        factory.activate(config);

        assertEquals(2, ((Map)field.get(factory)).size());
    }
}