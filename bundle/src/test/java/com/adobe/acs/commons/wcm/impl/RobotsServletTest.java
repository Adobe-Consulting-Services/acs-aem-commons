/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import com.day.cq.commons.Externalizer;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class RobotsServletTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private Externalizer externalizer;


    private MockSlingHttpServletRequest request;

    private MockSlingHttpServletResponse response;

    @Before
    public void setup() {
        response = context.response();
        request = context.request();

        context.load().json(getClass().getResourceAsStream("RobotsServlet.json"), "/content/geometrixx");

        context.registerService(Externalizer.class, externalizer);
        request.setResource(context.resourceResolver().getResource("/content/geometrixx/en/jcr:content"));


        when(externalizer.externalLink(eq(context.resourceResolver()), eq("publish"), anyString())).then(i -> "https://www.geometrixx.com" + i.getArgument(2));

    }

    @Test
    public void testWriteFromPageProperty() {
        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("robots.content.property.path", "robotsContents");
        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);

        try {
            robotsServlet.doGet(request, response);
            assertEquals("servlet returned an error", 200, response.getStatus());
            assertResponse(getClass().getResourceAsStream("RobotsServlet_testWriteFromPageProperty.txt"), response);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }

    @Test
    public void testWriteFromAsset() {
        context.create().asset("/content/dam/geometrixx/robots.txt", getClass().getResourceAsStream("RobotsServlet_testWriteFromAsset.txt"), "text/plain");

        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("robots.content.property.path", "/content/dam/geometrixx/robots.txt/jcr:content/renditions/original/jcr:content/jcr:data");
        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);

        try {
            robotsServlet.doGet(request, response);
            assertEquals("servlet returned an error", 200, response.getStatus());
            assertResponse(getClass().getResourceAsStream("RobotsServlet_testWriteFromAsset.txt"), response);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }

    @Test
    public void testWriteFromPageProperties() {

        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("user.agent.directives", new String[]{
                "*",
                "google:googlebot"
        });
        props.put("allow.property.names", new String[]{
                "allowRobots",
                "google:allowRobots"
        });
        props.put("disallow.property.names", new String[]{
                "denyRobots",
                "google:denyGoogle"
        });

        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);

        try {
            robotsServlet.doGet(request, response);
            assertEquals("servlet returned an error", 200, response.getStatus());
            assertResponse(getClass().getResourceAsStream("RobotsServlet_testWriteFromPageProperties.txt"), response);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }

    @Test
    public void testWriteFromOsgiConfigSimple() {

        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("user.agent.directives", new String[]{
                "*"
        });
        props.put("disallow.directives", new String[]{
                "/"
        });

        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);

        try {
            robotsServlet.doGet(request, response);
            assertEquals("servlet returned an error", 200, response.getStatus());
            assertResponse(getClass().getResourceAsStream("RobotsServlet_testWriteFromOsgiConfigSimple.txt"), response);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }

    @Test
    public void testWriteFromOsgiConfig() {

        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("print.grouping.comments", true);
        props.put("user.agent.directives", new String[]{
                "one:googlebot",
                "two:bingbot",
                "two:yahoobot",
                "three:*"
        });
        props.put("allow.directives", new String[]{
                "one:/botsOnly/",
                "one:/onlyGoogle/",
                "three:/content/geometrixx/en"
        });
        props.put("disallow.directives", new String[]{
                "one:/noGoogle/",
                "one:/stillNoGoogle/",
                "two:/content/geometrixx/en/no-bots",
        });
        props.put("sitemap.directives", new String[]{
                "https://www.geometrixx.com/sitemap.xml",
                "/content/geometrixx/en"
        });

        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);

        try {
            robotsServlet.doGet(request, response);
            assertEquals("servlet returned an error", 200, response.getStatus());
            assertResponse(getClass().getResourceAsStream("RobotsServlet_testWriteFromOsgiConfig.txt"), response);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }

    @Test
    public void testWriteFromNonExistentPropertyAbsolute() {

        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("robots.content.property.path", "/content/dam/geometrixx/robots.txt/jcr:content/renditions/original/jcr:content/jcr:data");
        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);

        try {
            robotsServlet.doGet(request, response);
            assertEquals("servlet did not return the expected error", 404, response.getStatus());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }

    @Test
    public void testWriteFromNonExistentPropertyRelative() {

        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("robots.content.property.path", "jcr:content/thisPropDoesntExist");
        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);

        try {
            robotsServlet.doGet(request, response);
            assertEquals("servlet did not return the expected error", 404, response.getStatus());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }

    private void assertResponse(InputStream resourceAsStream, MockSlingHttpServletResponse response) throws IOException {
        String output = response.getOutputAsString();
        String expected = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
        assertEquals(expected, output);
    }

}
