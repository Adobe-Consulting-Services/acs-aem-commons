/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.redirects.filter;

import com.adobe.acs.commons.redirects.LocationHeaderAdjuster;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.wcm.api.WCMMode;
import org.apache.http.Header;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.powermock.reflect.Whitebox;

import javax.management.openmbean.TabularData;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.getRules;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.*;

public class RedirectFilterTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private RedirectFilter filter;
    private FilterChain filterChain;
    private String redirectStoragePath = "/conf/acs-commons/redirects";

    private String[] contentRoots = new String[]{
            "/content/we-retail", "/content/geometrixx", "/content/dam/we-retail"};

    @Before
    public void setUp() throws Exception {

        filter = spy(new RedirectFilter());
        ResourceResolverFactory resourceResolverFactory = mock(ResourceResolverFactory.class);
        when(resourceResolverFactory.getServiceResourceResolver(anyMapOf(String.class, Object.class)))
                .thenReturn(context.resourceResolver());
        Whitebox.setInternalState(filter, "resourceResolverFactory", resourceResolverFactory);

        RedirectFilter.Configuration configuration = mock(RedirectFilter.Configuration.class);
        when(configuration.mapUrls()).thenReturn(true);
        when(configuration.enabled()).thenReturn(true);
        when(configuration.preserveQueryString()).thenReturn(true);
        when(configuration.storagePath()).thenReturn(redirectStoragePath);
        when(configuration.paths()).thenReturn(contentRoots);
        when(configuration.additionalHeaders()).thenReturn(new String[]{"Cache-Control: no-cache", "Invalid"});
        filter.activate(configuration, context.bundleContext());

        filterChain = mock(FilterChain.class);

        // mimic url shortening and removing extension by Sling Mappings
        doAnswer(invocation -> {
            String path = invocation.getArgument(0, String.class);
            // /content/we-retail/en/page.html --> /en/page
            if (path.startsWith("/content/we-retail/")) {
                return path
                        .replace("/content/we-retail/", "/")
                        .replace(".html", "");
            } else {
                return path;
            }
        }).when(filter).mapUrl(anyString(), any(ResourceResolver.class));

    }

    private MockSlingHttpServletResponse navigate(String resourcePath) throws IOException, ServletException {
        MockSlingHttpServletRequest request = context.request();
        int idx = resourcePath.lastIndexOf('.');
        if (idx > 0) {
            context.requestPathInfo().setExtension(resourcePath.substring(idx + 1));
        }
        context.requestPathInfo().setResourcePath(resourcePath);
        request.setResource(context.create().resource(resourcePath));

        MockSlingHttpServletResponse response = context.response();
        filter.doFilter(request, response, filterChain);

        return response;
    }

    @Test
    public void testActivate() {
        assertTrue(filter.isEnabled());
        assertEquals(redirectStoragePath, filter.getStoragePath());
        assertEquals(new HashSet<>(), filter.getExtensions());
        assertEquals(new HashSet<>(Arrays.asList(contentRoots)), filter.getPaths());
        assertEquals(Arrays.asList("GET", "HEAD"), filter.getMethods());
        List<Header> headers = filter.getOnDeliveryHeaders();
        assertEquals(1, headers.size());
        Header header = headers.iterator().next();
        assertEquals("Cache-Control", header.getName());
        assertEquals("no-cache", header.getValue());
    }

    @Test
    public void testReadRules() {
        List<RedirectRule> savedRules = Arrays.asList(
                new RedirectRule("/content/we-retail/en/one", "/content/we-retail/en/two", 302, null),
                new RedirectRule("/content/we-retail/en/three", "/content/we-retail/en/four", 301, null),
                new RedirectRule("/content/we-retail/en/events/*", "/content/we-retail/en/four", 301, null)
        );
        ResourceBuilder rb = context.build().resource(redirectStoragePath).siblingsMode();
        int idx = 0;
        for (RedirectRule rule : savedRules) {
            rb.resource("redirect-" + (++idx),
                    "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                    RedirectRule.SOURCE_PROPERTY_NAME, rule.getSource(),
                    RedirectRule.TARGET_PROPERTY_NAME, rule.getTarget(),
                    RedirectRule.STATUS_CODE_PROPERTY_NAME, rule.getStatusCode());
        }
        rb.resource("redirect-invalid-1","sling:resourceType", "cq:Page");

        Resource resource = context.resourceResolver().getResource(redirectStoragePath);
        Collection<RedirectRule> rules = getRules(resource);
        assertEquals(3, rules.size());
        Iterator<RedirectRule> it = rules.iterator();
        RedirectRule rule1 = it.next();
        assertEquals("/content/we-retail/en/one", rule1.getSource());
        assertEquals("/content/we-retail/en/two", rule1.getTarget());
        assertEquals(302, rule1.getStatusCode());

        RedirectRule rule2 = it.next();
        assertEquals("/content/we-retail/en/three", rule2.getSource());
        assertEquals("/content/we-retail/en/four", rule2.getTarget());
        assertEquals(301, rule2.getStatusCode());

        RedirectRule rule3 = it.next();
        assertEquals("/content/we-retail/en/events/*", rule3.getSource());
        assertEquals("/content/we-retail/en/four", rule3.getTarget());
        assertEquals(301, rule3.getStatusCode());
        assertNotNull(rule3.getRegex());
    }

    @Test
    public void testNavigate302() throws Exception {
        withRules(
                new RedirectRule("/content/geometrixx/en/one", "/content/geometrixx/en/two",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
        assertEquals("On-Delivery header", "no-cache", response.getHeader("Cache-Control"));
    }

    @Test
    public void testNavigateToExternalSite() throws Exception {
        withRules(
                new RedirectRule("/content/geometrixx/en/one", "https://www.geometrixx.com",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("https://www.geometrixx.com", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testNavigate301() throws Exception {
        withRules(
                new RedirectRule("/content/we-retail/en/one", "/content/we-retail/en/two",
                        301, null));
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(301, response.getStatus());
        assertEquals("/en/two", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testNavigateWithRewrite() throws Exception {
        withRules(
                new RedirectRule("/content/we-retail/en/one", "/content/we-retail/en/two",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/en/two", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testNavigateNoRewrite() throws Exception {
        withRules(
                new RedirectRule("/content/we-retail/en/one", "/content/we-retail/en/two",
                        302, null));
        when(filter.mapUrls()).thenReturn(false); // turn off resolver.map() in osgi config

        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/we-retail/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testPreserveQueryString() throws Exception {
        withRules(
                new RedirectRule("/content/geometrixx/en/one", "/content/geometrixx/en/two",
                        302, null));

        context.request().setQueryString("a=1&b=2");
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.html?a=1&b=2", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testMatchSingleAsset() throws Exception {
        withRules(
                new RedirectRule("/content/dam/we-retail/en/events/test.pdf", "/content/dam/geometrixx/en/target/test.pdf",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/content/dam/we-retail/en/events/test.pdf");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/content/dam/geometrixx/en/target/test.pdf", response.getHeader("Location"));
    }

    @Test
    public void testMatchRegexAsset() throws Exception {
        withRules(
                new RedirectRule("/content/dam/we-retail/en/events/(.*?).pdf", "/content/dam/geometrixx/en/target/welcome.pdf",
                        302, null));

        assertEquals("/content/dam/geometrixx/en/target/welcome.pdf",
                navigate("/content/dam/we-retail/en/events/one.pdf").getHeader("Location"));
    }

    @Test
    public void testNotMatchRegexAsset() throws Exception {
        withRules(
                new RedirectRule("/content/dam/we-retail/en/events/(.*?).pdf", "/content/dam/geometrixx/en/target/welcome.pdf",
                        302, null));

        assertEquals(null,
                navigate("/content/dam/we-retail/en/events/one.txt").getHeader("Location"));
    }

    @Test
    public void testLeadingSpaces() throws Exception {
        withRules(
                new RedirectRule(" /content/we-retail/en/one", " /content/we-retail/en/two",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/en/two", response.getHeader("Location"));
    }

    @Test
    public void testTrailingSpaces() throws Exception {
        withRules(
                new RedirectRule(" /content/we-retail/en/one ", " /content/we-retail/en/two ",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one");

        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

        assertEquals("/en/two", response.getHeader("Location"));
    }

    @Test
    public void testMatchRewrittenPagePath() throws Exception {
        withRules(
                new RedirectRule("/en/one", "/en/two",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/en/two.html", response.getHeader("Location"));
        verify(filterChain, never())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));

    }

    @Test
    public void testUnsupportedExtension() throws Exception {
        withRules(
                new RedirectRule(" /content/we-retail/en/one ", " /content/we-retail/en/two ",
                        302, null));
        when(filter.getExtensions()).thenReturn(Arrays.asList("html"));
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.json");

        assertEquals(null, response.getHeader("Location"));
        verify(filterChain, atLeastOnce())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testUnsupportedContentRoot() throws Exception {
        withRules(
                new RedirectRule(" /content/we-retail/en/one ", " /content/we-retail/en/two ",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/etc/tags/omg");

        assertEquals(null, response.getHeader("Location"));
        verify(filterChain, atLeastOnce())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testUnsupportedMethod() throws Exception {
        withRules(
                new RedirectRule(" /content/we-retail/en/one ", " /content/we-retail/en/two ",
                        302, null));
        context.request().setMethod("POST");
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(null, response.getHeader("Location"));
        verify(filterChain, atLeastOnce())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testAuthorEditWCMMode() throws Exception {
        withRules(
                new RedirectRule(" /content/we-retail/en/one ", " /content/we-retail/en/two ",
                        302, null));
        context.request().setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/one.html");

        assertEquals(null, response.getHeader("Location"));
        verify(filterChain, atLeastOnce())
                .doFilter(any(SlingHttpServletRequest.class), any(SlingHttpServletResponse.class));
    }

    @Test
    public void testUpdateCache() throws Exception {
        MockSlingHttpServletResponse response = navigate("/content/we-retail/en/research/page1.html");

        assertEquals(null, response.getHeader("Location"));
        assertEquals(0, filter.getPathRules().size());
        assertEquals(0, filter.getPatternRules().size());
        context.build()
                .resource(redirectStoragePath)
                .siblingsMode()
                .resource("redirect-1",
                        "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                        "source", "/content/we-retail/en/one",
                        "target", "/content/we-retail/en/moved/page", "statusCode", 301)
                .resource("redirect-2",
                        "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                        "source", "/content/we-retail/en/research/*",
                        "target", "/content/we-retail/en/moved/page", "statusCode", 302);
        filter.refreshCache();
        assertEquals(1, filter.getPathRules().size());
        assertEquals(1, filter.getPatternRules().size());

        filter.doFilter(context.request(), response, filterChain);
        assertEquals(302, response.getStatus());
        assertEquals("/en/moved/page", response.getHeader("Location"));
    }

    @Test
    public void testInvalidateOnChange() throws Exception {
        verify(filter, times(1)).refreshCache(); // refreshCache is called in @Activate

        filter.onChange(Arrays.asList(new ResourceChange(ResourceChange.ChangeType.ADDED, redirectStoragePath + "/redirect-1",
                false, null, null, null)));
        verify(filter, times(1)).refreshCache();

    }

    @Test
    public void testInvalidateOnReplication() throws Exception {
        verify(filter, times(1)).refreshCache(); // refreshCache is called in @Activate

        Event event = new Event(ReplicationAction.EVENT_TOPIC,
                Collections.singletonMap(SlingConstants.PROPERTY_PATH, redirectStoragePath + "/redirect-1"));

        filter.handleEvent(event);
        verify(filter, times(1)).refreshCache();

    }


    @Test
    public void testNoopRewrite() throws Exception {
        withRules(new RedirectRule("(.*)", "$1", 302, null));
        assertEquals("/content/geometrixx/about/contact-us", navigate("/content/geometrixx/about/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite1() throws Exception {
        withRules(new RedirectRule("/content/geometrixx/(.+)/contact-us", "/content/geometrixx/$1/about-us", 302, null));
        assertEquals("/content/geometrixx/about/about-us",
                navigate("/content/geometrixx/about/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite2() throws Exception {
        withRules(new RedirectRule("/content/geometrixx/(en)/(.+)/contact-us", "/content/geometrixx/us/$2/contact-us", 302, null));
        assertEquals("/content/geometrixx/us/1/contact-us",
                navigate("/content/geometrixx/en/1/contact-us").getHeader("Location"));
        assertEquals("/content/geometrixx/us/1/2/contact-us",
                navigate("/content/geometrixx/en/1/2/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite3() throws Exception {
        withRules(new RedirectRule("/content/geometrixx/(en)/(.*?/?)contact-us", "/content/geometrixx/us/$2contact-us", 302, null));
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/us/contact-us", navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
        assertEquals("/content/geometrixx/us/1/contact-us", navigate("/content/geometrixx/en/1/contact-us").getHeader("Location"));
        assertEquals("/content/geometrixx/us/1/2/contact-us", navigate("/content/geometrixx/en/1/2/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite4() throws Exception {
        withRules(new RedirectRule("/content/geometrixx/(en)/(.+)/contact-us", "/content/geometrixx/us/$2/contact-us#section", 302, null));
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/us/1/contact-us#section", navigate("/content/geometrixx/en/1/contact-us").getHeader("Location"));
    }

    @Test
    public void testPathRewrite5() throws Exception {
        withRules(new RedirectRule("/content/geometrixx/en/research/(.*)", "/content/geometrixx/en/search?keywords=talent-management", 302, null));
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/en/search?keywords=talent-management", navigate("/content/geometrixx/en/research/doc").getHeader("Location"));
    }

    @Test
    public void testPathRewrite6() throws Exception {
        withRules(new RedirectRule("/content/geometrixx/(.+)/contact-us#anchor", "/content/geometrixx/$1/contact-us#updated", 302, null));
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/en/about/contact-us#updated", navigate("/content/geometrixx/en/about/contact-us#anchor").getHeader("Location"));
    }

    @Test
    public void testInvalidRules() throws Exception {
        withRules(
                new RedirectRule("/content/we-retail/(.+", "/content/we-retail/$a", 302, null),
                new RedirectRule("/content/we-retail-events/(.+", "/content/we-retail/$", 302, null));
        doReturn(false).when(filter).mapUrls();
        assertEquals(null, navigate("/content/we-retail/en/about/contact-us").getHeader("Location"));
        assertEquals(null, navigate("/content/we-retail-events/en/about/contact-us").getHeader("Location"));
    }

    @Test
    public void testExpiredRedirect() throws Exception {
        LocalDate dateInPast = LocalDate.now().minusDays(1);
        withRules(
                new RedirectRule("/content/we-retail/en/contact-us", "/content/we-retail/en/contact-them",
                        302, RedirectRule.DATE_FORMATTER.format(dateInPast)));
        doReturn(false).when(filter).mapUrls();
        assertEquals(null, navigate("/content/we-retail/en/contact-us").getHeader("Location"));
    }

    @Test
    public void testNotExpiredRedirect() throws Exception {
        LocalDate dateInFuture = LocalDate.now().plusDays(1);
        withRules(
                new RedirectRule("/content/geometrixx/en/contact-us", "/content/geometrixx/en/contact-them",
                        302, RedirectRule.DATE_FORMATTER.format(dateInFuture)));
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/en/contact-them", navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
    }

    @Test
    public void testExpiringToday() throws Exception {
        LocalDate today = LocalDate.now();
        withRules(
                new RedirectRule("/content/geometrixx/en/contact-us", "/content/geometrixx/en/contact-them",
                        302, RedirectRule.DATE_FORMATTER.format(today)));
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/en/contact-them", navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
    }

    @Test
    public void testInvalidUntilDate() throws Exception {
        withRules(
                new RedirectRule("/content/geometrixx/en/contact-us", "/content/geometrixx/en/contact-them",
                        302, "2018-02-Invalid"));
        doReturn(false).when(filter).mapUrls();
        assertEquals("/content/geometrixx/en/contact-them", navigate("/content/geometrixx/en/contact-us").getHeader("Location"));
    }

    private void withRules(RedirectRule... rules) {
        Map<Pattern, RedirectRule> patternRules = Arrays.stream(rules).filter(r -> r.getRegex() != null).collect(Collectors.toMap(r -> r.getRegex(), r -> r));
        Map<String, RedirectRule> pathRules = Arrays.stream(rules).filter(r -> r.getRegex() == null).collect(Collectors.toMap(r -> r.getSource(), r -> r));

        doReturn(patternRules).when(filter).getPatternRules();
        doReturn(pathRules).when(filter).getPathRules();

    }

    @Test
    public void testLocationAdjuster() throws Exception {
        LocationHeaderAdjuster urlAdjuster = new LocationHeaderAdjuster() {
            @Override
            public String adjust(SlingHttpServletRequest request, String location) {
                return location.replace(".html", ".adjusted.html");
            }
        };
        Whitebox.setInternalState(filter, "urlAdjuster", urlAdjuster);
        withRules(
                new RedirectRule("/content/geometrixx/en/one", "/content/geometrixx/en/two",
                        302, null));
        MockSlingHttpServletResponse response = navigate("/content/geometrixx/en/one.html");

        assertEquals(302, response.getStatus());
        assertEquals("/content/geometrixx/en/two.adjusted.html", response.getHeader("Location"));
    }

    @Test
    public void testJxmTabularData() throws Exception {
        withRules(
                new RedirectRule("/content/geometrixx/en/one", "/content/geometrixx/en/two",
                        302, null),
                new RedirectRule("/content/geometrixx/en/contact-us", "/content/geometrixx/en/contact-them",
                        302, null));

        TabularData data = filter.getRedirectConfigurations();
        assertEquals(2, data.size());
    }

}