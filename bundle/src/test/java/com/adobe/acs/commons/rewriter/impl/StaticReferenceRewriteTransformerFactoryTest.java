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
package com.adobe.acs.commons.rewriter.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.apache.sling.commons.testing.osgi.MockBundle;
import org.apache.sling.commons.testing.osgi.MockComponentContext;
import org.apache.sling.rewriter.Transformer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

@RunWith(MockitoJUnitRunner.class)
public class StaticReferenceRewriteTransformerFactoryTest {

    @Mock
    private ContentHandler handler;

    @Captor
    private ArgumentCaptor<Attributes> attributesCaptor;

    @Test
    public void test_without_config_is_noop() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "href", null, "CDATA", "/etc/clientlib/test.css");
        transformer.startElement(null, "link", null, in);

        verify(handler, only()).startElement(isNull(), eq("link"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("/etc/clientlib/test.css", out.getValue(0));
    }

    @Test
    public void test_with_prefix_and_single_host() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/etc/clientlib" });
        ctx.setProperty("host.pattern", "static.host.com");

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "href", null, "CDATA", "/etc/clientlib/test.css");
        transformer.startElement(null, "link", null, in);

        verify(handler, only()).startElement(isNull(), eq("link"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("//static.host.com/etc/clientlib/test.css", out.getValue(0));
    }

    @Test
    public void test_with_prefix_and_matching_pattern_and_single_host() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/content/dam" });
        ctx.setProperty("attributes", new String[] { "img:srcset,src" });
        ctx.setProperty("host.pattern", "static.host.com");
        ctx.setProperty("matchingPatterns", "img:srcset;(\\/content\\/dam\\/.+?\\.(png|jpg))");

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl imageWithSrcSet = new AttributesImpl();
        imageWithSrcSet.addAttribute(null, "srcset", null, "CDATA", "/content/dam/flower.jpg 1280w,/content/dam/house.png 480w");
        transformer.startElement(null, "img", null, imageWithSrcSet);

        AttributesImpl imageWithJustSrc = new AttributesImpl();
        imageWithJustSrc.addAttribute(null, "src", null, "CDATA", "/content/dam/flower.jpg");
        transformer.startElement(null, "img", null, imageWithJustSrc);

        verify(handler, times(2)).startElement(isNull(), eq("img"), isNull(),
                attributesCaptor.capture());
        List<Attributes> values = attributesCaptor.getAllValues();
        assertEquals("//static.host.com/content/dam/flower.jpg 1280w,//static.host.com/content/dam/house.png 480w", values.get(0).getValue(0));
        assertEquals("//static.host.com/content/dam/flower.jpg", values.get(1).getValue(0));
    }

    @Test
    public void test_with_prefix_and_matching_pattern_and_single_host_and_replace_host() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/content/dam" });
        ctx.setProperty("attributes", new String[] { "img:src" });
        ctx.setProperty("host.pattern", "static.host.com");
        ctx.setProperty("matchingPatterns", "img:src;(\\/content\\/dam\\/.+?\\.(png|jpg))");
        ctx.setProperty("replaceHost", true);

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl imageWithJustSrc = new AttributesImpl();
        imageWithJustSrc.addAttribute(null, "src", null, "CDATA", "https://www.host.com/content/dam/flower.jpg");
        transformer.startElement(null, "img", null, imageWithJustSrc);

        verify(handler, only()).startElement(isNull(), eq("img"), isNull(),
                attributesCaptor.capture());
        List<Attributes> values = attributesCaptor.getAllValues();
        assertEquals("https://static.host.com/content/dam/flower.jpg", values.get(0).getValue(0));
    }

    @Test
    public void test_with_prefix_and_matching_pattern_and_single_host_and_replace_host_without_protocol() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/content/dam" });
        ctx.setProperty("attributes", new String[] { "img:src" });
        ctx.setProperty("host.pattern", "static.host.com");
        ctx.setProperty("matchingPatterns", "img:src;(\\/content\\/dam\\/.+?\\.(png|jpg))");
        ctx.setProperty("replaceHost", true);

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl imageWithJustSrc = new AttributesImpl();
        imageWithJustSrc.addAttribute(null, "src", null, "CDATA", "//www.host.com/content/dam/flower_2.jpg");
        transformer.startElement(null, "img", null, imageWithJustSrc);

        verify(handler, only()).startElement(isNull(), eq("img"), isNull(),
                attributesCaptor.capture());
        List<Attributes> values = attributesCaptor.getAllValues();
        assertEquals("//static.host.com/content/dam/flower_2.jpg", values.get(0).getValue(0));
    }

    @Test
    public void test_with_nostatic_class() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/etc/clientlib" });
        ctx.setProperty("host.pattern", "static.host.com");

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "href", null, "CDATA", "/etc/clientlib/test.css");
        in.addAttribute(null, "class", null, "CDATA", "something nostatic");
        transformer.startElement(null, "link", null, in);

        verify(handler, only()).startElement(isNull(), eq("link"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("/etc/clientlib/test.css", out.getValue(0));
    }


    @Test
    public void test_with_non_matching_prefix_and_single_host() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/etc/clientlib" });
        ctx.setProperty("host.pattern", "static.host.com");

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "href", null, "CDATA", "/content/clientlib/test.css");
        transformer.startElement(null, "link", null, in);

        verify(handler, only()).startElement(isNull(), eq("link"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("/content/clientlib/test.css", out.getValue(0));
    }

    @Test
    public void test_with_prefix_and_multiple_numbered_hosts() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/etc/clientlib" });
        ctx.setProperty("host.pattern", "static{}.host.com");
        ctx.setProperty("host.count", 2);

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "href", null, "CDATA", "/etc/clientlib/testA.css");
        transformer.startElement(null, "link", null, in);

        verify(handler, only()).startElement(isNull(), eq("link"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("//static2.host.com/etc/clientlib/testA.css", out.getValue(0));
    }

    @Test
    public void test_with_prefix_and_multiple_named_hosts() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/etc/clientlib" });
        ctx.setProperty("host.pattern", new String[] { "staticA.host.com", "staticB.host.com" });
        ctx.setProperty("host.count", 2);

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "href", null, "CDATA", "/etc/clientlib/testA.css");
        transformer.startElement(null, "link", null, in);

        verify(handler, only()).startElement(isNull(), eq("link"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("//staticB.host.com/etc/clientlib/testA.css", out.getValue(0));
    }

    @Test
    public void test_with_nonrewritten_attribute() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/etc/clientlib" });
        ctx.setProperty("host.pattern", "static{}.host.com");
        ctx.setProperty("host.count", 2);

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "src", null, "CDATA", "/etc/clientlib/testABC.css");
        transformer.startElement(null, "link", null, in);

        verify(handler, only()).startElement(isNull(), eq("link"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("/etc/clientlib/testABC.css", out.getValue(0));
    }

    @Test
    public void test_with_nonrewritten_element() throws Exception {
        MockBundle bundle = new MockBundle(-1);
        MockComponentContext ctx = new MockComponentContext(bundle);
        ctx.setProperty("prefixes", new String[] { "/etc/clientlib" });
        ctx.setProperty("host.pattern", "static{}.host.com");
        ctx.setProperty("host.count", 2);

        StaticReferenceRewriteTransformerFactory factory = new StaticReferenceRewriteTransformerFactory();
        factory.activate(ctx);

        Transformer transformer = factory.createTransformer();
        transformer.setContentHandler(handler);

        AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "src", null, "CDATA", "/etc/clientlib/testABC.css");
        transformer.startElement(null, "iframe", null, in);

        verify(handler, only()).startElement(isNull(), eq("iframe"), isNull(),
                attributesCaptor.capture());
        Attributes out = attributesCaptor.getValue();
        assertEquals("/etc/clientlib/testABC.css", out.getValue(0));
    }

}
