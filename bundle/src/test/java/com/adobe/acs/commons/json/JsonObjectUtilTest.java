/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class JsonObjectUtilTest {
    
    public JsonObjectUtilTest() {
    }
    
    @Before
    public void setUp() {
    }

    /**
     * Test of toJsonObject method, of class JsonObjectUtil.
     */
    @Test
    public void testStringToJsonObject() {
        String json = "{level:1.0,l2:{level:2.0,l3:{level:3.0}}}";
        JsonObject obj = JsonObjectUtil.toJsonObject(json);
        assertNotNull(obj);
        assertEquals((Long) 1L, JsonObjectUtil.getLong(obj, "level"));
        assertTrue(obj.has("l2"));
        assertEquals((Long) 2L, JsonObjectUtil.getLong(obj.getAsJsonObject("l2"), "level"));
        assertTrue(obj.getAsJsonObject("l2").has("l3"));
        assertEquals((Long) 3L, JsonObjectUtil.getLong(obj.getAsJsonObject("l2").getAsJsonObject("l3"), "level"));        
    }

    /**
     * Test of toJsonObject method, of class JsonObjectUtil.
     */
    @Test
    public void testDepthLimit() {
        String json = "{level:1.0,l2:{level:2.0,l3:{level:3.0}}}";
        JsonObject obj = JsonObjectUtil.toJsonObject(json, 2);
        assertNotNull(obj);
        assertEquals((Long) 1L, JsonObjectUtil.getLong(obj, "level"));
        assertTrue(obj.has("l2"));
        assertEquals((Long) 2L, JsonObjectUtil.getLong(obj.getAsJsonObject("l2"), "level"));
        assertTrue(obj.getAsJsonObject("l2").has("l3"));
        assertFalse(obj.getAsJsonObject("l2").getAsJsonObject("l3").has("level"));
        obj = JsonObjectUtil.toJsonObject(json, 1);
        assertFalse(obj.getAsJsonObject("l2").has("l3"));
        assertFalse(obj.getAsJsonObject("l2").has("level"));
    }

    /**
     * Test of getAsJsonString method, of class JsonObjectUtil.
     */
    @Test
    public void testGetAsJsonString() {
        String json = "{\"level\":1.0,\"l2\":{\"level\":2.0,\"l3\":{\"level\":3.0}}}";
        String conv = JsonObjectUtil.getAsJsonString(json, 3);
        assertEquals(json, conv);
    }    
}
