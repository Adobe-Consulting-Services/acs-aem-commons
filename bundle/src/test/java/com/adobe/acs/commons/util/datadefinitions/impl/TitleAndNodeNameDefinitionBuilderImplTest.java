package com.adobe.acs.commons.util.datadefinitions.impl;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class TitleAndNodeNameDefinitionBuilderImplTest {
    final ResourceDefinitionBuilder builder = new TitleAndNodeNameDefinitionBuilderImpl();

    @Test
    public void testConvert() throws Exception {
        String expectedTitle = "Hello World";
        String expectedName = "hello-world";

        ResourceDefinition actual = builder.convert("Hello World {{ hello-world }}");

        assertEquals(expectedTitle, actual.getTitle());
        assertEquals(expectedName, actual.getName());
    }

    @Test
    public void accepts() throws Exception {
        assertTrue(builder.accepts("This could be anything!! {{ anything }}"));
        assertFalse(builder.accepts("This could be anything!!"));
    }
}