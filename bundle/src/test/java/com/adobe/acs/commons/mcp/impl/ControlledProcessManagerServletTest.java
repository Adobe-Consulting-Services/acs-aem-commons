/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.mcp.impl;

import com.google.gson.Gson;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test various facilities of the MCP servlet
 */
@RunWith(MockitoJUnitRunner.class)
public class ControlledProcessManagerServletTest {

    ControlledProcessManagerServlet servlet;

    @Before
    public void setUp() {
        servlet = new ControlledProcessManagerServlet();
    }

    /**
     * Test of getGson method, of class ControlledProcessManagerServlet.
     */
    @Test
    public void testGetGson() {
        Gson gson = servlet.getGson();
        String test = gson.toJson(new SerializationTestBean());
        assertNotNull(test);
        assertTrue("Should serialize strings and ints", test.contains("serialize"));
        assertFalse("Serialize skips transient, volatile, and various classes", test.contains("noSerialize"));
    }

    /**
     * Test of convertRequestMap method, of class
     * ControlledProcessManagerServlet.
     */
    @Test
    public void testConvertRequestMap() {
        Map<String, RequestParameter[]> params = new HashMap<>();
        params.put("singleValue", getRequestParameter(null, "value"));
        params.put("multiValue", getRequestParameter(null, "value1", "value2", "value3"));
        params.put("fileUpload", getRequestParameter("filename", "value"));

        RequestParameterMap requestParameterMap = mock(RequestParameterMap.class);
        when(requestParameterMap.entrySet()).thenReturn(params.entrySet());
        Map<String, Object> converted = servlet.convertRequestMap(requestParameterMap);
        assertEquals("value", converted.get("singleValue"));
        assertTrue("Should be request parameter", RequestParameter.class.isAssignableFrom(converted.get("fileUpload").getClass()));
        assertTrue("Should be list", List.class.isAssignableFrom(converted.get("multiValue").getClass()));
        List<String> multiValue = (List<String>) converted.get("multiValue");
        assertArrayEquals(new String[]{"value1", "value2", "value3"}, multiValue.toArray());
    }

    private RequestParameter[] getRequestParameter(String fileName, String... values) {
        List<RequestParameter> params = new ArrayList<>();
        for (String val : values) {
            RequestParameter param = mock(RequestParameter.class);
            when(param.getFileName()).thenReturn(fileName);
            when(param.getString()).thenReturn(val);
            params.add(param);
        }
        return params.toArray(new RequestParameter[0]);
    }

    public static class SerializationTestBean {

        public String serialize1 = "ok";
        public transient String noSerialize2 = "skip";
        public volatile String noSerialize3 = "skip";
        public byte[] noSerialize4 = "skip".getBytes();
        public InputStream noSerialize5 = mock(InputStream.class);
        public Resource noSerialize6 = mock(Resource.class);
        public int serialize7 = 1;
    }
}
