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
package com.adobe.acs.commons.mcp.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Basic sanity check of value formats in MCP
 */
public class ValueFormatTest {

    private static String ONE = String.format("%.1f", 1f);

    enum FieldEnum {
        @FieldFormat(ValueFormat.plain)
        somePlainField,
        @FieldFormat(ValueFormat.storageSize)
        someStorageSizeField,
        someUnannotatedField
    }
    
    @Test
    public void testDefinedAnnotations() {
        assertEquals(ValueFormat.plain, ValueFormat.forField(FieldEnum.somePlainField));
        assertEquals(ValueFormat.storageSize, ValueFormat.forField(FieldEnum.someStorageSizeField));
    }
    
    @Test
    public void testUndefinedAnnotations() {
        assertEquals(ValueFormat.plain, ValueFormat.forField(FieldEnum.someUnannotatedField));
    }
    
    @Test
    public void oneKb() {
        assertEquals(ONE + " KiB", ValueFormat.getHumanSize(1 << 10));
    }
    
    @Test
    public void oneMb() {
        assertEquals(ONE + " MiB", ValueFormat.getHumanSize(1 << 20));
    }    
    
    @Test
    public void oneGb() {
        assertEquals(ONE + " GiB", ValueFormat.getHumanSize(1 << 30));
    }
    
    @Test
    public void onetb() {
        assertEquals(ONE + " TiB", ValueFormat.getHumanSize(((long) (1 << 30)) * 1024L));
    }
}
