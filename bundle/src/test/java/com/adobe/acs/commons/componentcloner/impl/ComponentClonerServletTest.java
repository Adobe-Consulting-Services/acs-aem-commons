/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.componentcloner.impl;

import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ComponentClonerServlet}.
 */
public class ComponentClonerServletTest {

    private static String COMPONENT_CLONER_PATH = "/content/geometrixx/en/company/jcr:content/par/component-cloner";
    private static String COMPONENT_CLONER_PATH_PARENT = "/content/geometrixx/en/company/jcr:content/par";
    private static String PATH_TO_CLONE = "/content/geometrixx/en/company/management/jcr:content/par/john_doe_ceo";
    private static String BOGUS_PATH_TO_CLONE = "/content/geometrixx/en/bogus/jcr:content/par/john_doe_ceo";
    private static String JSON_SUCCESS = "{\"componentClonerError\":false}";
    private static String JSON_ERROR = "{\"componentClonerError\":true}";

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @InjectMocks
    private ComponentClonerServlet servlet = new ComponentClonerServlet();

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    /**
     * Logic to be ran upon initialization.
     * @throws Exception exception
     */
    @Before
    public void setUp() throws Exception {
        this.context.load().json(getClass().getResourceAsStream("ComponentClonerServlet.json"), "/content/geometrixx");
        this.response = this.context.response();
        this.request = this.context.request();
        this.request.setResource(this.context.resourceResolver().getResource(COMPONENT_CLONER_PATH));
    }

    /**
     * Unit test for successful execution {@link ComponentClonerServlet#doGet(SlingHttpServletRequest, SlingHttpServletResponse)}.
     * @throws Exception exception
     */
    @Test
    public void testSuccessDoGet() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("path", PATH_TO_CLONE);

        this.request.setParameterMap(parameters);
        this.servlet = spy(this.servlet);

        doAnswer(invocationOnMock -> {
            Resource r = (Resource) invocationOnMock.getArguments()[0];
            Node n = (Node) invocationOnMock.getArguments()[1];

            assertEquals(COMPONENT_CLONER_PATH, r.getPath());
            assertEquals(PATH_TO_CLONE, n.getPath());

            return null;
        }).when(this.servlet).executeNodeCloning(any(Resource.class), any(Node.class));

        this.servlet.doGet(this.request, this.response);
        verify(this.servlet).executeNodeCloning(any(Resource.class), any(Node.class));

        String output = this.response.getOutputAsString();
        assertEquals(JSON_SUCCESS, output);
    }

    /**
     * Unit test for successful execution {@link ComponentClonerServlet#createUniqueNodeName(int, String, Node)}.
     * @throws Exception exception
     */
    @Test
    public void testSuccessCreateUniqueNodeName() throws Exception {
        Node parentNode = this.context.resourceResolver().getResource(COMPONENT_CLONER_PATH_PARENT).adaptTo(Node.class);
        String outputName = this.servlet.createUniqueNodeName(0, "john_doe_ceo", parentNode);
        assertEquals("john_doe_ceo_0", outputName);
    }

    /**
     * Unit test for failed execution when {@link ComponentClonerServlet}'s request param is null/blank.
     * @throws Exception exception
     */
    @Test
    public void testFailurePathBlank() throws Exception {
        this.servlet.doGet(this.request, this.response);

        String output = this.response.getOutputAsString();
        assertEquals(JSON_ERROR, output);
    }

    /**
     * Unit test for failed execution when {@link ComponentClonerServlet}'s request param does not exist in the JCR.
     * @throws Exception exception
     */
    @Test
    public void testFailurePathDoesNotExist() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("path", BOGUS_PATH_TO_CLONE);

        this.request.setParameterMap(parameters);
        this.servlet.doGet(this.request, this.response);

        String output = this.response.getOutputAsString();
        assertEquals(JSON_ERROR, output);
    }

    /**
     * Unit test for failed execution when {@link ComponentClonerServlet} throws an exception.
     * @throws Exception exception
     */
    @Test
    public void testFailureExceptionOccurred() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("path", PATH_TO_CLONE);

        this.request.setParameterMap(parameters);
        this.request.setResource(null);
        this.servlet.doGet(this.request, this.response);

        String output = this.response.getOutputAsString();
        assertEquals(JSON_ERROR, output);
    }
}
