/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.redirects.filter;

import com.adobe.acs.commons.redirects.LocationHeaderAdjuster;
import com.adobe.acs.commons.redirects.RedirectResourceBuilder;
import com.adobe.acs.commons.redirects.models.RedirectConfiguration;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import com.adobe.acs.commons.redirects.models.Redirects;
import com.day.cq.wcm.api.WCMMode;
import com.google.common.cache.Cache;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.management.openmbean.TabularData;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import org.apache.http.Header;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.getRules;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RedirectFilterTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);
    private ConfigurationResourceResolver configResolver;

    private RedirectFilter filter;
    private FilterChain filterChain;
    private RedirectFilter.Configuration configuration;
    private String redirectStoragePath = RedirectResourceBuilder.DEFAULT_CONF_PATH;

    private String[] contentRoots = new String[]{
            "/content/we-retail", "/content/geometrixx", "/content/dam/we-retail"};

    @Before
    public void setUp() throws Exception {
        context.addModelsForClasses(RedirectRule.class);
        filter = spy(new RedirectFilter());

        configuration = mock(RedirectFilter.Configuration.class);
        when(configuration.enabled()).thenReturn(true);
        when(configuration.preserveQueryString()).thenReturn(true);
        when(configuration.paths()).thenReturn(contentRoots);
        when(configuration.additionalHeaders()).thenReturn(new String[]{"Expires: 12345", "Invalid"});
        when(configuration.bucketName()).thenReturn("settings");
        when(configuration.configName()).thenReturn("redirects");
        when(configuration.preserveExtension()).thenReturn(true);
        filter.activate(configuration, context.bundleContext());

        filterChain = mock(FilterChain.class);

        // mimic url shortening and removing extension by Sling Mappings
        doAnswer(invocation -> {
            String path = invocation.getArgument(0, String.class);
            // /content/we-retail/en/page.html --> https://www.we-retail.com/en/page.html
            if (path.startsWith("/content/we-retail/")) {
                return path
                        .replace("/content/we-retail/", "https://www.we-retail.com/");
            } else {
                return path;
            }
        }).when(filter).mapUrl(anyString(), any(SlingHttpServletRequest.class));

        configResolver = Mockito.mock(ConfigurationResourceResolver.class);
        Mockito.when(configResolver.getResource(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(context.resourceResolver().getResource(redirectStoragePath));
        filter.configResolver = configResolver;
    }


    private MockSlingHttpServletResponse navigate(String resourcePath, String selectorString, String extension) throws IOException, ServletException {
        StringBuilder pathBuilder = new StringBuilder(resourcePath);
        if(selectorString != null) {
            pathBuilder.append(".").append(selectorString);
        }
        if(extension != null) {
            pathBuilder.append(".").append(extension);
        }
        String requestPath = pathBuilder.toString();

        context.requestPathInfo().setResourcePath(requestPath);
        context.requestPathInfo().setSelectorString(selectorString);
        context.requestPathInfo().setExtension(extension);
        Resource resource = new NonExistingResource(context.resourceResolver(), requestPath);
        resource.getResourceMetadata().put("sling.resolutionPathInfo", "." + selectorString + "." + extension);
        MockSlingHttpServletRequest request = context.request();
        request.setResource(resource);
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        return response;
    }

    private MockSlingHttpServletResponse navigate(String resourcePath) throws IOException, ServletException {
        MockSlingHttpServletRequest request = context.request();
        int idx = resourcePath.lastIndexOf('.');
        if (idx > 0) {
            context.requestPathInfo().setExtension(resourcePath.substring(idx + 1));
        }
        int qs = resourcePath.lastIndexOf('?');
        if (qs > 0) {
            request.setQueryString(resourcePath.substring(qs + 1));
        }
        context.requestPathInfo().setResourcePath(resourcePath);
        if(context.resourceResolver().getResource(resourcePath) == null) {
            request.setResource(context.create().resource(resourcePath));
        }
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        return response;
    }

    private MockSlingHttpServletResponse navigateToURI(String requestPath) throws IOException, ServletException {
        MockSlingHttpServletRequest request = spy(context.request());
        doReturn(requestPath).when(request).getRequestURI();

        String resourcePath = requestPath;
        int idx = resourcePath.indexOf('.');
        if (idx > -1) {
            resourcePath = resourcePath.substring(0, idx);
        }

        int qs = requestPath.lastIndexOf('?');
        if (qs > 0) {
            request.setQueryString(requestPath.substring(qs + 1));
        }
        context.requestPathInfo().setResourcePath(resourcePath);
        request.setResource(getOrCreateResource(resourcePath));


        MockSlingHttpServletResponse response = context.response();
        filter.doFilter(request, response, filterChain);

        return response;
    }

    private Resource getOrCreateResource(String resourcePath) {
        Resource resource = context.resourceResolver().getResource(resourcePath);
        if (resource == null) {
            resource = context.create().resource(resourcePath);
        }
        return resource;
    }

    @Test
    public void testActivate() {
        assertTrue(filter.isEnabled());
        assertEquals(new HashSet<>(), new HashSet<>(filter.getExtensions()));
        assertEquals(new HashSet<>(Arrays.asList(contentRoots)), new HashSet<>(filter.getPaths()));
        assertEquals(Arrays.asList("GET", "HEAD"), new ArrayList<>(filter.getMethods()));
        List<Header> headers = filter.getOnDeliveryHeaders();
        assertEquals(1, headers.size());
        Header header = headers.iterator().next();
        assertEquals("Expires", header.getName());
        assertEquals("12345", header.getValue());
    }

    @Test
    public void testReadRules() throws PersistenceException {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/one")
                    .setTarget("/content/we-retail/en/two")
                    .setStatusCode(302).build(),
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/three")
                    .setTarget("/content/we-retail/en/four")
                    .setStatusCode(301).build(),
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/events/*")
                    .setTarget("/content/we-retail/en/four")
                    .setEvaluateURI(true)
                    .setStatusCode(301).build(),
            new RedirectResourceBuilder(context).build() // invalid rule wo source|target|statusCode should be ignored
        );

        ResourceBuilder rb = context.build().resource(redirectStoragePath).siblingsMode();
        rb.resource("redirect-invalid-1","sling:resourceType", "cq:Page");

        Resource resource = context.resourceResolver().getResource(redirectStoragePath);
        Collection<RedirectRule> rules = getRules(resource);
        assertEquals(3, rules.size());
        Iterator<RedirectRule> it = rules.iterator();
        RedirectRule rule1 = it.next();
        assertEquals("/content/we-retail/en/one", rule1.getSource());
        assertEquals("/content/we-retail/en/two", rule1.getTarget());
        assertEquals(302, rule1.getStatusCode());
        assertFalse(rule1.getEvaluateURI());

        RedirectRule rule2 = it.next();
        assertEquals("/content/we-retail/en/three", rule2.getSource());
        assertEquals("/content/we-retail/en/four", rule2.getTarget());
        assertEquals(301, rule2.getStatusCode());
        assertFalse(rule2.getEvaluateURI());

        RedirectRule rule3 = it.next();
        assertEquals("/content/we-retail/en/events/*", rule3.getSource());
        assertEquals("/content/we-retail/en/four", rule3.getTarget());
        assertEquals(301, rule3.getStatusCode());
        assertNotNull(rule3.getRegex());
        assertTrue(rule3.getEvaluateURI());
    }

    @Test
    public void testNavigate302() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("/content/geometrixx/en/two")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
        assertEquals("On-Delivery header", "12345", response.getHeader("Expires"));
    }

    @Test
    public void testNavigateToExternalSite() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("https://www.geometrixx.com")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("https://www.geometrixx.com", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testNavigateToExternalSiteWithQueryString() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("https://www.geometrixx.com")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html?a=1&b=2&c=3");

        assertEquals(302, response.getStatus());
        assertEquals("https://www.geometrixx.com?a=1&b=2&c=3", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testNavigate301() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/one")
                    .setTarget("/content/we-retail/en/two")
                    .setStatusCode(301).build());
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(301, response.getStatus());
        assertEquals("/content/we-retail/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testNavigateWithRewrite() throws Exception {
        when(filter.mapUrls()).thenReturn(true); // turn on resolver.map()
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/one")
                    .setTarget("/content/we-retail/en/two")
                    .setStatusCode(302).build());
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("https://www.we-retail.com/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }


    @Test
    public void testMatchWithRewrite() throws Exception {
        when(filter.mapUrls()).thenReturn(true); // turn on resolver.map()
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/en/one")
                    .setTarget("/en/two")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testNavigateNoRewrite() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/one")
                    .setTarget("/content/we-retail/en/two")
                    .setStatusCode(302).build()
        );

        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/we-retail/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testPreserveQueryString() {

        // Test combining query strings (combine = true)
        assertEquals("/path?a=1&b=2",
            filter.preserveQueryString("/path?a=1", "b=2", true));

        // Test replacing query strings (combine = false)
        assertEquals("/path?b=2",
            filter.preserveQueryString("/path?a=1", "b=2", false));

        // Test with fragment - combining
        assertEquals("/path?a=1&b=2#section",
            filter.preserveQueryString("/path?a=1#section", "b=2", true));

        // Test with fragment - replacing
        assertEquals("/path?b=2#section",
            filter.preserveQueryString("/path?a=1#section", "b=2", false));

        // Test empty request query - combining
        assertEquals("/path?a=1",
            filter.preserveQueryString("/path?a=1", "", true));

        // Test empty request query - replacing (should keep original)
        assertEquals("/path?a=1",
            filter.preserveQueryString("/path?a=1", "", false));

        // Test null request query - combining
        assertEquals("/path?a=1",
            filter.preserveQueryString("/path?a=1", null, true));

        // Test null request query - replacing (should keep original)
        assertEquals("/path?a=1",
            filter.preserveQueryString("/path?a=1", null, false));

        // Test complex combining
        assertEquals("/path?a=1&b=2&c=3&d=4",
            filter.preserveQueryString("/path?a=1&b=2", "c=3&d=4", true));

        // Test complex replacing
        assertEquals("/path?c=3&d=4",
            filter.preserveQueryString("/path?a=1&b=2", "c=3&d=4", false));

        // Test with fragment and multiple parameters - combining
        assertEquals("/path?a=1&b=2&c=3#section",
            filter.preserveQueryString("/path?a=1&b=2#section", "c=3", true));

        // Test with fragment and multiple parameters - replacing
        assertEquals("/path?c=3#section",
            filter.preserveQueryString("/path?a=1&b=2#section", "c=3", false));
    }

    @Test
    public void testPreserveExtension() throws Exception {
        when(configuration.preserveExtension()).thenReturn(true); // append .html extension to the Location header
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/we-retail/en/events/test")
                        .setTarget("/content/we-retail/en")
                        .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/events/test.html");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/content/we-retail/en.html", response.getHeader("Location"));
    }

    @Test
    public void testNotPreserveExtension() throws Exception {
        when(configuration.preserveExtension()).thenReturn(false); // no .html extension in the Location header
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/we-retail/en/events/test")
                        .setTarget("/content/we-retail/en")
                        .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/events/test.html");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/content/we-retail/en", response.getHeader("Location"));
    }

    @Test
    public void testMatchSingleAsset() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/dam/we-retail/en/events/test.pdf")
                    .setTarget("/content/dam/geometrixx/en/target/test.pdf")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/dam/we-retail/en/events/test.pdf");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/content/dam/geometrixx/en/target/test.pdf", response.getHeader("Location"));
    }

    @Test
    public void testMatchWithHtmlExtension() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/events/test.html")
                    .setTarget("/content/we-retail/en.html")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/events/test.html");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/content/we-retail/en.html", response.getHeader("Location"));
    }

    @Test
    public void testMatchRegexAsset() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/dam/we-retail/en/events/(.*?).pdf")
                    .setTarget("/content/dam/geometrixx/en/target/welcome.pdf")
                    .setStatusCode(302).build()
        );

        assertEquals("/content/dam/geometrixx/en/target/welcome.pdf",
                navigate("/content/dam/we-retail/en/events/one.pdf").getHeader("Location"));
    }

    @Test
    public void testNotMatchRegexAsset() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/dam/we-retail/en/events/(.*?).pdf")
                    .setTarget("/content/dam/geometrixx/en/target/welcome.pdf")
                    .setStatusCode(302).build()
        );

        assertEquals(null,
                navigate("/content/dam/we-retail/en/events/one.txt").getHeader("Location"));
    }

    @Test
    public void testLeadingSpaces() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource(" /content/we-retail/en/one")
                    .setTarget(" /content/we-retail/en/two")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/content/we-retail/en/two", response.getHeader("Location"));
    }

    @Test
    public void testTrailingSpaces() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource(" /content/we-retail/en/one ")
                    .setTarget(" /content/we-retail/en/two ")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/content/we-retail/en/two", response.getHeader("Location"));
    }

    @Test
    public void testUnsupportedExtension() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource(" /content/we-retail/en/one ")
                    .setTarget(" /content/we-retail/en/two ")
                    .setStatusCode(302).build()
        );
        when(filter.getExtensions()).thenReturn(Arrays.asList("html"));
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.json");

        assertEquals(null, response.getHeader("Location"));
        verify(filterChain, atLeastOnce())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testUnsupportedContentRoot() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource(" /content/we-retail/en/one ")
                    .setTarget(" /content/we-retail/en/two ")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/etc/tags/omg");

        assertEquals(null, response.getHeader("Location"));
        verify(filterChain, atLeastOnce())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    /**
     * #3105 Support handling redirects when the request path does not start with /content
     */
    @Test
    public void testContentRootMatchAll() throws Exception {

        doReturn(Collections.singletonList("/")).when(filter).getPaths();
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/test")
                        .setTarget("/content/we-retail/en/target")
                        .setStatusCode(302).build(),
                new RedirectResourceBuilder(context)
                        .setSource("/test/")
                        .setTarget("/content/we-retail/en/target")
                        .setStatusCode(302).build(),
                new RedirectResourceBuilder(context)
                        .setSource("/etc/tags/omg")
                        .setTarget("/content/we-retail/en/target")
                        .setStatusCode(302).build(),
                new RedirectResourceBuilder(context)
                        .setSource("/content/we-retail/en/source")
                        .setTarget("/content/we-retail/en/target")
                        .setStatusCode(302).build()

        );

        Resource resource = context.resourceResolver().getResource(redirectStoragePath);
        for(RedirectRule rule : getRules(resource)){

            MockSlingHttpServletResponse response = navigate(rule.getSource());
            assertEquals(rule.getTarget(), response.getHeader("Location"));

            context.response().reset();
        }
    }

    @Test
    public void testUnsupportedMethod() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource(" /content/we-retail/en/one ")
                    .setTarget(" /content/we-retail/en/two ")
                    .setStatusCode(302).build());
        context.request().setMethod("POST");
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(null, response.getHeader("Location"));
        verify(filterChain, atLeastOnce())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testAuthorEditWCMMode() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource(" /content/we-retail/en/one ")
                    .setTarget(" /content/we-retail/en/two ")
                    .setStatusCode(302).build()
        );
        context.request().setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(null, response.getHeader("Location"));
        verify(filterChain, atLeastOnce())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testUpdateCache() throws Exception {
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(null, response.getHeader("Location"));

        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("/content/geometrixx/en/two")
                    .setStatusCode(302).build()
        );
        filter.invalidate(redirectStoragePath);

        filter.doFilter(context.request(), response, filterChain);
        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html", response.getHeader("Location"));

        RedirectConfiguration rules = filter.getRulesCache().getIfPresent(redirectStoragePath);
        assertEquals(1, rules.getPathRules().size());
        assertEquals(0, rules.getPatternRules().size());
    }

    @Test
    public void testInvalidateOnChange() throws Exception {

        Cache<String, RedirectConfiguration> rulesCache = mock(Cache.class);
        filter.rulesCache = rulesCache;

        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/one")
                    .setTarget("/content/we-retail/en/two")
                    .setNodeName("redirect-1")
                    .setStatusCode(302).build()
        );

        filter.invalidate("/conf/global/settings/redirects/redirect-1");
        verify(rulesCache, times(1)).invalidate(eq("/conf/global/settings/redirects"));

        withRules(
            new RedirectResourceBuilder(context, "/conf/my-site/en/settings/redirects")
                    .setSource("/content/my-site/en/one")
                    .setTarget("/contentmy-site/en/two")
                    .setNodeName("redirect-1")
                    .setStatusCode(302).build()
        );
        filter.invalidate("/conf/my-site/en/settings/redirects/redirect-1");
        verify(rulesCache, times(1)).invalidate(eq("/conf/my-site/en/settings/redirects"));
    }

    @Test
    public void testNoopRewrite() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("(.*)")
                    .setTarget("$1")
                    .setStatusCode(302).build()
        );
        assertEquals("/content/geometrixx/about/contact-us", navigate("/content/geometrixx/about/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite1() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/(.+)/contact-us")
                    .setTarget("/content/geometrixx/$1/about-us")
                    .setStatusCode(302).build()
        );
        assertEquals("/content/geometrixx/about/about-us",
                navigate("/content/geometrixx/about/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite2() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/(en)/(.+)/contact-us")
                    .setTarget("/content/geometrixx/us/$2/contact-us")
                    .setStatusCode(302).build()
        );
        assertEquals("/content/geometrixx/us/1/contact-us",
                navigate("/content/geometrixx/en/1/contact-us").getHeader("Location"));
        assertEquals("/content/geometrixx/us/1/2/contact-us",
                navigate("/content/geometrixx/en/1/2/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite3() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/(en)/(.*?/?)contact-us")
                    .setTarget("/content/geometrixx/us/$2contact-us")
                    .setStatusCode(302).build()
        );
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/us/contact-us", navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
        assertEquals("/content/geometrixx/us/1/contact-us", navigate("/content/geometrixx/en/1/contact-us").getHeader("Location"));
        assertEquals("/content/geometrixx/us/1/2/contact-us", navigate("/content/geometrixx/en/1/2/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite4() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/(en)/(.+)/contact-us")
                    .setTarget("/content/geometrixx/us/$2/contact-us#section")
                    .setStatusCode(302).build()
        );
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/us/1/contact-us#section", navigate("/content/geometrixx/en/1/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite5() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/en/research/(.*)")
                .setTarget("/content/geometrixx/en/search?keywords=talent-management")
                .setStatusCode(302).build()
        );
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/en/search?keywords=talent-management", navigate("/content/geometrixx/en/research/doc").getHeader("Location"));
    }

    @Test
    public void testPathRewrite6() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                .setSource("/content/geometrixx/(.+)/contact-us#anchor")
                .setTarget("/content/geometrixx/$1/contact-us#updated")
                .setStatusCode(302).build()
        );
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/en/about/contact-us#updated", navigate("/content/geometrixx/en/about/contact-us#anchor").getHeader("Location"));
    }

    @Test
    public void testInvalidRules() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/(.+")
                    .setTarget("/content/we-retail/$a")
                    .setStatusCode(302).build(),
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail-events/(.+")
                    .setTarget("/content/we-retail/$")
                    .setStatusCode(302).build()
        );
        doReturn(false).when(filter).mapUrls();
        assertEquals(null, navigate("/content/we-retail/en/about/contact-us").getHeader("Location"));
        assertEquals(null, navigate("/content/we-retail-events/en/about/contact-us").getHeader("Location"));
    }

    @Test
    public void testUntilDateExpired() throws Exception {
        ZonedDateTime offTime = ZonedDateTime.now().minusDays(1);
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/we-retail/en/contact-us")
                    .setTarget("/content/we-retail/en/contact-them")
                    .setStatusCode(302)
                    .setUntilDate(GregorianCalendar.from(offTime))
                    .build()
        );
        assertEquals(null, navigate("/content/we-retail/en/contact-us").getHeader("Location"));
    }

    @Test
    public void testUntilDateInFuture() throws Exception {
        ZonedDateTime offTime = ZonedDateTime.now().plusDays(1);
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/contact-us")
                    .setTarget("/content/geometrixx/en/contact-them")
                    .setStatusCode(302)
                    .setUntilDate(GregorianCalendar.from(offTime)).build());
        assertEquals("/content/geometrixx/en/contact-them", navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
    }

    @Test
    public void testEffectiveDateInPast() throws Exception {
        ZonedDateTime onTime = ZonedDateTime.now().minusDays(1);
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/geometrixx/en/contact-us")
                        .setTarget("/content/geometrixx/en/contact-them")
                        .setStatusCode(302)
                        .setEffectiveFrom(GregorianCalendar.from(onTime)).build());
        assertEquals("/content/geometrixx/en/contact-them", navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
    }

    @Test
    public void testEffectiveDateInFuture() throws Exception {
        ZonedDateTime onTime = ZonedDateTime.now().plusDays(1);
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/geometrixx/en/contact-us")
                        .setTarget("/content/geometrixx/en/contact-them")
                        .setStatusCode(302)
                        .setEffectiveFrom(GregorianCalendar.from(onTime)).build());
        assertEquals(null, navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
    }

    @Test
    public void testEffectiveDateLessThanUntilDate() throws Exception {
        ZonedDateTime onTime = ZonedDateTime.now().minusDays(1);
        ZonedDateTime offTime = ZonedDateTime.now().plusDays(1);
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/geometrixx/en/contact-us")
                        .setTarget("/content/geometrixx/en/contact-them")
                        .setStatusCode(302)
                        .setUntilDate(GregorianCalendar.from(offTime))
                        .setEffectiveFrom(GregorianCalendar.from(onTime)).build());
        assertEquals("/content/geometrixx/en/contact-them", navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
    }

    @Test
    public void testEffectiveDateGreaterThanUntilDate() throws Exception {
        ZonedDateTime onTime = ZonedDateTime.now().plusDays(1);
        ZonedDateTime offTime = ZonedDateTime.now().minusDays(1);
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/geometrixx/en/contact-us")
                        .setTarget("/content/geometrixx/en/contact-them")
                        .setStatusCode(302)
                        .setUntilDate(GregorianCalendar.from(offTime))
                        .setEffectiveFrom(GregorianCalendar.from(onTime)).build());
        assertEquals(null, navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
    }

    private void withRules(Resource... rules) {
        withRules(redirectStoragePath, rules);

    }

    private void withRules(String configPath, Resource... rules) {
        Resource configResource = context.resourceResolver().getResource(configPath);
        doAnswer(invocation -> configResource).when(configResolver).getResource(any(Resource.class), any(String.class), any(String.class));

    }

    @Test
    public void testLocationAdjuster() throws Exception {
        LocationHeaderAdjuster urlAdjuster = new LocationHeaderAdjuster() {
            @Override
            public String adjust(SlingHttpServletRequest request, String location) {
                return location.replace(".html", ".adjusted.html");
            }
        };
        filter.urlAdjuster = urlAdjuster;
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("/content/geometrixx/en/two")
                    .setStatusCode(302).build()
        );
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.adjusted.html", response.getHeader("Location"));
    }

    @Test
    public void testJxmTabularData() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("/content/geometrixx/en/two")
                    .setStatusCode(302).build(),
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/contact-us")
                    .setTarget("/content/geometrixx/en/contact-them")
                    .setStatusCode(302).build()
        );

        TabularData data = filter.getRedirectRules(redirectStoragePath);
        assertEquals(0, data.size());

        assertEquals("/content/geometrixx/en/two", navigate("/content/geometrixx/en/one").getHeader("Location"));

        data = filter.getRedirectRules(redirectStoragePath);
        assertEquals(2, data.size());
    }

    @Test
    public void testNotEnabledDeactivate() throws Exception {
        RedirectFilter.Configuration configuration = mock(RedirectFilter.Configuration.class);
        when(configuration.enabled()).thenReturn(false);

        RedirectFilter redirectFilter = spy(new RedirectFilter());
        redirectFilter.activate(configuration, context.bundleContext());

        // #2673 : ensure no NPE in deactivate() when filter is disabled
        redirectFilter.deactivate();
    }

    @Test
    public void testNotEnabledOnChange() throws Exception {

        RedirectFilter.Configuration configuration = mock(RedirectFilter.Configuration.class);
        when(configuration.enabled()).thenReturn(false);
        when(configuration.bucketName()).thenReturn("settings");
        when(configuration.configName()).thenReturn("redirects");

        RedirectFilter redirectFilter = spy(new RedirectFilter());
        redirectFilter.activate(configuration, context.bundleContext());

        ResourceChange event = new ResourceChange(ResourceChange.ChangeType.CHANGED,
                "/conf/global/setting/redirects/rule", false, null, null, null);

        // #2673 : ensure no NPE in onChange() when filter is disabled
        redirectFilter.onChange(Collections.singletonList(event));
    }

    @Test
    public void testContextPrefix() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/en/one")
                    .setTarget("/en/two")
                    .setStatusCode(302).build()
        );

        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_CONTEXT_PREFIX, "/content/geometrixx");

        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testContextPrefixFullPathRedirectRule() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("/content/geometrixx/en/two")
                    .setStatusCode(302).build()
        );

        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_CONTEXT_PREFIX, "/content/geometrixx");

        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testContextPrefixMixedRedirectRules() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("/content/geometrixx/en/two")
                    .setStatusCode(302).build(),
            new RedirectResourceBuilder(context)
                    .setSource("/en/three")
                    .setTarget("/en/four")
                    .setStatusCode(302).build()
        );

        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_CONTEXT_PREFIX, "/content/geometrixx");

        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        response = navigate("/content/geometrixx/en/three.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/four.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testContextPrefixMixedRedirects() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/content/geometrixx/en/one")
                    .setTarget("/en/two")
                    .setStatusCode(302).build(),
            new RedirectResourceBuilder(context)
                    .setSource("/en/three")
                    .setTarget("/content/geometrixx/en/four")
                    .setStatusCode(302).build()
        );

        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_CONTEXT_PREFIX, "/content/geometrixx");

        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        response = navigate("/content/geometrixx/en/three.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/four.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testContextPrefixWithAbsoluteUrl() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/en/one")
                    .setTarget("https://adobe-consulting-services.github.io/acs-aem-commons/")
                    .setStatusCode(302).build()
        );

        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_CONTEXT_PREFIX, "/content/geometrixx");

        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("https://adobe-consulting-services.github.io/acs-aem-commons/", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testIgnoredContextPrefix() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/en/one")
                    .setTarget("/content/escapedsite/en/one")
                    .setStatusCode(302)
                    .setContextPrefixIgnored(true).build()
        );

        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_CONTEXT_PREFIX, "/content/geometrixx");

        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/escapedsite/en/one.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testContextPrefixWithPatternRule() throws Exception {
        withRules(
            new RedirectResourceBuilder(context)
                    .setSource("/en/one(.*)")
                    .setTarget("/en/two")
                    .setStatusCode(302).build(),
            new RedirectResourceBuilder(context)
                    .setSource("/en/three(.*)")
                    .setTarget("/content/escaped/en/four")
                    .setStatusCode(302)
                    .setContextPrefixIgnored(true).build(),
            new RedirectResourceBuilder(context)
                    .setSource("/(.*)")
                    .setTarget("/content/geometrixx/en/six")
                    .setStatusCode(302)
                    .build()
        );

        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_CONTEXT_PREFIX, "/content/geometrixx");

        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        response = navigate("/content/geometrixx/en/three.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/escaped/en/four.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        response = navigate("/content/geometrixx/en/five.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/six.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    /**
     * Default behaviour: selectors are taken into account, e.g.
     * for the rule
     * /content/we-retail/en/one => /content/we-retail/en/page1
     *
     *  /content/we-retail/en/one.html will match
     *  and
     *  /content/we-retail/en/one.a.b.c.html will not match because of the selectors
     *
     */
    @Test
    public void testNotMatchSelectors() throws Exception {
        RedirectFilter.Configuration configuration = filter.getConfiguration();
        filter.activate(configuration, context.bundleContext());

        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/we-retail/en/one")
                        .setTarget("/content/we-retail/en/page1")
                        .setStatusCode(302).build()
        );
        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_IGNORE_SELECTORS, false);

        assertEquals("/content/we-retail/en/page1.html",
                navigate("/content/we-retail/en/one", null, "html").getHeader("Location"));
        assertEquals(null,
                navigate("/content/we-retail/en/one", "a.b.c", "html").getHeader("Location"));
    }

    /**
     * Ignore Selectors in the caconf is checked and selectors are ignored
     * for the rule
     * /content/we-retail/en/one => /content/we-retail/en/page1
     *  both will match:
     *  /content/we-retail/en/one.html
     *  and
     *  /content/we-retail/en/one.a.b.c.html
     *
     */
    @Test
    public void testMatchIfSelectorsIgnored() throws Exception {
        RedirectFilter.Configuration configuration = filter.getConfiguration();
        filter.activate(configuration, context.bundleContext());

        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/we-retail/en/one")
                        .setTarget("/content/we-retail/en/page1")
                        .setStatusCode(302).build()
        );
        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(Redirects.CFG_PROP_IGNORE_SELECTORS, true);

        assertEquals("/content/we-retail/en/page1.html",
                navigate("/content/we-retail/en/one", "a.b.c", "html").getHeader("Location"));
        assertEquals("/content/we-retail/en/page1.html",
                navigate("/content/we-retail/en/one", "a.b.c", "html").getHeader("Location"));
    }

    @Test
    public void testEvaluateURI() throws Exception {
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/geometrixx/en/one.html/suffix.html")
                        .setTarget("/content/geometrixx/en/redirected-page")
                        .setStatusCode(302)
                        .setEvaluateURI(true)
                        .build(),
                new RedirectResourceBuilder(context)
                        .setSource("/content/geometrixx/en/two.mobile.html/suffix.html")
                        .setTarget("/content/geometrixx/en/redirected-page-selector")
                        .setStatusCode(302)
                        .setEvaluateURI(true)
                        .build(),
                new RedirectResourceBuilder(context)
                        .setSource("(.*)/geometrixx/en/three.html/suffix.html")
                        .setTarget("/content/geometrixx/en/redirected-page-regex")
                        .setStatusCode(302)
                        .setEvaluateURI(true)
                        .build(),
                new RedirectResourceBuilder(context)
                        .setSource("/content/geometrixx/en/(\\w+).(mobile|desktop).html/suffix-(\\d+).html")
                        .setTarget("/content/geometrixx/en/redirected-page-multi-regex")
                        .setStatusCode(302)
                        .setEvaluateURI(true)
                        .build()
        );

        MockSlingHttpServletResponse responseNoMatch = navigateToURI("/content/geometrixx/en/zero.html");
        assertNull(responseNoMatch.getHeader("Location")); //not redirected

        MockSlingHttpServletResponse responseURI = navigateToURI("/content/geometrixx/en/one.html/suffix.html");
        assertEquals("/content/geometrixx/en/redirected-page", responseURI.getHeader("Location"));

        MockSlingHttpServletResponse responseSelector = navigateToURI("/content/geometrixx/en/two.mobile.html/suffix.html");
        assertEquals("/content/geometrixx/en/redirected-page-selector", responseSelector.getHeader("Location"));

        MockSlingHttpServletResponse responseRegex = navigateToURI("/content/geometrixx/en/three.html/suffix.html");
        assertEquals("/content/geometrixx/en/redirected-page-regex", responseRegex.getHeader("Location"));

        MockSlingHttpServletResponse responseMultiRegex1 = navigateToURI("/content/geometrixx/en/four.mobile.html/suffix-1.html");
        assertEquals("/content/geometrixx/en/redirected-page-multi-regex", responseMultiRegex1.getHeader("Location"));

        MockSlingHttpServletResponse responseMultiRegex2 = navigateToURI("/content/geometrixx/en/four.desktop.html/suffix-2.html");
        assertEquals("/content/geometrixx/en/redirected-page-multi-regex", responseMultiRegex2.getHeader("Location"));

        MockSlingHttpServletResponse responseMultiRegex3 = navigateToURI("/content/geometrixx/en/five.mobile.html/suffix-3.html");
        assertEquals("/content/geometrixx/en/redirected-page-multi-regex", responseMultiRegex3.getHeader("Location"));
    }

    @Test
    public void testEvaluateURIWithQueryString() throws Exception {
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/geometrixx/en/page.html/suffix.html")
                        .setTarget("/content/geometrixx/en/redirected")
                        .setStatusCode(302)
                        .setEvaluateURI(true)
                        .build()
        );

        MockSlingHttpServletResponse responseNoQueryString = navigateToURI("/content/geometrixx/en/page.html/suffix.html");
        assertEquals("/content/geometrixx/en/redirected", responseNoQueryString.getHeader("Location"));

        MockSlingHttpServletResponse responseQueryString = navigateToURI("/content/geometrixx/en/page.html/suffix.html?a=1&b=2");
        assertEquals("/content/geometrixx/en/redirected", responseQueryString.getHeader("Location"));

        MockSlingHttpServletResponse responseAnchor = navigateToURI("/content/geometrixx/en/page.html/suffix.html#anchor");
        assertEquals("/content/geometrixx/en/redirected", responseAnchor.getHeader("Location"));

        MockSlingHttpServletResponse responseQueryStringAndAnchor = navigateToURI("/content/geometrixx/en/page.html/suffix.html?a=3&b=4#anchorAndQueryString");
        assertEquals("/content/geometrixx/en/redirected", responseQueryStringAndAnchor.getHeader("Location"));
    }

    /**
     * Cache-Control header is configured in the redirect properties
     */
    @Test
    public void testCacheControlHeaders() throws Exception {
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/we-retail/en/page")
                        .setTarget("/content/we-retail/en/target")
                        .setStatusCode(301)
                        .setCacheControlHeader("max-age=3600")
                        .build()
        );
        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(RedirectRule.CACHE_CONTROL_HEADER_NAME + "_301", "no-cache");

        MockSlingHttpServletResponse response = navigateToURI("/content/we-retail/en/page.html");
        assertEquals("/content/we-retail/en/target", response.getHeader("Location"));
        assertEquals("max-age=3600", response.getHeader("Cache-Control"));

    }

    /**
     * Cache-Control header is configured in the contextual configuration
     */
    @Test
    public void testCacheControlHeadersDefault() throws Exception {
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource("/content/we-retail/en/page")
                        .setTarget("/content/we-retail/en/target")
                        .setStatusCode(301)
                        .build()
        );
        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        configResource.adaptTo(ModifiableValueMap.class).put(RedirectRule.CACHE_CONTROL_HEADER_NAME + "_301", "no-cache");

        MockSlingHttpServletResponse response = navigateToURI("/content/we-retail/en/page.html");
        assertEquals("/content/we-retail/en/target", response.getHeader("Location"));
        assertEquals("no-cache", response.getHeader("Cache-Control"));
    }

    @Test
    public void testCaseInsensitiveRedirects() throws Exception {
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource(" /content/we-retail/en/one")
                        .setTarget(" /content/we-retail/en/two")
                        .setCaseInsensitive(true)
                        .setStatusCode(302).build()

        );
        assertEquals("/content/we-retail/en/two", navigate("/content/we-retail/en/one").getHeader("Location"));
        assertEquals("/content/we-retail/en/two", navigate("/content/we-retail/en/ONE").getHeader("Location"));
    }

    @Test
    public void testCaseInsensitiveRegexRedirects() throws Exception {
        withRules(
                new RedirectResourceBuilder(context)
                        .setSource(" /content/we-retail/en/.*")
                        .setTarget(" /content/we-retail/en/two")
                        .setCaseInsensitive(true)
                        .setStatusCode(302).build()

        );
        assertEquals("/content/we-retail/en/two", navigate("/content/we-retail/en/one").getHeader("Location"));
        assertEquals("/content/we-retail/en/two", navigate("/content/we-retail/EN/ONE").getHeader("Location"));
    }
}