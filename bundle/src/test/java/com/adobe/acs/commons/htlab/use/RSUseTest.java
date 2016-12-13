/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.htlab.use;

import javax.script.SimpleBindings;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class RSUseTest {

    @Test
    public void testParseFunctionKey() {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put(RSUse.B_PIPE, "$");
        bindings.put(RSUse.B_WRAP, ValueMapDecorator.EMPTY);
        RSUse rs = new RSUse();

        rs.init(bindings);
        RSUse.FunctionKey key1 = rs.parseFunctionKey("jcr:title");
        assertEquals("key1.normalizedKey='jcr:title'", "jcr:title", key1.getNormalizedKey());
        assertEquals("key1.property='jcr:title'", "jcr:title", key1.getProperty());
        assertEquals("key1.functions.length=0", 0, key1.getFunctions().length);

        RSUse.FunctionKey key2 = rs.parseFunctionKey("jcr:title $");
        assertEquals("key2.normalizedKey='jcr:title $ '", "jcr:title $ ", key2.getNormalizedKey());
        assertEquals("key2.property='jcr:title'", "jcr:title", key2.getProperty());
        assertEquals("key2.functions.length=0", 0, key2.getFunctions().length);

        RSUse.FunctionKey key3 = rs.parseFunctionKey("$ pageTitle");
        assertEquals("key3.normalizedKey=' $ pageTitle'", " $ pageTitle", key3.getNormalizedKey());
        assertEquals("key3.property=''", "", key3.getProperty());
        assertEquals("key3.functions.length=1", 1, key3.getFunctions().length);

        RSUse.FunctionKey key4 = rs.parseFunctionKey("$");
        assertEquals("key4.normalizedKey=' $ '", " $ ", key4.getNormalizedKey());
        assertEquals("key4.property=''", "", key4.getProperty());
        assertEquals("key4.functions.length=0", 0, key4.getFunctions().length);

        RSUse.FunctionKey key5 = rs.parseFunctionKey("$ pageTitle $ ");
        assertEquals("key5.normalizedKey=' $ pageTitle'", " $ pageTitle", key5.getNormalizedKey());
        assertEquals("key5.property=''", "", key5.getProperty());
        assertEquals("key5.functions.length=1", 1, key5.getFunctions().length);

        RSUse.FunctionKey key6 = rs.parseFunctionKey("$ pageTitle $ trunc ");
        assertEquals("key6.normalizedKey=' $ pageTitle'", " $ pageTitle $ trunc", key6.getNormalizedKey());
        assertEquals("key6.property=''", "", key6.getProperty());
        assertEquals("key6.functions.length=2", 2, key6.getFunctions().length);
        assertArrayEquals("key6.functions=[pageTitle,trunc]", new String[]{"pageTitle", "trunc"}, key6.getFunctions());

        RSUse.FunctionKey key7 = rs.parseFunctionKey("$ $");
        assertEquals("key7.normalizedKey=' $ '", " $ ", key7.getNormalizedKey());
        assertEquals("key7.property=''", "", key7.getProperty());
        assertEquals("key7.functions.length=0", 0, key7.getFunctions().length);

        RSUse.FunctionKey key8 = rs.parseFunctionKey("jcr:created $ jsonDate");
        assertEquals("key8.normalizedKey='jcr:created $ jsonDate'", "jcr:created $ jsonDate", key8.getNormalizedKey());
        assertEquals("key8.property='jcr:created'", "jcr:created", key8.getProperty());
        assertEquals("key8.functions.length=1", 1, key8.getFunctions().length);
        assertArrayEquals("key8.functions=[jsonDate]", new String[]{"jsonDate"}, key8.getFunctions());

    }
}
