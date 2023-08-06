/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync.servlet;

import com.adobe.acs.commons.contentsync.UpdateStrategy;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.StringReader;

import static com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.DEFAULT_GET_SERVLET;
import static com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.REDIRECT_SERVLET;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class TestContentCatalogServlet {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    private ContentCatalogServlet servlet;
    private UpdateStrategy updateStrategy;
    private ServletResolver servletResolver;

    @Before
    public void setUp() {
        servletResolver = mock(ServletResolver.class);
        updateStrategy = mock(UpdateStrategy.class);
        doAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            return resource.isResourceType("cq:Page");
        }).when(updateStrategy).accepts(any(Resource.class));
        context.registerService(UpdateStrategy.class, updateStrategy);
        context.registerService(ServletResolver.class, servletResolver);
        servlet = context.registerInjectActivateService(new ContentCatalogServlet());

        GenericServlet defaultServlet = mock(GenericServlet.class);
        doReturn(DEFAULT_GET_SERVLET).when(defaultServlet).getServletName();
        doReturn(defaultServlet).when(servletResolver).resolveServlet(any(SlingHttpServletRequest.class));
    }

    @Test
    public void testMissingRequiredParameters() throws IOException {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertEquals("root request parameter is required", jsonResponse.getString("error"));
    }

    /**
     * return an empty array if the requested path does not exist
     */
    @Test
    public void testContentTreeDoesNotExist() throws IOException {
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", "/content/wknd");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(SC_OK, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertTrue("resources[] is missing in the response json", jsonResponse.containsKey("resources"));

        JsonArray resources = jsonResponse.getJsonArray("resources");

        assertEquals(0, resources.size());
    }

    @Test
    public void testPageTree() throws IOException {
        context.create().page("/content/wknd");
        context.create().page("/content/wknd/page1");

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", "/content/wknd");
        request.addRequestParameter("strategy", updateStrategy.getClass().getName());
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(SC_OK, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertTrue("resources[] is missing in the response json", jsonResponse.containsKey("resources"));

        JsonArray resources = jsonResponse.getJsonArray("resources");
        assertEquals(2, resources.size());

        // first item is the root
        JsonObject item1 = resources.getJsonObject(0);
        assertEquals("/content/wknd", item1.getString("path"));
        assertEquals("cq:Page", item1.getString("jcr:primaryType"));

        JsonObject item2 = resources.getJsonObject(1);
        assertEquals("/content/wknd/page1", item2.getString("path"));
        assertEquals("cq:Page", item2.getString("jcr:primaryType"));
    }

    @Test
    public void testForwardRedirectServletToDefaultGetServlet() throws IOException {
        doAnswer(invocation -> {
            SlingHttpServletRequest request = invocation.getArgument(0, SlingHttpServletRequest.class);
            String resourcePath = request.getResource().getPath();
            GenericServlet servlet = mock(GenericServlet.class);
            if("/content/cq:tags".equals(resourcePath)){
                doReturn(REDIRECT_SERVLET).when(servlet).getServletName();
            }
            return servlet;
        }).when(servletResolver).resolveServlet(any(SlingHttpServletRequest.class));

        MockSlingHttpServletRequest request = context.request();

        String path = "/content/cq:tags";
        Resource resource = context.create().resource(path, "jcr:primaryType", "cq:Tag");
        String servletName = servlet.getJsonRendererServlet(request, resource, path + ".json");
        assertEquals(DEFAULT_GET_SERVLET, servletName);

    }

    @Test
    public void testSlingRedirectDefaultGetServlet() throws IOException {
        String path = "/content/cq:tags";
        doAnswer(invocation -> {
            SlingHttpServletRequest request = invocation.getArgument(0, SlingHttpServletRequest.class);
            String resourcePath = request.getResource().getPath();
            GenericServlet servlet = mock(GenericServlet.class);
            if(path.equals(resourcePath)){
                doReturn(REDIRECT_SERVLET).when(servlet).getServletName();
            }
            return servlet;
        }).when(servletResolver).resolveServlet(any(SlingHttpServletRequest.class));

        doAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            return resource.isResourceType("cq:Tag");
        }).when(updateStrategy).accepts(any(Resource.class));

        context.create().resource(path, "jcr:primaryType", "cq:Tag");

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", path);
        request.addRequestParameter("strategy", updateStrategy.getClass().getName());
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(SC_OK, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();

        JsonArray resources = jsonResponse.getJsonArray("resources");
        assertEquals(1, resources.size());

        JsonObject item = resources.getJsonObject(0);
        assertEquals(path, item.getString("path"));
        assertEquals("cq:Tag", item.getString("jcr:primaryType"));
        assertEquals("/content/cq:tags.json", item.getString("exportUri"));
        assertFalse("unexpected custom json exporter: " + item.get("renderServlet"), item.containsKey("renderServlet"));

    }

    /**
     *
     * /conf/wknd/settings/wcm/templates/article-page-template/policies (cq:Page) export json rendered  by DefaultGetServlet
     *   + jcr:content - export json rendered by ContentPolicyMappingServlet
     *
     */
    @Test
    public void testCustomRendererUseParent() throws IOException {
        String path = "/conf/wknd/settings/wcm/templates/article-page-template/policies";
        doAnswer(invocation -> {
            SlingHttpServletRequest request = invocation.getArgument(0, SlingHttpServletRequest.class);
            String resourcePath = request.getResource().getPath();
            GenericServlet servlet = mock(GenericServlet.class);
            if(resourcePath.equals(path + "/jcr:content")){
                doReturn("com.day.cq.wcm.core.impl.policies.ContentPolicyMappingServlet").when(servlet).getServletName();
            } else if(resourcePath.equals(path)){
                doReturn(DEFAULT_GET_SERVLET).when(servlet).getServletName();
            }
            return servlet;
        }).when(servletResolver).resolveServlet(any(SlingHttpServletRequest.class));

        context.create().page(path);

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", path);
        request.addRequestParameter("strategy", updateStrategy.getClass().getName());
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(SC_OK, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();

        JsonArray resources = jsonResponse.getJsonArray("resources");
        assertEquals(1, resources.size());

        JsonObject item = resources.getJsonObject(0);
        assertEquals(path, item.getString("path"));
        assertEquals("cq:Page", item.getString("jcr:primaryType"));
        assertEquals(path + ".infinity.json", item.getString("exportUri"));
        assertFalse("unexpected custom json exporter: " + item.get("renderServlet"), item.containsKey("renderServlet"));

    }

    @Test
    public void testCustomExporter() throws IOException {
        String path = "/content/page";
        String customExporter = "com.adobe.CustomJsonExporter";
        doAnswer(invocation -> {
            SlingHttpServletRequest request = invocation.getArgument(0, SlingHttpServletRequest.class);
            String resourcePath = request.getResource().getPath();
            GenericServlet servlet = mock(GenericServlet.class);
            if(path.equals(resourcePath)){
                doReturn(customExporter).when(servlet).getServletName();
            }
            return servlet;
        }).when(servletResolver).resolveServlet(any(SlingHttpServletRequest.class));

        context.create().page(path);

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", path);
        request.addRequestParameter("strategy", updateStrategy.getClass().getName());
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(SC_OK, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();

        JsonArray resources = jsonResponse.getJsonArray("resources");
        assertEquals(1, resources.size());

        JsonObject item = resources.getJsonObject(0);
        assertEquals(path, item.getString("path"));
        assertEquals("cq:Page", item.getString("jcr:primaryType"));
        assertEquals("/content/page.infinity.json", item.getString("exportUri"));
        assertEquals(customExporter, item.getString("renderServlet"));

    }
}
