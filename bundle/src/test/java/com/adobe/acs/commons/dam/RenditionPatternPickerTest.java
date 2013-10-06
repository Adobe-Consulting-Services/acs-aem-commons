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
package com.adobe.acs.commons.dam;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;

import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RenditionPatternPickerTest {

    Rendition originalRendition;
    Rendition webRendition;
    Rendition largeRendition;
    Rendition smallRendition;
    Rendition customRendition;
    List<Rendition> renditions;
    Asset asset;

    public RenditionPatternPickerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        originalRendition = mock(Rendition.class);
        when(originalRendition.getName()).thenReturn("original");

        largeRendition = mock(Rendition.class);
        when(largeRendition.getName()).thenReturn("cq5dam.thumbnail.1000.1000");

        smallRendition = mock(Rendition.class);
        when(smallRendition.getName()).thenReturn("cq5dam.thumbnail.100.100");

        webRendition = mock(Rendition.class);
        when(webRendition.getName()).thenReturn("cq5dam.web.1280.1280");

        customRendition = mock(Rendition.class);
        when(customRendition.getName()).thenReturn("custom");

        renditions = new ArrayList<Rendition>();
        renditions.add(originalRendition);
        renditions.add(webRendition);
        renditions.add(largeRendition);
        renditions.add(smallRendition);
        renditions.add(customRendition);

        asset = mock(Asset.class);
        when(asset.getRenditions()).thenReturn(renditions);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getRendition method, of class RenditionPatternPicker.
     */
    @Test
    public void testGetRendition_MatchingRegex() {
        RenditionPatternPicker instance = new RenditionPatternPicker("^cust.*");
        Rendition expResult = customRendition;
        Rendition result = instance.getRendition(asset);
        assertEquals(expResult, result);
    }

    public void testGetRendition_MultiMatchingRegex() {
        RenditionPatternPicker instance = new RenditionPatternPicker("^cq5dam.*");
        Rendition expResult = largeRendition;
        Rendition result = instance.getRendition(asset);
        assertEquals(expResult, result);
    }

    public void testGetRendition_NonMatchingRegex() {
        RenditionPatternPicker instance = new RenditionPatternPicker("nothinghere");
        Rendition expResult = originalRendition;
        Rendition result = instance.getRendition(asset);
        assertEquals(expResult, result);
    }
}
