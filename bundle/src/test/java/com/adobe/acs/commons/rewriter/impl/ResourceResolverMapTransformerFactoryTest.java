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

import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Uses a real (JCR_OAK-backed) ResourceResolver rather than a hand-rolled mock, since
 * ResourceResolver#map's own encoding behaviour (eg. escaping "jcr:content" to "_jcr_content",
 * or double-encoding an already percent-encoded path) is exactly what this class needs to work
 * around, and what its query-string handling needs to avoid disturbing.
 */
@ExtendWith({MockitoExtension.class, SlingContextExtension.class})
class ResourceResolverMapTransformerFactoryTest {

    private final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Mock
    private ProcessingContext processingContext;

    private ResourceResolverMapTransformerFactory factoryFor(String... elementAttributePairs) {
        return context.registerInjectActivateService(ResourceResolverMapTransformerFactory.class, "attributes", elementAttributePairs);
    }

    private String rebuiltValue(ResourceResolverMapTransformerFactory factory, String element, String attrName,
                                 String attrValue) throws Exception {
        // A fresh handler/captor per call: several tests exercise more than one element through
        // the same factory, and a shared mock would accumulate interactions across those calls.
        final ContentHandler handler = mock(ContentHandler.class);
        final ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);
        when(processingContext.getRequest()).thenReturn(context.request());

        final Transformer transformer = factory.createTransformer();
        transformer.init(processingContext, null);
        transformer.setContentHandler(handler);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, attrName, null, "CDATA", attrValue);

        transformer.startElement(null, element, null, in);

        verify(handler, only()).startElement(isNull(), eq(element), isNull(), attributesCaptor.capture());
        return attributesCaptor.getValue().getValue(0);
    }

    @Test
    void testRebuildAttributes_MapsMatchingAttribute() throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:src");

        assertEquals("/site/en/_jcr_content/img.png",
                rebuiltValue(factory, "img", "src", "/content/site/en/jcr:content/img.png"));
    }

    @Test
    void testRebuildAttributes_UnconfiguredAttributeIsLeftUntouched() throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:data-src,src");

        assertEquals("/img.png", rebuiltValue(factory, "img", "data-uri", "/img.png"));
    }

    @Test
    void testRebuildAttributes_UnconfiguredElementIsLeftUntouched() throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:src");

        assertEquals("/content/site/en/page.html", rebuiltValue(factory, "div", "href", "/content/site/en/page.html"));
    }

    @Test
    void testRebuildAttributes_NullRequestIsLeftUntouched() {
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:src");

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute(null, "src", null, "CDATA", "/content/site/en/image.png");

        final Attributes out = factory.rebuildAttributes(null, "img", in);

        assertEquals("/content/site/en/image.png", out.getValue(0));
    }

    @Test
    void testRebuildAttributes_RelativePathIsNotMapped() throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:src");

        assertEquals("relative/img.png", rebuiltValue(factory, "img", "src", "relative/img.png"));
    }

    @Test
    void testRebuildAttributes_ProtocolRelativeUrlIsNotMapped() throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:src");

        assertEquals("//cdn.example.com/img.png",
                rebuiltValue(factory, "img", "src", "//cdn.example.com/img.png"));
    }

    @Test
    void testRebuildAttributes_DoubleEncodedPathIsDecodedBeforeMapping() throws Exception {
        // "jcr%3acontent" is what a thumbnail rendition URI looks like once Text#escapePath has
        // already encoded it once (see #1464). Without decoding it first, ResourceResolver#map
        // would encode the already-encoded "%" again, corrupting the path.
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:src");

        assertEquals("/site/en/_jcr_content/img%20test.png",
                rebuiltValue(factory, "img", "src", "/content/site/en/jcr%3acontent/img%20test.png"));
    }

    @Test
    void testRebuildAttributes_InvalidPercentEncodingFallsBackToRawPath() throws Exception {
        // "%gg" is not a valid percent-encoded sequence; decoding must fail gracefully and fall
        // back to mapping the raw (undecoded) path rather than propagating the exception.
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:src");

        assertEquals("/site/en/_jcr_content/img%25gg.png",
                rebuiltValue(factory, "img", "src", "/content/site/en/jcr:content/img%gg.png"));
    }

    @Test
    void testRebuildAttributes_QueryStringIsNeverDecoded() throws Exception {
        // %22 is an encoded double quote; decoding it here would let it break out of the HTML
        // attribute once the rewritten value is serialized back into the page (see #2165).
        final ResourceResolverMapTransformerFactory factory = factoryFor("a:href");

        assertEquals("/site/en/page.html?q=%22Search%22",
                rebuiltValue(factory, "a", "href", "/content/site/en/page.html?q=%22Search%22"));
    }

    @Test
    void testRebuildAttributes_FragmentAfterQueryStringIsNeverDecoded() throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("a:href");

        assertEquals("/site/en/page.html?q=%22Search%22#section",
                rebuiltValue(factory, "a", "href", "/content/site/en/page.html?q=%22Search%22#section"));
    }

    @Test
    void testActivate_Array() throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("a:b", "c:d");

        assertEquals("/one.png", rebuiltValue(factory, "a", "b", "/content/one.png"));
        assertEquals("/two.png", rebuiltValue(factory, "c", "d", "/content/two.png"));
        assertEquals("/content/three.png", rebuiltValue(factory, "e", "f", "/content/three.png"));
    }

    @Test
    void testActivate_String() throws Exception {
        final Map<String, Object> config = new HashMap<>();
        config.put("attributes", "a:b,c:d");
        final ResourceResolverMapTransformerFactory factory =
                context.registerInjectActivateService(ResourceResolverMapTransformerFactory.class, config);

        assertEquals("/one.png", rebuiltValue(factory, "a", "b", "/content/one.png"));
        assertEquals("/two.png", rebuiltValue(factory, "c", "d", "/content/two.png"));
        assertEquals("/content/three.png", rebuiltValue(factory, "e", "f", "/content/three.png"));
    }
}
