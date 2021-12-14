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

import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import com.adobe.acs.commons.mcp.model.impl.ArchivedProcessInstance;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.ServletException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test various facilities of the MCP servlet
 */
@RunWith(MockitoJUnitRunner.class)
public class ControlledProcessManagerServletTest {
    @Rule
    public AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    ControlledProcessManager manager;

    @InjectMocks
    ControlledProcessManagerServlet servlet = new ControlledProcessManagerServlet();;

    @Before
    public void setUp() {
        ctx.addModelsForClasses(ArchivedProcessInstance.class, ManagedProcess.class);

        List<ProcessInstance> activeProcesses = new ArrayList<>();
        ArchivedProcessInstance p1 = new ArchivedProcessInstance();
        ManagedProcess infoBean = new ManagedProcess();
        p1.infoBean = infoBean;

        List<ProcessInstance> inactiveProcesses = new ArrayList<>();
        inactiveProcesses.add(p1);
        when(manager.getActiveProcesses()).thenReturn(activeProcesses);
        when(manager.getInactiveProcesses()).thenReturn(inactiveProcesses);
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

    @Test // #2749: Manage Controlled Processes does not show any process
    public void test2749() throws IOException, ServletException {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        RequestPathInfo pathInfo = mock(RequestPathInfo.class);
        when(pathInfo.getSelectorString()).thenReturn("list");
        when(request.getRequestPathInfo()).thenReturn(pathInfo);

        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (PrintWriter writer = new PrintWriter(bos)) {
                when(response.getWriter()).thenReturn(writer);
                servlet.doGet(request, response);
            } // the servlet engine closes the response stream

            String json = bos.toString(); // the string returned in the response
            Type list = new TypeToken<ArrayList<ArchivedProcessInstance>>() {}.getType();
            List<ProcessInstance> instances = servlet.getGson().fromJson(json, list);
            assertEquals(1, instances.size());
        }
    }
}
