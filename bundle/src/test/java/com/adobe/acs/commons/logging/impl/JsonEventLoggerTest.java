/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.logging.impl;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;
import org.osgi.service.event.Event;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonEventLoggerTest {

    @Test
    public void testConstructMessage() throws JSONException {
        Map<String, Object> emptyProps = new LinkedHashMap<String, Object>();
        Event empty = new Event("my/empty/topic", emptyProps);

        JSONObject jEmptyProps = new JSONObject(JsonEventLogger.constructMessage(empty));
        assertEquals("basic event, empty props", "my/empty/topic", jEmptyProps.getString("event.topics"));

        Map<String, Object> stringProps = new LinkedHashMap<String, Object>();
        stringProps.put("slingevent:application", "376e48ac-b010-4905-8a35-f5413cf6a930");

        Event stringEvent = new Event("my/simple/topic", stringProps);
        JSONObject jStringProps = new JSONObject(JsonEventLogger.constructMessage(stringEvent));

        assertEquals("simple event, string props", "376e48ac-b010-4905-8a35-f5413cf6a930", jStringProps.getString("slingevent:application"));

        Map<String, Object> intProps = new LinkedHashMap<String, Object>();
        intProps.put("event.job.retries", -1);
        Event intEvent = new Event("my/simple/topic", intProps);
        JSONObject jIntProps = new JSONObject(JsonEventLogger.constructMessage(intEvent));

        assertEquals("simple event, int props", -1, jIntProps.getInt("event.job.retries"));

        Map<String, Object> boolProps = new LinkedHashMap<String, Object>();
        boolProps.put("event.isSimple", true);
        Event boolEvent = new Event("my/simple/topic", boolProps);
        JSONObject jBoolProps = new JSONObject(JsonEventLogger.constructMessage(boolEvent));

        assertTrue("simple event, bool props", jBoolProps.getBoolean("event.isSimple"));

        Map<String, Object> stringArrayProps = new LinkedHashMap<String, Object>();
        stringArrayProps.put("resourceChangedAttributes", new String[]{"first", "second"});
        Event stringArrayEvent = new Event("my/simple/topic", stringArrayProps);
        JSONObject jStringArray = new JSONObject(JsonEventLogger.constructMessage(stringArrayEvent));

        assertNotNull("complex event, string array not null", jStringArray.optJSONArray("resourceChangedAttributes"));
        assertEquals("complex event, string array props", "first", jStringArray.getJSONArray("resourceChangedAttributes").getString(0));
        assertEquals("complex event, string array props", "second", jStringArray.getJSONArray("resourceChangedAttributes").getString(1));

        Map<String, Object> intArrayProps = new LinkedHashMap<String, Object>();
        intArrayProps.put("numbers", new Integer[]{0, 1, 2});
        Event intArrayEvent = new Event("my/simple/topic", intArrayProps);
        JSONObject jIntArray = new JSONObject(JsonEventLogger.constructMessage(intArrayEvent));

        assertNotNull("complex event, int array not null", jIntArray.optJSONArray("numbers"));
        assertEquals("complex event, int array props", 0, jIntArray.getJSONArray("numbers").getInt(0));
        assertEquals("complex event, int array props", 1, jIntArray.getJSONArray("numbers").getInt(1));
        assertEquals("complex event, int array props", 2, jIntArray.getJSONArray("numbers").getInt(2));

        Map<String, Object> mapProps = new LinkedHashMap<String, Object>();
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("user-agent", "curl/7.25.0");
        mapProps.put("headers", headers);
        Event mapEvent = new Event("my/simple/topic", mapProps);
        JSONObject jMapProps = new JSONObject(JsonEventLogger.constructMessage(mapEvent));

        assertNotNull("complex event, map not null", jMapProps.optJSONObject("headers"));
        assertEquals("complex event, map value in props", "curl/7.25.0", jMapProps.getJSONObject("headers").getString("user-agent"));

        Map<String, Object> stringSetProps = new LinkedHashMap<String, Object>();
        stringSetProps.put("resourceChangedAttributes", new LinkedHashSet<>(Arrays.asList("first", "second")));
        Event stringSetEvent = new Event("my/simple/topic", stringSetProps);
        JSONObject jStringSet = new JSONObject(JsonEventLogger.constructMessage(stringSetEvent));

        assertNotNull("complex event, string set not null", jStringSet.optJSONArray("resourceChangedAttributes"));
        assertEquals("complex event, string set props", "first", jStringSet.getJSONArray("resourceChangedAttributes").getString(0));
        assertEquals("complex event, string set props", "second", jStringSet.getJSONArray("resourceChangedAttributes").getString(1));

    }
}
