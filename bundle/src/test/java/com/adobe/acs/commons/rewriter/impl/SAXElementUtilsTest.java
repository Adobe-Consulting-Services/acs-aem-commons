/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

@RunWith(MockitoJUnitRunner.class)
public class SAXElementUtilsTest {

    @Test
    public void testIsCSS() throws Exception {
        assertTrue("CSS Happy Path", 
                SAXElementUtils.isCSS("link", 
                        makeAtts(
                                "href", "/css.css",
                                "type", "text/css")));
        
        assertFalse("CSS - not a link", 
                SAXElementUtils.isCSS("notlink", 
                        makeAtts(
                                "href", "/css.css",
                                "type", "text/css")));
        
        assertFalse("CSS - not a path to css file", 
                SAXElementUtils.isCSS("link", 
                        makeAtts(
                                "href", "/css.notcss",
                                "type", "text/css")));

        assertFalse("CSS - relative path", 
                SAXElementUtils.isCSS("link", 
                        makeAtts(
                                "href", "css.css",
                                "type", "text/css")));

        assertFalse("CSS - external path", 
                SAXElementUtils.isCSS("link", 
                        makeAtts(
                                "href", "http://www.adobe.com/css.css",
                                "type", "text/css")));

        
        assertFalse("CSS - wrongtype", 
                SAXElementUtils.isCSS("link", 
                        makeAtts(
                                "href", "/css.css",
                                "type", "text/notcss")));
    }
    
    @Test
    public void testIsJavascript() throws Exception {
        assertTrue("JS Happy Path", 
                SAXElementUtils.isJavaScript("script", 
                        makeAtts(
                                "src", "/js.js",
                                "type", SAXElementUtils.JS_TYPE)));
        
        assertFalse("JS - not a link", 
                SAXElementUtils.isJavaScript("notscript", 
                        makeAtts(
                                "src", "/js.js",
                                "type", SAXElementUtils.JS_TYPE)));
        
        assertFalse("JS - not a path to js file", 
                SAXElementUtils.isJavaScript("script", 
                        makeAtts(
                                "src", "/js.notjs",
                                "type", SAXElementUtils.JS_TYPE)));

        assertFalse("JS - relative path", 
                SAXElementUtils.isJavaScript("script", 
                        makeAtts(
                                "src", "js.js",
                                "type", SAXElementUtils.JS_TYPE)));

        assertFalse("JS - external path", 
                SAXElementUtils.isJavaScript("script", 
                        makeAtts(
                                "src", "http://www.adobe.com/js.js",
                                "type", SAXElementUtils.JS_TYPE)));

        
        assertFalse("JS - wrongtype", 
                SAXElementUtils.isJavaScript("script", 
                        makeAtts(
                                "src", "/js.js",
                                "type", "not" + SAXElementUtils.JS_TYPE)));
    }
    
    private Attributes makeAtts( String... strings ) {
        AttributesImpl atts = new AttributesImpl();
        for( int i = 0; i < strings.length/2; i++) {
            String key = strings[i*2];
            String value = strings[i*2 + 1];
            atts.addAttribute("", key, "", "CDATA", value); 
        }
        return atts;
    }
}
