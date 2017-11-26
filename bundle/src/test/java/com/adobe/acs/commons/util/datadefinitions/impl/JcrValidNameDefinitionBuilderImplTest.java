package com.adobe.acs.commons.util.datadefinitions.impl;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JcrValidNameDefinitionBuilderImplTest {
    final ResourceDefinitionBuilder builder = new JcrValidNameDefinitionBuilderImpl();

    @Test
    public void testConvert() throws Exception {
        final String expectedTitle = "?! This  is @ crazy title #  WITH funky (& weird/bizarre) chArs!? in it!!!!  ";
        final String expectedName = "_this_is_crazy_titlewithfunkyweirdbizarrecharsinit";

        ResourceDefinition actual = builder.convert(expectedTitle);

        assertEquals(expectedTitle, actual.getTitle());
        assertEquals(expectedName, actual.getName());
    }

    @Test
    public void accepts() throws Exception {
        assertTrue(builder.accepts("This could be anything!!"));
    }
}