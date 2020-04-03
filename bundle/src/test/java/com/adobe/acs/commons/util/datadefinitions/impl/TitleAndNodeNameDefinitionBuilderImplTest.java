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