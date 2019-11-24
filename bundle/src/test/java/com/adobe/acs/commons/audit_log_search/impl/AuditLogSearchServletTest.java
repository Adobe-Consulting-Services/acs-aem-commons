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
package com.adobe.acs.commons.audit_log_search.impl;

import com.adobe.granite.security.user.UserPropertiesManager;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

import javax.annotation.Nullable;
import javax.jcr.Session;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class AuditLogSearchServletTest {

    @Mock
    private UserPropertiesManager userPropertiesManager;

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private AuditLogSearchServlet servlet = new AuditLogSearchServlet();

    @Before
    public void setup() throws Exception {
        context.registerAdapter(ResourceResolver.class, UserPropertiesManager.class, userPropertiesManager);
        context.registerAdapter(ResourceResolver.class, UserManager.class, new Function<ResourceResolver, UserManager>() {
            @Nullable
            @Override
            public UserManager apply(@Nullable ResourceResolver input) {
                try {
                    return ((JackrabbitSession) input.adaptTo(Session.class)).getUserManager();
                } catch (Exception e) {
                    return null;
                }
            }
        });
        Session session = context.resourceResolver().adaptTo(Session.class);
        Reader reader = new InputStreamReader(getClass().getResourceAsStream("audit.cnd"), "UTF-8");
        CndImporter.registerNodeTypes(reader, session);
        context.load().json(getClass().getResourceAsStream("AuditLogSearchServletTest.json"), "/var/audit");
    }

    @Test
    public void testAll() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setMethod("GET");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);
        String output = response.getOutputAsString();
        JsonObject json = new Gson().fromJson(output, JsonObject.class);
        assertEquals(4, json.getAsJsonPrimitive("count").getAsInt());
    }

    @Test
    public void testExactContentPath() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(Collections.singletonMap("contentRoot", "/content/geometrixx"));
        request.setMethod("GET");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);
        String output = response.getOutputAsString();
        JsonObject json = new Gson().fromJson(output, JsonObject.class);
        assertEquals(1, json.getAsJsonPrimitive("count").getAsInt());
    }

    @Test
    public void testSubContentPath() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        Map<String, Object> params = new HashMap<>();
        params.put("contentRoot", "/content/geometrixx");
        params.put("includeChildren", "true");
        request.setParameterMap(params);
        request.setMethod("GET");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);
        String output = response.getOutputAsString();
        JsonObject json = new Gson().fromJson(output, JsonObject.class);
        assertEquals(2, json.getAsJsonPrimitive("count").getAsInt());
    }

    @Test
    public void testDateWindow() throws Exception {
        final MockSlingHttpServletRequest request = context.request();
        final Map<String, Object> params = new HashMap<>();
        params.put("contentRoot", "/content");
        params.put("includeChildren", "true");
        params.put("startDate", "2017-11-01T01:00");
        params.put("endDate", "2017-11-01T23:00");
        request.setParameterMap(params);
        request.setMethod("GET");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);
        String output = response.getOutputAsString();
        JsonObject json = new Gson().fromJson(output, JsonObject.class);
        assertEquals(2, json.getAsJsonPrimitive("count").getAsInt());
    }

    @Test
    public void testDateWindowAfterNoon() throws Exception {
        final MockSlingHttpServletRequest request = context.request();
        final Map<String, Object> params = new HashMap<>();
        params.put("contentRoot", "/content");
        params.put("includeChildren", "true");
        params.put("startDate", "2017-11-01T16:00");
        params.put("endDate", "2017-11-01T23:00");
        request.setParameterMap(params);
        request.setMethod("GET");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);
        String output = response.getOutputAsString();
        JsonObject json = new Gson().fromJson(output, JsonObject.class);
        assertEquals(1, json.getAsJsonPrimitive("count").getAsInt());
    }
}
