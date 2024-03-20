/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.rewriter.impl;


import com.adobe.acs.commons.ccvar.ContextualContentVariableTestUtil;
import com.adobe.acs.commons.ccvar.impl.AllPagePropertiesContentVariableProvider;
import com.adobe.acs.commons.ccvar.impl.PropertyAggregatorServiceImpl;
import com.adobe.acs.commons.ccvar.impl.PropertyConfigServiceImpl;
import com.adobe.acs.commons.ccvar.impl.UrlEncodeAction;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import io.wcm.testing.mock.aem.junit.AemContext;

import java.util.Map;

import static com.adobe.acs.commons.ccvar.ContextualContentVariableTestUtil.defaultService;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContentVariableTransformerFactoryTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Mock
    private ContentHandler handler;

    @Mock
    private ProcessingContext processingContext;

    private TransformerFactory transformerFactory;
    private Transformer transformer;

    @Before
    public void setup() throws Exception {
        context.load().json(getClass().getResourceAsStream("ContentVariableTransformerContent.json"), "/content/we-retail/language-masters/en/experience");

        MockSlingHttpServletRequest request = context.request();
        request.setResource(context.resourceResolver().getResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/responsivegrid/text"));
        when(processingContext.getRequest()).thenReturn(context.request());
    }

    @Test
    public void testSingleReplacement() throws Exception {
        reinitTransformer(true);

        String input = "((page_properties.jcr:title))";
        String output = "Arctic Surfing In Lofoten";
        transformer.characters(input.toCharArray(), 0, input.length());

        ArgumentCaptor<char[]> charCaptor = ArgumentCaptor.forClass(char[].class);
        ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(handler, atLeast(1)).characters(charCaptor.capture(), eq(0), lengthCaptor.capture());

        String outputTitle = new String(charCaptor.getValue());
        assertEquals(output, outputTitle);
        assertEquals(Integer.valueOf(output.length()), lengthCaptor.getValue());
    }

    @Test
    public void testBaseConstructor() throws Exception {
        ContentVariableTransformer localTransformer = new ContentVariableTransformer();
    }

    @Test
    public void testWithInvalidConfiguration() throws Exception {
        context.registerInjectActivateService(new PropertyConfigServiceImpl(),
                ContextualContentVariableTestUtil.defaultConfigMap());
        context.registerInjectActivateService(new UrlEncodeAction());
        context.registerInjectActivateService(new PropertyAggregatorServiceImpl());
        reinitTransformer(false);

        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, "href", null, "CDATA", "/content/page.html?title=((inherited_page_properties.inheritedProperty))");
        transformer.startElement(null, "a", null, attributes);

        ArgumentCaptor<AttributesImpl> attributesCaptor = ArgumentCaptor.forClass(AttributesImpl.class);
        verify(handler, atLeast(1)).startElement(isNull(String.class), eq("a"), isNull(String.class), attributesCaptor.capture());

        Attributes out = attributesCaptor.getValue();
        assertEquals("/content/page.html?title=((inherited_page_properties.inheritedProperty))", out.getValue(0));
    }

    @Test
    public void testSingleReplacementWithEscape() throws Exception {
        reinitTransformer(true);

        String input = "((page_properties.propToEscape))";
        String output = "&lt;html&gt;";
        transformer.characters(input.toCharArray(), 0, input.length());

        ArgumentCaptor<char[]> charCaptor = ArgumentCaptor.forClass(char[].class);
        ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(handler, atLeast(1)).characters(charCaptor.capture(), eq(0), lengthCaptor.capture());

        String outputTitle = new String(charCaptor.getValue());
        assertEquals(output, outputTitle);
        assertEquals(Integer.valueOf(output.length()), lengthCaptor.getValue());
    }

    @Test
    public void testSingleReplacementWithActionDisablingEscaping() throws Exception {
        Map<String, Object> configMap = ContextualContentVariableTestUtil.defaultConfigMap();
        context.registerInjectActivateService(new PropertyConfigServiceImpl(), configMap);
        context.registerInjectActivateService(new AllPagePropertiesContentVariableProvider());
        context.registerInjectActivateService(new UrlEncodeAction());
        context.registerInjectActivateService(new PropertyAggregatorServiceImpl());
        reinitTransformer(false);

        String input = "((page_properties.urlPropToEscape!url))";
        String output = "%3Cp%3EparamValue%3C%2Fp%3E";
        transformer.characters(input.toCharArray(), 0, input.length());

        ArgumentCaptor<char[]> charCaptor = ArgumentCaptor.forClass(char[].class);
        ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(handler, atLeast(1)).characters(charCaptor.capture(), eq(0), lengthCaptor.capture());

        String outputTitle = new String(charCaptor.getValue());
        assertEquals(output, outputTitle);
        assertEquals(Integer.valueOf(output.length()), lengthCaptor.getValue());
    }

    @Test
    public void testMultipleReplacements() throws Exception {
        reinitTransformer(true);

        String input = "((page_properties.jcr:title)) and ((inherited_page_properties.inheritedProperty))";
        String output = "Arctic Surfing In Lofoten and inheritedValue";
        transformer.characters(input.toCharArray(), 0, input.length());

        ArgumentCaptor<char[]> charCaptor = ArgumentCaptor.forClass(char[].class);
        ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(handler, atLeast(1)).characters(charCaptor.capture(), eq(0), lengthCaptor.capture());

        String outputTitle = new String(charCaptor.getValue());
        assertEquals(output, outputTitle);
        assertEquals(Integer.valueOf(output.length()), lengthCaptor.getValue());
    }

    @Test
    public void testInvalidAndValidReplacement() throws Exception {
        reinitTransformer(true);

        String input = "((page_properties.jcr:title)) and ((page_properties.nonexisting))";
        String output = "Arctic Surfing In Lofoten and ((page_properties.nonexisting))";
        transformer.characters(input.toCharArray(), 0, input.length());

        ArgumentCaptor<char[]> charCaptor = ArgumentCaptor.forClass(char[].class);
        ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(handler, atLeast(1)).characters(charCaptor.capture(), eq(0), lengthCaptor.capture());

        String outputTitle = new String(charCaptor.getValue());
        assertEquals(output, outputTitle);
        assertEquals(Integer.valueOf(output.length()), lengthCaptor.getValue());
    }

    @Test
    public void testInvalidReplacement() throws Exception {
        reinitTransformer(true);

        String input = "((page_properties.nonexisting))";
        String output = "((page_properties.nonexisting))";
        transformer.characters(input.toCharArray(), 0, input.length());

        ArgumentCaptor<char[]> charCaptor = ArgumentCaptor.forClass(char[].class);
        ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(handler, atLeast(1)).characters(charCaptor.capture(), eq(0), lengthCaptor.capture());

        String outputTitle = new String(charCaptor.getValue());
        assertEquals(output, outputTitle);
        assertEquals(Integer.valueOf(output.length()), lengthCaptor.getValue());
    }

    @Test
    public void testHrefReplacementNotEncoded() throws Exception {
        reinitTransformer(true);

        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, "href", null, "CDATA", "/content/page.html?title=((inherited_page_properties.inheritedProperty))");
        transformer.startElement(null, "a", null, attributes);

        ArgumentCaptor<AttributesImpl> attributesCaptor = ArgumentCaptor.forClass(AttributesImpl.class);
        verify(handler, atLeast(1)).startElement(isNull(String.class), eq("a"), isNull(String.class), attributesCaptor.capture());

        Attributes out = attributesCaptor.getValue();
        assertEquals("/content/page.html?title=inheritedValue", out.getValue(0));
    }

    private void reinitTransformer(boolean defaultService) throws Exception {
        if (defaultService) {
            defaultService(context);
        }
        transformerFactory = context.registerInjectActivateService(new ContentVariableTransformerFactory());
        transformer = transformerFactory.createTransformer();
        transformer.setContentHandler(handler);
        transformer.init(processingContext, null);
    }
}
