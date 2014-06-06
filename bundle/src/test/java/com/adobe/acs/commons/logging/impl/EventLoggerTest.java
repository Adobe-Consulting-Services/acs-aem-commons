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
import org.junit.Test;
import org.osgi.service.event.Event;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by mark.j.adamcin on 6/5/14.
 */
public class EventLoggerTest {

    @Test
    public void testConstructMessage() throws JSONException {
        Map<String, Object> emptyProps = new LinkedHashMap<String, Object>();
        Event empty = new Event("my/empty/topic", mapToDictionary(emptyProps));

        assertEquals("basic event, empty props", "{\"event.topics\":\"my/empty/topic\"}", EventLogger.constructMessage(empty));

        Map<String, Object> stringProps = new LinkedHashMap<String, Object>();
        stringProps.put("slingevent:application", "376e48ac-b010-4905-8a35-f5413cf6a930");

        Event stringEvent = new Event("my/simple/topic", mapToDictionary(stringProps));

        assertEquals("simple event, string props", "{\"event.topics\":\"my/simple/topic\",\"slingevent:application\":\"376e48ac-b010-4905-8a35-f5413cf6a930\"}",
                EventLogger.constructMessage(stringEvent));

        Map<String, Object> intProps = new LinkedHashMap<String, Object>();
        intProps.put("event.job.retries", -1);


        Event intEvent = new Event("my/simple/topic", mapToDictionary(intProps));

        assertEquals("simple event, int props", "{\"event.topics\":\"my/simple/topic\",\"event.job.retries\":-1}",
                EventLogger.constructMessage(intEvent));

        Map<String, Object> boolProps = new LinkedHashMap<String, Object>();
        boolProps.put("event.isSimple", true);


        Event boolEvent = new Event("my/simple/topic", mapToDictionary(boolProps));

        assertEquals("simple event, bool props", "{\"event.topics\":\"my/simple/topic\",\"event.isSimple\":true}",
                EventLogger.constructMessage(boolEvent));

        Map<String, Object> stringArrayProps = new LinkedHashMap<String, Object>();
        stringArrayProps.put("resourceChangedAttributes", new String[]{"first", "second"});


        Event stringArrayEvent = new Event("my/simple/topic", mapToDictionary(stringArrayProps));

        assertEquals("complex event, string array props", "{\"event.topics\":\"my/simple/topic\",\"resourceChangedAttributes\":[\"first\",\"second\"]}",
                EventLogger.constructMessage(stringArrayEvent));

        Map<String, Object> intArrayProps = new LinkedHashMap<String, Object>();
        intArrayProps.put("numbers", new Integer[]{0, 1, 2});


        Event intArrayEvent = new Event("my/simple/topic", mapToDictionary(intArrayProps));

        assertEquals("complex event, int array props", "{\"event.topics\":\"my/simple/topic\",\"numbers\":[0,1,2]}",
                EventLogger.constructMessage(intArrayEvent));

        Map<String, Object> mapProps = new LinkedHashMap<String, Object>();
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("user-agent", "curl/7.25.0");
        mapProps.put("headers", headers);


        Event mapEvent = new Event("my/simple/topic", mapToDictionary(mapProps));

        assertEquals("complex event, map value in props", "{\"event.topics\":\"my/simple/topic\",\"headers\":{\"user-agent\":\"curl/7.25.0\"}}",
                EventLogger.constructMessage(mapEvent));

        Map<String, Object> stringSetProps = new LinkedHashMap<String, Object>();
        stringSetProps.put("resourceChangedAttributes", new LinkedHashSet<String>(Arrays.asList("first", "second")));


        Event stringSetEvent = new Event("my/simple/topic", mapToDictionary(stringSetProps));

        assertEquals("complex event, string set props", "{\"event.topics\":\"my/simple/topic\",\"resourceChangedAttributes\":[\"first\",\"second\"]}",
                EventLogger.constructMessage(stringSetEvent));
    }

    private static Dictionary<?, ?> mapToDictionary(Map<String, Object> props) {
        return new Hashtable<String, Object>(props);
    }
}
