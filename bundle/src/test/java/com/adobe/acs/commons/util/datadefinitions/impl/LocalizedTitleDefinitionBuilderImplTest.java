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