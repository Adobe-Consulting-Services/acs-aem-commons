package com.adobe.acs.commons.dam.renditionpickers;

import com.adobe.acs.commons.dam.renditionspickers.RenditionPatternPicker;
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
