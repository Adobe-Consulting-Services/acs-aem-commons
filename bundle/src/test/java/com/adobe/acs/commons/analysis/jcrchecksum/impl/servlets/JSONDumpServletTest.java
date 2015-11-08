/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum.impl.servlets;

import com.adobe.acs.commons.analysis.jcrchecksum.impl.servlets.JSONDumpServlet;
import com.day.cq.widget.HtmlLibraryManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JSONDumpServletTest {

    private static final String SERVLET_PATH = "/bin/acs-commons/jcr-compare";

    private static final String SERVLET_SELECTORS = "dump";

    private static final String SERVLET_EXTENSION = "json";

    @Mock
    private HtmlLibraryManager manager;

    @InjectMocks
    public JSONDumpServlet servlet = new JSONDumpServlet();

    MockSlingHttpServletRequest request;
    ResourceResolver resourceResolver;
    MockSlingHttpServletResponse response;
    Session session;

    @Before
    public void setUp() throws LoginException {
        response = new MockSlingHttpServletResponse();
        session = MockJcr.newSession();
        this.resourceResolver = mock(ResourceResolver.class);
        when(this.resourceResolver.adaptTo(Session.class)).thenReturn(session);
    }

    @Test
    public void testWithNoPath() throws Exception {
        this.resourceResolver = mock(ResourceResolver.class);
        this.request =
            new MockSlingHttpServletRequest(SERVLET_PATH, SERVLET_SELECTORS, SERVLET_EXTENSION, null,
                null) {
                public ResourceResolver getResourceResolver() {
                    return resourceResolver;
                };
            };
        this.response = new MockSlingHttpServletResponse() {
                public void setHeader(String header, String value) {
                    //do nothing
                    return;
                };
            };
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals("ERROR: At least one path must be specified", response
            .getOutput().toString());
    }

    @Test
    public void testWithPath() throws Exception {
        Node contentNode = session.getRootNode().addNode("content", "nt:unstructured");

        contentNode.addNode("my-page", "cq:Page")
                .addNode("jcr:content", "cq:PageContent")
                .setProperty("jcr:title", "My Page");

        contentNode.addNode("your-page", "cq:Page")
                .addNode("jcr:content", "cq:PageContent")
                .setProperty("jcr:title", "Your Page");

        this.request =
            new MockSlingHttpServletRequest(SERVLET_PATH, SERVLET_SELECTORS, SERVLET_EXTENSION, null,
                null) {
                public ResourceResolver getResourceResolver() {
                    return resourceResolver;
                };

                public String getParameter(String name) {
                    if("optionsName".equals(name)) {
                        return "REQUEST";
                    } else {
                        return null;
                    }
                }

                public String[] getParameterValues(String name) {
                    if ("paths".equals(name)) {
                        return new String[]{ "/content" };
                    } else if ("nodeTypes".equals(name)) {
                        return new String[]{ "cq:PageContent" };

                    } else {
                        return null;
                    }
                };
            };
        this.response = new MockSlingHttpServletResponse() {
                public void setHeader(String header, String value) {
                    //do nothing
                    return;
                };
            };
        servlet.doGet(request, response);
        assertEquals("application/json", response.getContentType());
        assertEquals(
            "{\"/content/my-page/jcr:content\":{\"jcr:primaryType\":\"cq:PageContent\"," +
                    "\"jcr:title\":\"My Page\"}," +
                    "\"/content/your-page/jcr:content\":{\"jcr:primaryType\":\"cq:PageContent\"," +
                    "\"jcr:title\":\"Your Page\"}}",
            response.getOutput().toString());
    }
}

