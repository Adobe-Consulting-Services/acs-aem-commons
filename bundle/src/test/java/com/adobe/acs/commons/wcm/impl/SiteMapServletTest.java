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
import com.day.cq.wcm.api.NameConstants;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.wcm.impl.SiteMapServlet.Config.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

@RunWith(MockitoJUnitRunner.class)
public class SiteMapServletTest {

    private static final Map<String, String> NS = Collections.singletonMap("ns", "http://www.sitemaps.org/schemas/sitemap/0.9");

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
        context.registerService(Externalizer.class, externalizer);
        response = context.response();
        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext()) {
            @Override
            public String getResponseContentType() {
                return "text/xml";
            }
        };
        request.setResource(context.resourceResolver().getResource("/content/geometrixx/en"));

        when(externalizer.externalLink(eq(context.resourceResolver()), eq("external"), anyString())).then(i -> "http://test.com" + i.getArgumentAt(2, String.class));
    }

    private void activateWithDefaultValues(Map<String,Object> specifiedProps){
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROP_EXTERNALIZER_DOMAIN, "publish" );
        properties.put(PROP_INCLUDE_LAST_MODIFIED,false);
        properties.put(PROP_EXCLUDE_FROM_SITEMAP_PROPERTY,NameConstants.PN_HIDE_IN_NAV);
        properties.put(PROP_INCLUDE_INHERITANCE_VALUE,false);
        properties.put(PROP_EXTENSIONLESS_URLS,false);
        properties.put(PROP_REMOVE_TRAILING_SLASH,false);

        properties.putAll(specifiedProps);
        context.registerInjectActivateService(servlet, properties);
    }

    @Test
    public void testDefaultPageSetup() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROP_EXTERNALIZER_DOMAIN, "external" );
        activateWithDefaultValues(properties);
        servlet.doGet(request, response);

        String output = response.getOutputAsString();

        assertThat(output, hasXPath("(//ns:loc)[1]/text()", equalTo("http://test.com/content/geometrixx/en.html")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[2]/text()", equalTo("http://test.com/content/geometrixx/en/events.html")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[3]/text()", equalTo("http://test.com/content/geometrixx/en/about.html")).withNamespaceContext(NS));
    }

    @Test
    public void testExtensionlessPages() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROP_EXTERNALIZER_DOMAIN, "external" );
        properties.put(PROP_EXTENSIONLESS_URLS, true);
        activateWithDefaultValues(properties);
        servlet.doGet(request, response);

        String output = response.getOutputAsString();

        assertThat(output, hasXPath("(//ns:loc)[1]/text()", equalTo("http://test.com/content/geometrixx/en/")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[2]/text()", equalTo("http://test.com/content/geometrixx/en/events/")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[3]/text()", equalTo("http://test.com/content/geometrixx/en/about/")).withNamespaceContext(NS));
    }

    @Test
    public void testExtensionlessAndSlashlessPages() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROP_EXTERNALIZER_DOMAIN, "external" );
        properties.put(PROP_EXTENSIONLESS_URLS, true);
        properties.put(PROP_REMOVE_TRAILING_SLASH, true);
        activateWithDefaultValues(properties);
        servlet.doGet(request, response);

        String output = response.getOutputAsString();

        assertThat(output, hasXPath("(//ns:loc)[1]/text()", equalTo("http://test.com/content/geometrixx/en")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[2]/text()", equalTo("http://test.com/content/geometrixx/en/events")).withNamespaceContext(NS));
        assertThat(output, hasXPath("(//ns:loc)[3]/text()", equalTo("http://test.com/content/geometrixx/en/about")).withNamespaceContext(NS));
    }

}
