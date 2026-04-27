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
package com.adobe.acs.commons.redirects.models;

import com.adobe.acs.commons.redirects.RedirectResourceBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.adobe.acs.commons.redirects.models.RedirectConfiguration.determinePathToEvaluate;
import static com.adobe.acs.commons.redirects.models.RedirectConfiguration.normalizePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedirectConfigurationTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private final String redirectStoragePath = RedirectResourceBuilder.DEFAULT_CONF_PATH;

    @Before
    public void setUp() {
        context.addModelsForClasses(RedirectRule.class);
    }

    @Test
    public void testNormalizePath(){
        assertEquals("/content/we-retail/en", normalizePath("/content/we-retail/en"));
        assertEquals("/content/we-retail/en", normalizePath("/content/we-retail/en.html"));
        assertEquals("/content/dam/we-retail/en.html", normalizePath("/content/dam/we-retail/en.html"));
        assertEquals("/content/dam/we-retail/en.pdf", normalizePath("/content/dam/we-retail/en.pdf"));
    }

    @Test
    public void testPathToEvaluate(){
        final String resourcePath = "/content/we-retail/en";
        final String expectedURI = "/content/we-retail/en.html/suffix.html";
        SlingHttpServletRequest mockRequest = mock(SlingHttpServletRequest.class);
        doReturn(expectedURI).when(mockRequest).getRequestURI();

        assertEquals(resourcePath, determinePathToEvaluate(resourcePath, false, null));
        assertEquals(resourcePath, determinePathToEvaluate(resourcePath, false, mockRequest));
        assertEquals(expectedURI, determinePathToEvaluate(resourcePath, true, mockRequest));
    }

    /**
     * #3696 An .html path rule must not hijack requests for other extensions (e.g. .xml sitemaps).
     */
    @Test
    public void testHtmlRuleDoesNotMatchXmlRequest_3696() throws Exception {
        RedirectConfiguration config = buildConfig(
                new RedirectResourceBuilder(context)
                        .setSource("/content/mysite/na.html")
                        .setTarget("/content/mysite/na/en.html")
                        .setStatusCode(301).build());

        RedirectMatch match = config.match("/content/mysite/na", "", mockRequestWithExtension("xml"));
        assertNull("an .html rule must not match an .xml request", match);
    }

    @Test
    public void testHtmlRuleMatchesHtmlRequest() throws Exception {
        RedirectConfiguration config = buildConfig(
                new RedirectResourceBuilder(context)
                        .setSource("/content/mysite/na.html")
                        .setTarget("/content/mysite/na/en.html")
                        .setStatusCode(301).build());

        RedirectMatch match = config.match("/content/mysite/na", "", mockRequestWithExtension("html"));
        assertNotNull(match);
        assertEquals("/content/mysite/na/en.html", match.getRule().getTarget());
    }

    @Test
    public void testHtmlRuleMatchesRequestWithoutExtension() throws Exception {
        RedirectConfiguration config = buildConfig(
                new RedirectResourceBuilder(context)
                        .setSource("/content/mysite/na.html")
                        .setTarget("/content/mysite/na/en.html")
                        .setStatusCode(301).build());

        RedirectMatch match = config.match("/content/mysite/na", "", mockRequestWithExtension(null));
        assertNotNull("backward compat: a null request extension still matches an .html rule", match);
    }

    @Test
    public void testExtensionlessRuleMatchesAnyExtension() throws Exception {
        RedirectConfiguration config = buildConfig(
                new RedirectResourceBuilder(context)
                        .setSource("/content/mysite/na")
                        .setTarget("/content/mysite/na/en")
                        .setStatusCode(301).build());

        assertNotNull(config.match("/content/mysite/na", "", mockRequestWithExtension("html")));
        assertNotNull(config.match("/content/mysite/na", "", mockRequestWithExtension("xml")));
        assertNotNull(config.match("/content/mysite/na", "", mockRequestWithExtension(null)));
    }

    @Test
    public void testDamRuleRejectsDifferentExtension() throws Exception {
        RedirectConfiguration config = buildConfig(
                new RedirectResourceBuilder(context)
                        .setSource("/content/dam/foo.pdf")
                        .setTarget("/content/dam/bar.pdf")
                        .setStatusCode(301).build());

        // normalizePath() does not strip DAM extensions, so the lookup key is /content/dam/foo.pdf
        RedirectMatch match = config.match("/content/dam/foo.pdf", "", mockRequestWithExtension("jpg"));
        assertNull("a .pdf rule must not match a .jpg request", match);
    }

    /**
     * isExtensionCompatible() uses equalsIgnoreCase, so a rule with source "/foo.html"
     * must accept a request whose extension is uppercase "HTML".
     */
    @Test
    public void testRequestExtensionMatchedCaseInsensitively() throws Exception {
        RedirectConfiguration config = buildConfig(
                new RedirectResourceBuilder(context)
                        .setSource("/content/mysite/na.html")
                        .setTarget("/content/mysite/na/en.html")
                        .setStatusCode(301).build());

        // uppercase request extension — isExtensionCompatible uses equalsIgnoreCase
        RedirectMatch match = config.match("/content/mysite/na", "", mockRequestWithExtension("HTML"));
        assertNotNull("A .html rule should match a request with .HTML extension (case-insensitive)", match);

        // different extension must still be rejected
        RedirectMatch noMatch = config.match("/content/mysite/na", "", mockRequestWithExtension("xml"));
        assertNull("A .html rule must not match a .xml request", noMatch);
    }

    /**
     * When two rules share the same normalised path key (e.g. /foo.html and /foo both map to /foo),
     * the last rule written to pathRules wins because it is a LinkedHashMap.
     * This test documents that behaviour so any future change to the collision strategy is visible.
     */
    @Test
    public void testRuleWithSameNormalizedKeyOverwritesPreviousRule() throws Exception {
        // extensionless rule first — stored under key /content/mysite/na, sourceExtension=null
        new RedirectResourceBuilder(context)
                .setSource("/content/mysite/na")
                .setTarget("/catch-all")
                .setStatusCode(302).build();
        // extension-specific rule second — same key /content/mysite/na, overwrites the first
        new RedirectResourceBuilder(context)
                .setSource("/content/mysite/na.html")
                .setTarget("/html-only")
                .setStatusCode(301).build();

        Resource cfgResource = context.resourceResolver().getResource(redirectStoragePath);
        RedirectConfiguration config = new RedirectConfiguration(cfgResource, "settings/redirects");

        // The .html rule (added last) now occupies the slot; .xml is rejected by extension check
        RedirectMatch xmlMatch = config.match("/content/mysite/na", "", mockRequestWithExtension("xml"));
        assertNull("The extensionless catch-all rule was overwritten; xml request should not match", xmlMatch);

        // The .html rule itself still matches html
        RedirectMatch htmlMatch = config.match("/content/mysite/na", "", mockRequestWithExtension("html"));
        assertNotNull(htmlMatch);
        assertEquals("/html-only", htmlMatch.getRule().getTarget());
    }

    /**
     * Regex rules (source contains a capture group) store sourceExtension=null and bypass the
     * extension compatibility check entirely. This is intentional: the regex pattern itself
     * controls which paths match.
     */
    @Test
    public void testRegexRuleIsNotFilteredByExtension() throws Exception {
        RedirectConfiguration config = buildConfig(
                new RedirectResourceBuilder(context)
                        .setSource("/content/(.*)")
                        .setTarget("/other/$1")
                        .setStatusCode(302).build());

        assertNotNull("Regex rule should match .xml request", config.match("/content/foo", "", mockRequestWithExtension("xml")));
        assertNotNull("Regex rule should match .html request", config.match("/content/foo", "", mockRequestWithExtension("html")));
        assertNotNull("Regex rule should match no-extension request", config.match("/content/foo", "", mockRequestWithExtension(null)));
    }

    /**
     * Unit tests for the package-private extractSourceExtension helper covering edge cases
     * not exercised by higher-level tests.
     */
    @Test
    public void testExtractSourceExtension_edgeCases() {
        assertNull("null source", RedirectRule.extractSourceExtension(null));
        assertNull("empty source", RedirectRule.extractSourceExtension(""));
        assertNull("no dot", RedirectRule.extractSourceExtension("/content/foo"));
        assertNull("trailing dot only", RedirectRule.extractSourceExtension("/content/foo."));
        assertNull("regex metachar in candidate", RedirectRule.extractSourceExtension("/content/foo.(html)"));
        assertEquals("html", RedirectRule.extractSourceExtension("/content/foo.html"));
        // extractSourceExtension is case-preserving; isExtensionCompatible does equalsIgnoreCase
        assertEquals("HTML", RedirectRule.extractSourceExtension("/content/foo.HTML"));
        assertEquals("123", RedirectRule.extractSourceExtension("/content/foo.123"));
        // only the final extension segment is extracted
        assertEquals("gz", RedirectRule.extractSourceExtension("/content/foo.tar.gz"));
    }

    private RedirectConfiguration buildConfig(Resource... rules) {
        // ensure the builder has created the storage resource before constructing the configuration
        Resource configResource = context.resourceResolver().getResource(redirectStoragePath);
        assertNotNull(configResource);
        return new RedirectConfiguration(configResource, "settings/redirects");
    }

    private static SlingHttpServletRequest mockRequestWithExtension(String extension) {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        RequestPathInfo info = mock(RequestPathInfo.class);
        when(request.getRequestPathInfo()).thenReturn(info);
        when(info.getExtension()).thenReturn(extension);
        return request;
    }
}
