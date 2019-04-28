package com.adobe.acs.commons.httpcache.config.impl.keys;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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