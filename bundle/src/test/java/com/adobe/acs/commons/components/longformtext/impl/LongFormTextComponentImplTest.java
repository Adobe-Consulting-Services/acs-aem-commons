/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.components.longformtext.impl;

import junitx.util.PrivateAccessor;
import org.apache.sling.commons.html.HtmlParser;
import org.apache.sling.commons.html.impl.DOMBuilder;
import org.ccil.cowan.tagsoup.Parser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import java.io.IOException;
import java.io.InputStream;


@RunWith(MockitoJUnitRunner.class)
public class LongFormTextComponentImplTest {

    SlingHtmlParser htmlParser = new SlingHtmlParser();

    final LongFormTextComponentImpl longFormTextComponent = new LongFormTextComponentImpl();

    @Before
    public void setup() throws Exception {
        PrivateAccessor.setField(longFormTextComponent, "htmlParser", htmlParser);
    }

    @Test
    public void testGetTextParagraphs_1() throws Exception {
        final String input = "<p>ira is a dog</p>\n<p> she barks a lot</p>";

        final String[] expected = new String[] {"<p>ira is a dog</p>", "<p> she barks a lot</p>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetTextParagraphs_2() throws Exception {
        final String input = "<div class=\"dog-park\"><p>ira is a dog</p><p> she barks a lot</p></div>";

        final String[] expected = new String[] {"<div class=\"dog-park\"><p>ira is a dog</p><p> she barks a "
                + "lot</p></div>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetTextParagraphs_3() throws Exception {
        final String input = "<span>ira looks like this: <img src=\"dog.png\"/></span><p>she barks a lot</p>";

        final String[] expected = new String[] {"<span>ira looks like this: <img src=\"dog.png\"/></span>",
                "<p>she barks a lot</p>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetTextParagraphs_4() throws Exception {
        final String input = "<p>ira looks like this</p><img src=\"dog.png\"/>";

        final String[] expected = new String[] {"<p>ira looks like this</p>","<img src=\"dog.png\"/>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetTextParagraphs_5() throws Exception {
        final String input = "random junk text <p> ira looks like this</p>  <img src=\"dog.png\"/>   other junk";

        final String[] expected = new String[] {"<p> ira looks like this</p>","<img src=\"dog.png\"/>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }
}
