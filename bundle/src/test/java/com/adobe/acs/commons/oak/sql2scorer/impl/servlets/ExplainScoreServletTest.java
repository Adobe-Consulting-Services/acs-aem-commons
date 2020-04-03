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
package com.adobe.acs.commons.oak.sql2scorer.impl.servlets;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class ExplainScoreServletTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExplainScoreServletTest.class);

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Test
    public void test_doPost_noParams() throws Exception {
        ExplainScoreServlet servlet = new ExplainScoreServlet();
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        servlet.doPost(request, response);
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.getOutputAsString(), JsonObject.class);
        assertTrue("response should have error key", json.has("error"));

        LOGGER.info("response string: {}", json.get("error").getAsString());
    }

    @Test
    public void test_doPost_xpath_fail() throws Exception {
        ExplainScoreServlet servlet = new ExplainScoreServlet();
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        Map<String, Object> params = new HashMap<>();
        params.put("statement", "//element(*, cq:Page)");
        request.setParameterMap(params);

        servlet.doPost(request, response);
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.getOutputAsString(), JsonObject.class);
        assertTrue("response should have error key", json.has("error"));

        LOGGER.info("response string: {}", json.get("error").getAsString());
        assertTrue("error should contain 'ParseException'",
                json.get("error").getAsString().contains("ParseException"));
    }

    @Test
    public void test_doPost_sql2_fail_logout() throws Exception {
        final Map<String, Object> params = new HashMap<>();
        params.put("statement", "select * from [nt:base]");

        final ExplainScoreServlet servlet = new ExplainScoreServlet();
        final MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        final MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        request.setParameterMap(params);

        final Session jcr = context.resourceResolver().adaptTo(Session.class);

        jcr.logout();

        servlet.doPost(request, response);
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.getOutputAsString(), JsonObject.class);
        assertTrue("response should have error key", json.has("error"));

        LOGGER.info("response string: {}", json.get("error").getAsString());
    }

    @Test
    public void test_doPost_sql2_happy_no_score() throws Exception {
        final Map<String, Object> params = new HashMap<>();
        params.put("statement", "select [jcr:path] from [nt:base] where ISSAMENODE([nt:base], '/')");
        params.put("limit", "1");
        params.put("offset", "0");

        ExplainScoreServlet servlet = new ExplainScoreServlet();
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(context.resourceResolver());
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        request.setParameterMap(params);

        servlet.doPost(request, response);
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.getOutputAsString(), JsonObject.class);
        assertFalse("response should not have error key", json.has("error"));

        assertTrue("response should have stmt key", json.has("stmt"));
        LOGGER.info("stmt: {}", json.get("stmt").getAsString());
        assertTrue("stmt should contain 'oak:scoreExplanation'",
                json.get("stmt").getAsString().contains("oak:scoreExplanation"));

        assertTrue("response should have plan key", json.has("plan"));
        LOGGER.info("plan: {}", json.get("plan").getAsString());
        assertTrue("plan should contain 'traverse'",
                json.get("plan").getAsString().contains("traverse"));

        assertTrue("response should have cols key", json.has("cols"));
        LOGGER.info("cols: {}", json.get("cols"));
        assertTrue("cols should contain 'oak:scoreExplanation'",
                json.get("cols").getAsJsonArray().contains(new JsonPrimitive("oak:scoreExplanation")));
        assertTrue("cols should contain 'jcr:path'",
                json.get("cols").getAsJsonArray().contains(new JsonPrimitive("jcr:path")));

        assertTrue("response should have rows key", json.has("rows"));
        LOGGER.info("rows: {}", json.get("rows"));
        assertTrue("rows should not be empty", json.get("rows").getAsJsonArray().size() > 0);
        assertTrue("rows[0][0] should contain JsonElement.nullValue()",
                json.get("rows").getAsJsonArray().get(0).getAsJsonArray().get(0).isJsonNull());
        assertEquals("rows[0][1] should be the root path", "/",
                json.get("rows").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsString());
    }

    @Test
    public void test_TypeAdapter_write_fail() throws Exception {
        final Session session = context.resourceResolver().adaptTo(Session.class);
        final ExplainScoreServlet servlet = new ExplainScoreServlet();
        final ExplainScoreServlet.QueryExecutingTypeAdapter adapter =
                servlet.new QueryExecutingTypeAdapter(session.getWorkspace().getQueryManager());
        final JsonWriter mockWriter = mock(JsonWriter.class);
        final Query mockQuery = mock(Query.class);
        when(mockQuery.execute()).thenThrow(new RepositoryException("I am a RepositoryException!"));

        boolean caughtException = false;
        try {
            adapter.write(mockWriter, mockQuery);
        } catch (final IOException e) {
            caughtException = true;
        }

        assertTrue("adapter.write() should throw IOException when RepositoryException is encountered",
                caughtException);
    }

    @Test
    public void test_TypeAdapter_read_fail() throws Exception {
        final Session session = context.resourceResolver().adaptTo(Session.class);
        final ExplainScoreServlet servlet = new ExplainScoreServlet();
        final ExplainScoreServlet.QueryExecutingTypeAdapter adapter =
                servlet.new QueryExecutingTypeAdapter(session.getWorkspace().getQueryManager());
        final JsonReader mockReader = mock(JsonReader.class);

        boolean caughtException = false;
        try {
            adapter.read(mockReader);
        } catch (final UnsupportedOperationException e) {
            caughtException = true;
        }

        assertTrue("adapter.read() should throw UnsupportedOperationException when called",
                caughtException);
    }

    @Test
    public void test_TypeAdapter_writeValue() throws Exception {
        final Session session = context.resourceResolver().adaptTo(Session.class);
        final ExplainScoreServlet servlet = new ExplainScoreServlet();
        final ExplainScoreServlet.QueryExecutingTypeAdapter adapter =
                servlet.new QueryExecutingTypeAdapter(session.getWorkspace().getQueryManager());
        final JsonWriter mockWriter = mock(JsonWriter.class);
        Binary binary = session.getValueFactory()
                .createBinary(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        adapter.writeValue(mockWriter, null);
        adapter.writeValue(mockWriter, session.getValueFactory().createValue(binary));
        adapter.writeValue(mockWriter, session.getValueFactory().createValue(1L));
        adapter.writeValue(mockWriter, session.getValueFactory().createValue(Calendar.getInstance()));
        adapter.writeValue(mockWriter, session.getValueFactory().createValue(true));
        adapter.writeValue(mockWriter, session.getValueFactory().createValue("hello"));
        adapter.writeValue(mockWriter, session.getValueFactory().createValue(42.0D));
        adapter.writeValue(mockWriter, session.getValueFactory().createValue(new BigDecimal("42.0")));
    }
}
