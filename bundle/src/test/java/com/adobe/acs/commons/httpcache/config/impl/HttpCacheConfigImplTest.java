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
package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants.ANONYMOUS_REQUEST;
import static com.adobe.acs.commons.httpcache.config.AuthenticationStatusConfigConstants.AUTHENTICATED_REQUEST;
import static com.adobe.acs.commons.httpcache.config.impl.HttpCacheConfigImpl.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpCacheConfigImplTest {

    @Rule
    public AemContext context = new AemContext();
    private final HttpCacheConfigImpl systemUnderTest = new HttpCacheConfigImpl();
    private Map<String, Object> properties;

    @Mock
    private GroupHttpCacheConfigExtension extension;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ResourceResolver resourceResolver;

    @Before
    public void init() throws HttpCacheRepositoryAccessException {

        properties = new HashMap<>();

        context.registerService(CacheKeyFactory.class, extension);

        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getUserID()).thenReturn("anonymous");
        when(extension.accepts(request, systemUnderTest)).thenReturn(true);
    }

    private void activateWithDefaultValues(Map<String,Object> specifiedProps){
        properties.put(PROP_ORDER, DEFAULT_ORDER );
        properties.put(PROP_REQUEST_URI_PATTERNS, new String[]{});
        properties.put(PROP_BLACKLISTED_REQUEST_URI_PATTERNS, new String[]{});
        properties.put(PROP_AUTHENTICATION_REQUIREMENT,DEFAULT_AUTHENTICATION_REQUIREMENT);
        properties.put(PROP_CACHE_INVALIDATION_PATH_PATTERNS,new String[]{});
        properties.put(PROP_CACHE_STORE,DEFAULT_CACHE_STORE);
        properties.put(PROP_FILTER_SCOPE,DEFAULT_FILTER_SCOPE);
        properties.put(PROP_EXPIRY_ON_CREATE,DEFAULT_EXPIRY_ON_CREATE);
        properties.put(PROP_EXPIRY_ON_ACCESS,DEFAULT_EXPIRY_ON_ACCESS);
        properties.put(PROP_EXPIRY_ON_UPDATE,DEFAULT_EXPIRY_ON_UPDATE);
        properties.put(PROP_CACHE_HANDLING_RULES_PID,new String[]{});
        properties.put(PROP_RESPONSE_HEADER_EXCLUSIONS, new String[]{});

        properties.putAll(specifiedProps);
        context.registerInjectActivateService(systemUnderTest, properties);
    }

    @Test
    public void test_default_config_values(){
        activateWithDefaultValues(Collections.emptyMap());

        assertEquals(DEFAULT_ORDER, systemUnderTest.getOrder());
        assertTrue( systemUnderTest.getRequestUriPatterns().isEmpty());
        assertTrue(systemUnderTest.getBlacklistedRequestUriPatterns().isEmpty());
        assertEquals(DEFAULT_AUTHENTICATION_REQUIREMENT, systemUnderTest.getAuthenticationRequirement());
        assertTrue(systemUnderTest.getJCRInvalidationPathPatterns().isEmpty());
        assertEquals(DEFAULT_CACHE_STORE, systemUnderTest.getCacheStoreName());
        assertEquals(HttpCacheConfig.FilterScope.REQUEST, systemUnderTest.getFilterScope());
        assertEquals(DEFAULT_EXPIRY_ON_CREATE, systemUnderTest.getExpiryOnCreate());
        assertEquals(DEFAULT_EXPIRY_ON_ACCESS, systemUnderTest.getExpiryForAccess());
        assertEquals(DEFAULT_EXPIRY_ON_UPDATE, systemUnderTest.getExpiryForUpdate());
        assertFalse( systemUnderTest.acceptsRule("nonexisting"));


    }

    @Test
    public void test_specified_values(){

        properties.put(PROP_ORDER, 22 );
        properties.put(PROP_REQUEST_URI_PATTERNS, new String[]{"/content"});
        properties.put(PROP_BLACKLISTED_REQUEST_URI_PATTERNS, new String[]{"/content/blacklisted"});
        properties.put(PROP_AUTHENTICATION_REQUIREMENT,AUTHENTICATED_REQUEST);
        properties.put(PROP_CACHE_INVALIDATION_PATH_PATTERNS,new String[]{"/content"});
        properties.put(PROP_CACHE_STORE,"JCR");
        properties.put(PROP_FILTER_SCOPE,FILTER_SCOPE_INCLUDE);
        properties.put(PROP_EXPIRY_ON_CREATE,5L);
        properties.put(PROP_EXPIRY_ON_ACCESS,10L);
        properties.put(PROP_EXPIRY_ON_UPDATE,15L);
        properties.put(PROP_CACHE_HANDLING_RULES_PID,new String[]{"handling-rule"});
        properties.put(PROP_RESPONSE_HEADER_EXCLUSIONS, new String[]{"my-login-header"});

        context.registerInjectActivateService(systemUnderTest, properties);

        assertEquals(22, systemUnderTest.getOrder());
        assertEquals("/content", systemUnderTest.getRequestUriPatterns().get(0).pattern());
        assertEquals("/content/blacklisted",systemUnderTest.getBlacklistedRequestUriPatterns().get(0).pattern());
        assertEquals(AUTHENTICATED_REQUEST, systemUnderTest.getAuthenticationRequirement());
        assertEquals("/content", systemUnderTest.getJCRInvalidationPathPatterns().get(0).pattern());
        assertEquals("JCR", systemUnderTest.getCacheStoreName());
        assertEquals(HttpCacheConfig.FilterScope.INCLUDE, systemUnderTest.getFilterScope());
        assertEquals(5L, systemUnderTest.getExpiryOnCreate());
        assertEquals(10L, systemUnderTest.getExpiryForAccess());
        assertEquals(15L, systemUnderTest.getExpiryForUpdate());
        assertEquals("my-login-header", systemUnderTest.getExcludedResponseHeaderPatterns().get(0).pattern());
        assertFalse( systemUnderTest.acceptsRule("nonexisting"));
        assertTrue( systemUnderTest.acceptsRule("handling-rule"));
    }

    @Test
    public void test_accepts_sling_request() throws HttpCacheRepositoryAccessException {

        Map<String,Object> properties = new HashMap<>();

        properties.put(PROP_REQUEST_URI_PATTERNS, new String[]{"/content/[a-z]{4}.html"});
        activateWithDefaultValues(properties);

        when(request.getRequestURI()).thenReturn("/content/page.html");

        assertTrue(systemUnderTest.accepts(request));

    }

    @Test
    public void test_denies_sling_request_authentication() throws HttpCacheRepositoryAccessException {

        Map<String,Object> properties = new HashMap<>();

        properties.put(PROP_AUTHENTICATION_REQUIREMENT, AUTHENTICATED_REQUEST);
        activateWithDefaultValues(properties);

        when(resourceResolver.getUserID()).thenReturn("anonymous");

        assertFalse(systemUnderTest.accepts(request));

    }

    @Test
    public void test_denies_sling_request_authentication_anon() throws HttpCacheRepositoryAccessException {

        Map<String,Object> properties = new HashMap<>();

        properties.put(PROP_AUTHENTICATION_REQUIREMENT, ANONYMOUS_REQUEST);
        activateWithDefaultValues(properties);

        when(resourceResolver.getUserID()).thenReturn("admin");

        assertFalse(systemUnderTest.accepts(request));

    }

    @Test
    public void test_denies_sling_request_uri_whitelist() throws HttpCacheRepositoryAccessException {

        Map<String,Object> properties = new HashMap<>();

        properties.put(PROP_REQUEST_URI_PATTERNS, new String[]{"/content/(.*)"});

        activateWithDefaultValues(properties);

        when(request.getRequestURI()).thenReturn("/invalid/url");

        assertFalse(systemUnderTest.accepts(request));

    }


    @Test
    public void test_denies_sling_request_uri_blacklisted() throws HttpCacheRepositoryAccessException {

        Map<String,Object> properties = new HashMap<>();

        properties.put(PROP_REQUEST_URI_PATTERNS, new String[]{"/content/(.*)"});
        properties.put(PROP_BLACKLISTED_REQUEST_URI_PATTERNS, new String[]{"/content/[a-z]{4}/blacklisted.html"});

        activateWithDefaultValues(properties);

        when(request.getRequestURI()).thenReturn("/content/page/blacklisted.html");

        assertFalse(systemUnderTest.accepts(request));

    }

    @Test
    public void test_build_cachekey() throws HttpCacheKeyCreationException {

        activateWithDefaultValues(Collections.emptyMap());
        CacheKey cacheKey = mock(CacheKey.class);

        final String resourcePath = "/content/test.html";
        when(extension.build(resourcePath, systemUnderTest)).thenReturn(cacheKey);
        when(extension.build(request, systemUnderTest)).thenReturn(cacheKey);
        when(extension.doesKeyMatchConfig(cacheKey, systemUnderTest)).thenReturn(true);

        CacheKey producedFromResourcePath = systemUnderTest.buildCacheKey(resourcePath);
        assertSame(cacheKey, producedFromResourcePath);
        verify(extension,times(1)).build(resourcePath, systemUnderTest);


        CacheKey producedFromRequest = systemUnderTest.buildCacheKey(request);
        assertSame(cacheKey, producedFromRequest);
        verify(extension,times(1)).build(request, systemUnderTest);

        boolean knows = systemUnderTest.knows(cacheKey);
        assertTrue(knows);
        verify(extension, times(1)).doesKeyMatchConfig(cacheKey, systemUnderTest);
    }



}
