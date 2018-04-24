package com.adobe.acs.commons.util.datadefinitions.impl;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class LocalizedTitleDefinitionBuilderImplTest {
    final ResourceDefinitionBuilder builder = new LocalizedTitleDefinitionBuilderImpl();

    @Test
    public void convertWithDefault() throws Exception {

        String data = "default[Default Title] en[English Title] fr[French Title] es[Spanish Title] {{ my-title }}";

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("en", "English Title");
        expected.put("fr", "French Title");
        expected.put("es", "Spanish Title");

        ResourceDefinition def = builder.convert(data);

        Map<String, String> actual = def.getLocalizedTitles();

        assertEquals("Default Title", def.getTitle());
        assertEquals(expected, actual);
    }

    @Test
    public void convertWithoutDefault() throws Exception {

        String data = " en[English Title] fr[French Title] es[Spanish Title] {{my-title}} ";

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("en", "English Title");
        expected.put("fr", "French Title");
        expected.put("es", "Spanish Title");

        ResourceDefinition def = builder.convert(data);

        Map<String, String> actual = def.getLocalizedTitles();

        assertEquals("English Title", def.getTitle());
        assertEquals(expected, actual);
    }

    @Test
    public void accepts() throws Exception {
        assertTrue(builder.accepts("en[My Title] {{my-title}}"));
        assertTrue(builder.accepts("en[My Title] {{ my-title }}"));
        assertTrue(builder.accepts("en[Title] fr[Titre] es[Titulo] {{ node-name }}"));
        assertTrue(builder.accepts("en_US[Title] {{ node-name }}"));
        assertTrue(builder.accepts("default[Title]  en[Title] fr[Titre] es[Titulo] {{ node-name }}"));
        assertFalse(builder.accepts("en![Title] {{ node-name }}"));
        assertFalse(builder.accepts("en us[Title] {{ node-name }}"));
    }
}