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
package com.adobe.acs.commons.wcm.impl;

import com.day.cq.commons.Externalizer;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

public class SiteMapServletTest {

    private static final Map<String, String> NS = Collections.singletonMap("ns", "http://www.sitemaps.org/schemas/sitemap/0.9");

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    private Externalizer externalizer;

    @InjectMocks
    private SiteMapServlet servlet = new SiteMapServlet();

    private MockSlingHttpServletRequest request;

    private MockSlingHttpServletResponse response;

    @Before
    public void setup() {
        context.load().json(getClass().getResourceAsStream("SiteMapServlet.json"), "/content/geometrixx");
        response = context.response();
        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext()) {
            @Override
            public String getResponseContentType() {
                return "text/xml";
            }
        };

        request.setResource(context.resourceResolver().getResource("/content/geometrixx/en"));

        when(externalizer.externalLink(eq(context.resourceResolver()), eq("external"), anyString())).then(i -> "http://test.com" + i.getArgument(2));
    }

    @Test
    public void testDefaultPageSetup() throws Exception {
        servlet.activate(new HashMap<String, Object>() {{
                put("externalizer.domain", "external");
            }
        });

        servlet.doGet(request, response);

        String output = response.getOutputAsString();

        assertThat(output, hasXPath("(//ns:loc)[1]/text()", equalTo("http://test.com/content/geometrixx/en.html")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[2]/text()", equalTo("http://test.com/content/geometrixx/en/events.html")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[3]/text()", equalTo("http://test.com/content/geometrixx/en/about.html")).withNamespaceContext(NS));
    }

    @Test
    public void testExtensionlessPages() throws Exception {
        servlet.activate(new HashMap<String, Object>() {{
                put("externalizer.domain", "external");
                put("extensionless.urls", true);
            }
        });

        servlet.doGet(request, response);

        String output = response.getOutputAsString();

        assertThat(output, hasXPath("(//ns:loc)[1]/text()", equalTo("http://test.com/content/geometrixx/en/")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[2]/text()", equalTo("http://test.com/content/geometrixx/en/events/")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[3]/text()", equalTo("http://test.com/content/geometrixx/en/about/")).withNamespaceContext(NS));
    }

    @Test
    public void testExtensionlessAndSlashlessPages() throws Exception {
        servlet.activate(new HashMap<String, Object>() {{
                put("externalizer.domain", "external");
                put("extensionless.urls", true);
                put("remove.slash", true);
            }
        });

        servlet.doGet(request, response);

        String output = response.getOutputAsString();

        assertThat(output, hasXPath("(//ns:loc)[1]/text()", equalTo("http://test.com/content/geometrixx/en")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[2]/text()", equalTo("http://test.com/content/geometrixx/en/events")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[3]/text()", equalTo("http://test.com/content/geometrixx/en/about")).withNamespaceContext(NS));
    }

}
