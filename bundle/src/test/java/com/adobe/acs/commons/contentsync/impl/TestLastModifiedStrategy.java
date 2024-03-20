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
package com.adobe.acs.commons.contentsync.impl;

import com.adobe.acs.commons.contentsync.CatalogItem;
import com.adobe.acs.commons.contentsync.UpdateStrategy;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.GenericServlet;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import static com.adobe.acs.commons.contentsync.impl.LastModifiedStrategy.DEFAULT_GET_SERVLET;
import static com.adobe.acs.commons.contentsync.impl.LastModifiedStrategy.REDIRECT_SERVLET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class TestLastModifiedStrategy {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    private UpdateStrategy updateStrategy;

    private ServletResolver servletResolver;

    @Before
    public void setUp() {
        servletResolver = mock(ServletResolver.class);
        context.registerService(ServletResolver.class, servletResolver);
        updateStrategy = context.registerInjectActivateService(new LastModifiedStrategy());
    }

    /**
     * isModified() returns false if cq:lastModified/jcr:lastModified is not set
     */
    @Test
    public void testLastModifiedNA() {
        String pagePath = "/content/wknd/page";
        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", pagePath)
                .add("jcr:primaryType", "cq:Page")
                .build();

        Page page = context.create().page(pagePath);
        Resource pageResource = page.adaptTo(Resource.class);
        assertFalse(updateStrategy.isModified(new CatalogItem(catalogItem), pageResource));
    }

    @Test
    public void testPageModified() {
        String pagePath = "/content/wknd/page";
        ZonedDateTime remoteTimestamp = ZonedDateTime.now().minusDays(1);
        ZonedDateTime localTimestamp = ZonedDateTime.now().minusDays(2);

        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", pagePath)
                .add("jcr:primaryType", "cq:Page")
                .add("lastModified", remoteTimestamp.toInstant().toEpochMilli())
                .build();

        Page page = context.create().page(pagePath, null, Collections.singletonMap("cq:lastModified", GregorianCalendar.from(localTimestamp)));
        Resource pageResource = page.adaptTo(Resource.class);

        assertTrue(updateStrategy.isModified(new CatalogItem(catalogItem), pageResource));
    }

    @Test
    public void testPageNotModified() {
        String pagePath = "/content/wknd/page";
        ZonedDateTime localTimestamp = ZonedDateTime.now().minusDays(1);
        ZonedDateTime remoteTimestamp = ZonedDateTime.now().minusDays(2);

        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", pagePath)
                .add("jcr:primaryType", "cq:Page")
                .add("lastModified", remoteTimestamp.toInstant().toEpochMilli())
                .build();

        Page page = context.create().page(pagePath, null, Collections.singletonMap("cq:lastModified", GregorianCalendar.from(localTimestamp)));
        Resource pageResource = page.adaptTo(Resource.class);

        assertFalse(updateStrategy.isModified(new CatalogItem(catalogItem), pageResource));
    }


    @Test
    public void testForwardRedirectServletToDefaultGetServlet() {
        doAnswer(invocation -> {
            GenericServlet servlet = mock(GenericServlet.class);
            doReturn(REDIRECT_SERVLET).when(servlet).getServletName();
            return servlet;
        }).when(servletResolver).resolveServlet(any(SlingHttpServletRequest.class));

        String path = "/content/cq:tags";
        context.create().resource(path, "jcr:primaryType", "cq:Tag");

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", path);

        List<CatalogItem> items = updateStrategy.getItems(request);
        assertEquals(1, items.size());
        CatalogItem item = items.iterator().next();
        assertEquals("/content/cq:tags.json", item.getContentUri());
        assertNull(item.getCustomExporter());
    }

    /**
     * /conf/wknd/settings/wcm/templates/article-page-template/policies (cq:Page) export json rendered  by DefaultGetServlet
     * + jcr:content - export json rendered by ContentPolicyMappingServlet
     */
    @Test
    public void testCustomRendererUseParent() throws IOException {
        String path = "/conf/wknd/settings/wcm/templates/article-page-template/policies";
        doAnswer(invocation -> {
            SlingHttpServletRequest request = invocation.getArgument(0, SlingHttpServletRequest.class);
            String resourcePath = request.getResource().getPath();
            GenericServlet servlet = mock(GenericServlet.class);
            if (resourcePath.equals(path + "/jcr:content")) {
                doReturn("com.day.cq.wcm.core.impl.policies.ContentPolicyMappingServlet").when(servlet).getServletName();
            } else if (resourcePath.equals(path)) {
                doReturn(DEFAULT_GET_SERVLET).when(servlet).getServletName();
            }
            return servlet;
        }).when(servletResolver).resolveServlet(any(SlingHttpServletRequest.class));

        context.create().page(path);

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", path);
        request.addRequestParameter("strategy", updateStrategy.getClass().getName());

        List<CatalogItem> items = updateStrategy.getItems(request);
        assertEquals(1, items.size());
        CatalogItem item = items.iterator().next();
        assertEquals("cq:Page", item.getPrimaryType());
        assertEquals(path + ".infinity.json", item.getContentUri());
        assertEquals(null, item.getCustomExporter());

    }

    @Test
    public void testCustomExporter() {
        String path = "/content/wknd/page";
        String customExporter = "com.adobe.CustomJsonExporter";
        doAnswer(invocation -> {
            GenericServlet servlet = mock(GenericServlet.class);
            doReturn(customExporter).when(servlet).getServletName();
            return servlet;
        }).when(servletResolver).resolveServlet(any(SlingHttpServletRequest.class));

        context.create().page(path);

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", path);
        request.addRequestParameter("strategy", updateStrategy.getClass().getName());

        List<CatalogItem> items = updateStrategy.getItems(request);
        assertEquals(1, items.size());
        CatalogItem item = items.iterator().next();
        assertEquals("cq:Page", item.getPrimaryType());
        assertEquals(path + ".infinity.json", item.getContentUri());
        assertEquals(customExporter, item.getCustomExporter());
    }
}
