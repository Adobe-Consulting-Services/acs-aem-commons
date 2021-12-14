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
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;

import io.wcm.testing.mock.aem.MockExternalizer;
import io.wcm.testing.mock.aem.MockPageManagerFactory;
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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import static org.junit.Assert.assertEquals;


public class RobotsServletTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private MockSlingHttpServletRequest request;

    private MockSlingHttpServletResponse response;

    @Before
    public void setup() {
        response = context.response();
        request = context.request();

        context.load().json(getClass().getResourceAsStream("RobotsServlet.json"), "/content/geometrixx");
        request.setResource(context.resourceResolver().getResource("/content/geometrixx/en/jcr:content"));
        MockExternalizer externalizer = (MockExternalizer)context.getService(Externalizer.class);
        externalizer.setMapping("publish", "https://www.geometrixx.com");
    }

    @Test
    public void testWriteFromPageProperty() throws IOException, ServletException {
        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("robots.content.property.path", "robotsContents");
        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);
        robotsServlet.doGet(request, response);
        assertEquals("servlet returned an error", 200, response.getStatus());
        assertResponse("RobotsServlet_testWriteFromPageProperty.txt", response);
    }

    @Test
    public void testWriteFromAsset() throws ServletException, IOException {
        context.create().asset("/content/dam/geometrixx/robots.txt", getClass().getResourceAsStream("RobotsServlet_testWriteFromAsset.txt"), "text/plain");

        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("robots.content.property.path", "/content/dam/geometrixx/robots.txt/jcr:content/renditions/original/jcr:content/jcr:data");
        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);
        robotsServlet.doGet(request, response);
        assertEquals("servlet returned an error", 200, response.getStatus());
        assertResponse("RobotsServlet_testWriteFromAsset.txt", response);
    }

    @Test
    public void testWriteFromPageProperties() throws ServletException, IOException {
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
        props.put("sitemap.property.names", new String[]{
           "isSiteMap"
        });

        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);
        robotsServlet.doGet(request, response);
        assertEquals("servlet returned an error", 200, response.getStatus());
        assertResponse("RobotsServlet_testWriteFromPageProperties.txt", response);
    }

    @Test
    public void testWriteFromOsgiConfigSimple() throws ServletException, IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("user.agent.directives", new String[]{
                "*"
        });
        props.put("disallow.directives", new String[]{
                "/"
        });

        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);
        robotsServlet.doGet(request, response);
        assertEquals("servlet returned an error", 200, response.getStatus());
        assertResponse("RobotsServlet_testWriteFromOsgiConfigSimple.txt", response);
    }

    @Test
    public void testWriteFromOsgiConfig() throws ServletException, IOException {
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
        robotsServlet.doGet(request, response);
        assertEquals("servlet returned an error", 200, response.getStatus());
        assertResponse("RobotsServlet_testWriteFromOsgiConfig.txt", response);
    }

    @Test
    public void testWriteFromNonExistentPropertyAbsolute() throws ServletException, IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("robots.content.property.path", "/content/dam/geometrixx/robots.txt/jcr:content/renditions/original/jcr:content/jcr:data");
        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);
        robotsServlet.doGet(request, response);
        assertEquals("servlet did not return the expected error", 404, response.getStatus());
    }

    @Test
    public void testWriteFromNonExistentPropertyRelative() throws ServletException, IOException {
        Map<String, Object> props = new HashMap<>();
        props.put("sling.servlet.resourceTypes", "geometrixx/components/structure/page");
        props.put("robots.content.property.path", "jcr:content/thisPropDoesntExist");
        RobotsServlet robotsServlet = context.registerInjectActivateService(new RobotsServlet(), props);
        robotsServlet.doGet(request, response);
        assertEquals("servlet did not return the expected error", 404, response.getStatus());
    }

    private void assertResponse(String expectedResponseOutputResourceName, MockSlingHttpServletResponse response) throws IOException {
        try (InputStream resourceAsStream = getClass().getResourceAsStream(expectedResponseOutputResourceName)) {
            // both response and input stream contains OS dependent line endings (no need to normalize)
            String expected = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
            assertEquals(expected, response.getOutputAsString());
        }
    }

}
