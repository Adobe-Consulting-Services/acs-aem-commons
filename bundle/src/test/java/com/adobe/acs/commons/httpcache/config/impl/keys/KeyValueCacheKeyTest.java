/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.config.impl.keys;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class KeyValueCacheKeyTest {

    @Mock
    private HttpCacheConfig cacheConfig;

    private static final HashMap<String,String[]> TESTMAP = new HashMap();

    static{
        TESTMAP.put("SomeValue", new String[]{"Value1", "Value2"});
        TESTMAP.put("SomeOtherValue", new String[]{"Value1"});
    }

    @Test
    public void test_serialization(){

        KeyValueCacheKey cacheKey = new KeyValueCacheKey("/content/acs-commons/en/homepage.html", cacheConfig, "synthetic", TESTMAP);

        byte[] serializedByteArray = SerializationUtils.serialize(cacheKey);

        KeyValueCacheKey unserialized = SerializationUtils.deserialize(serializedByteArray);

        assertEquals(cacheKey, unserialized);

        Map<String, String[]> unserializedMap = unserialized.getAllowedKeyValues();

        assertTrue(unserializedMap.containsKey("SomeValue"));
        assertTrue(unserializedMap.containsKey("SomeOtherValue"));

        assertArrayEquals(TESTMAP.get("SomeValue"), unserializedMap.get("SomeValue"));
        assertArrayEquals(TESTMAP.get("SomeOtherValue"), unserializedMap.get("SomeOtherValue"));


    }

}