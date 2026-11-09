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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    /**
     * The attribute value is a URL whose path must end up as a single, correctly encoded path.
     * ResourceResolver#map expects a decoded path (it encodes every "%" and "+" itself, and treats the first
     * "#", or else "?", as the start of the fragment/query string), so the path is decoded the same way the
     * OOTB LinkCheckerTransformer does (URI#getPath, or the path as-is if it isn't a valid URI), while the query
     * string and fragment are passed through exactly as they are.
     */
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            // --- plain paths
            "/content/site/en/page.html                         | /site/en/page.html",
            "/content/site/en/page.sel1.sel2.html/suffix/a.json | /site/en/page.sel1.sel2.html/suffix/a.json",
            "/content/site/en/jcr:content/img.png               | /site/en/_jcr_content/img.png",
            "/content/site/en/jcr%3Acontent/img.png             | /site/en/_jcr_content/img.png",
            "/content/site/en/jcr%3acontent/img.png             | /site/en/_jcr_content/img.png",
            // --- spaces
            "/content/site/en/a b.png                           | /site/en/a%20b.png",
            "/content/site/en/a%20b.png                         | /site/en/a%20b.png",
            // a raw space makes the path an invalid URI, so (like LinkChecker) its escapes are taken literally
            "/content/site/en/a%20b c.png                       | /site/en/a%2520b%20c.png",
            // --- plus: a literal plus in a path, never a space
            "/content/something+something/test.html             | /something%2Bsomething/test.html",
            "/content/something%2Bsomething/test.html           | /something%2Bsomething/test.html",
            "/content/something%2bsomething/test.html           | /something%2Bsomething/test.html",
            "/content/a+%2B+b/test.html                         | /a%2B%2B%2Bb/test.html",
            "/content/a%20+%20b/test.html                       | /a%20%2B%20b/test.html",
            // --- non-ASCII characters (#3808)
            "/content/dam/Cat © Adobe.jpg                       | /dam/Cat%20%C2%A9%20Adobe.jpg",
            "/content/dam/Cat%20©%20Adobe.jpg                   | /dam/Cat%20%C2%A9%20Adobe.jpg",
            "/content/dam/Cat%20%C2%A9%20Adobe.jpg              | /dam/Cat%20%C2%A9%20Adobe.jpg",
            "/content/dam/Cat%20© Adobe.jpg                     | /dam/Cat%2520%C2%A9%20Adobe.jpg",
            "/content/dam/caf%c3%a9.png                         | /dam/caf%C3%A9.png",
            "/content/dam/café.png                              | /dam/caf%C3%A9.png",
            "/content/dam/日本.png                               | /dam/%E6%97%A5%E6%9C%AC.png",
            "/content/dam/%F0%9F%90%88.png                      | /dam/%F0%9F%90%88.png",
            "/content/dam/🐈.png                                 | /dam/%F0%9F%90%88.png",
            // --- percent signs
            "/content/dam/100%25.png                            | /dam/100%25.png",
            "/content/dam/100%2525.png                          | /dam/100%2525.png",
            "/content/dam/100%.png                              | /dam/100%25.png",
            "/content/dam/img%gg.png                            | /dam/img%25gg.png",
            // --- other characters that are or aren't allowed raw in a path
            "/content/dam/it's (1).png                          | /dam/it's%20(1).png",
            "/content/dam/a&b=c.png                             | /dam/a&b=c.png",
            "/content/dam/a%26b%3Dc.png                         | /dam/a&b=c.png",
            "/content/dam/q\"uote.png                           | /dam/q%22uote.png",
            "/content/dam/q%22uote.png                          | /dam/q%22uote.png",
            "/content/dam/<tag>.png                             | /dam/%3Ctag%3E.png",
            "/content/dam/%3Ctag%3E.png                         | /dam/%3Ctag%3E.png",
            // --- mixed raw characters and escapes, still a valid URI: the escapes are decoded
            "/content/dam/café%20bar.png                        | /dam/caf%C3%A9%20bar.png",
            "/content/dam/日本%2B.png                            | /dam/%E6%97%A5%E6%9C%AC%2B.png",
            "/content/dam/a+b%20c+d.png                         | /dam/a%2Bb%20c%2Bd.png",
            "/content/dam/©%C2%A9.png                           | /dam/%C2%A9%C2%A9.png",
            "/content/dam/a&b%3Dc.png                           | /dam/a&b=c.png",
            // --- mixed raw characters and escapes, not a valid URI (raw space, quote, "<", ">" or an invalid
            // escape): like LinkChecker, the whole path is taken literally, so its escapes are encoded again
            "/content/dam/a%2Bb c.png                           | /dam/a%252Bb%20c.png",
            "/content/dam/caf%C3%A9 bar.png                     | /dam/caf%25C3%25A9%20bar.png",
            "/content/dam/100% sure.png                         | /dam/100%25%20sure.png",
            "/content/dam/q\"uote%20x.png                       | /dam/q%22uote%2520x.png",
            "/content/dam/<b>%20x.png                           | /dam/%3Cb%3E%2520x.png",
            "/content/dam/a%20b%gg.png                          | /dam/a%2520b%25gg.png",
            // --- known limitation, shared with LinkChecker: a decoded "?" or "#" in the path is treated by
            // ResourceResolver#map as the start of the query string/fragment. Such names are invalid in AEM
            // (DAM upload rejects * / : [ \ ] | # % { } ? & and page names are generated without them), so
            // they can only exist when created bypassing AEM's validation.
            "/content/dam/what%3F.png                           | /dam/what?.png",
            "/content/dam/no%231.png                            | /dam/no#1.png",
            "/content/dam/what%3F%23.png?q=1#top                | /dam/what#.png?q=1#top",
            // --- query strings and fragments are passed through untouched
            "/content/site/en/page.html?q=a+b                   | /site/en/page.html?q=a+b",
            "/content/site/en/page.html?q=a%2Bb&r=%22x%22       | /site/en/page.html?q=a%2Bb&r=%22x%22",
            "/content/site/en/page.html?q=©                     | /site/en/page.html?q=©",
            "/content/site/en/page.html?                        | /site/en/page.html?",
            "/content/site/en/page.html#top                     | /site/en/page.html#top",
            "/content/site/en/page.html#a b?c                   | /site/en/page.html#a b?c",
            "/content/site/en/page.html?q=1#top                 | /site/en/page.html?q=1#top",
            // (unlike LinkChecker, which decodes the query string when the URL also has a fragment)
            "/content/site/en/page.html?q=a%2Bb#top             | /site/en/page.html?q=a%2Bb#top",
            "/content/site/en/page.html?q=%26x%3D1#top          | /site/en/page.html?q=%26x%3D1#top",
            "/content/site/en/page.html?q=a+b#top               | /site/en/page.html?q=a+b#top",
            "/content/site/en/page.html?q=%22x%22#top           | /site/en/page.html?q=%22x%22#top",
            "/content/site/en/page.html?q=©#top                 | /site/en/page.html?q=©#top",
            "/content/site/en/page.html?q=a b#top b             | /site/en/page.html?q=a b#top b",
            "/content/site/en/page.html?q=a%2Bb#frag%20x        | /site/en/page.html?q=a%2Bb#frag%20x",
            "/content/site/en/page.html?a=1&b=%2B&c=%25#x       | /site/en/page.html?a=1&b=%2B&c=%25#x",
            "/content/site/en/page.html#top?x=%2B               | /site/en/page.html#top?x=%2B",
            "/content/site/en/page.html?#                       | /site/en/page.html?#",
            "/content/site/en/page.html#                        | /site/en/page.html#",
            // --- path that needs decoding/encoding combined with a query string and fragment
            "/content/dam/Cat%20%C2%A9.jpg?w=1&h=%2B#x          | /dam/Cat%20%C2%A9.jpg?w=1&h=%2B#x",
            "/content/dam/Cat © Adobe.jpg?q=%2B#x               | /dam/Cat%20%C2%A9%20Adobe.jpg?q=%2B#x",
            "/content/dam/a+b.jpg?q=a+b#c%20d                   | /dam/a%2Bb.jpg?q=a+b#c%20d",
            "/content/dam/a%20b c.jpg?q=%20#%20                 | /dam/a%2520b%20c.jpg?q=%20#%20",
            "/content/site/en/jcr%3Acontent/img.png?w=1#x       | /site/en/_jcr_content/img.png?w=1#x",
            "/content/site/en/a+b.html?q=a+b#c+d                | /site/en/a%2Bb.html?q=a+b#c+d",
            "/content/site/en/a b.html?q=a b                    | /site/en/a%20b.html?q=a b",
    })
    void testRebuildAttributes_UrlMatrix(String in, String expected) throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("a:href");

        assertEquals(expected, rebuiltValue(factory, "a", "href", in));
    }

    @Test
    void testRebuildAttributes_PlusInQueryStringIsLeftUntouched() throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("a:href");

        assertEquals("/site/en/a%2Bb.html?q=c+d%2Be",
                rebuiltValue(factory, "a", "href", "/content/site/en/a+b.html?q=c+d%2Be"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/content/site/en/img%.png", "/content/site/en/img%4", "/content/site/en/img%"})
    void testRebuildAttributes_IncompletePercentEncodingFallsBackToRawPath(String in) throws Exception {
        final ResourceResolverMapTransformerFactory factory = factoryFor("img:src");

        assertEquals(in.replace("/content", "").replace("%", "%25"), rebuiltValue(factory, "img", "src", in));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = false, value = {
            "/no/escapes|/no/escapes",
            "/a%20b|/a b",
            "/a%2fb|/a/b",
            "/a%2Fb|/a/b",
            "/a+b|/a+b",
            "/a%2Bb|/a+b",
            "/%C2%A9|/©",
            "/©%20©|/© ©",
            "/%E6%97%A5%E6%9C%AC|/日本",
            "/%F0%9F%90%88|/\uD83D\uDC08",
            "/%25|/%",
            "/%2525|/%25",
            "/%C3|/\uFFFD",
            // not a valid URI, so returned as-is
            "/a b|/a b",
            "/a%20b c|/a%20b c",
            "/a%gg|/a%gg",
            "/a%|/a%",
    })
    void testDecodePath(String in, String expected) {
        assertEquals(expected, ResourceResolverMapTransformerFactory.decodePath(in));
    }

    @Test
    void testRebuildAttributes_InvalidPercentEncodingFallsBackToRawPath() throws Exception {
        // "%gg" is not a valid percent-encoded sequence, so the path isn't a valid URI; it must be
        // mapped as-is (undecoded) rather than propagating an exception.
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
