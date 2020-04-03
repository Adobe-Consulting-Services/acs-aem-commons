/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.*;
import static com.adobe.acs.commons.httpcache.config.impl.ResourceTypeHttpCacheConfigExtension.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResourceTypeHttpCacheConfigExtensionTest {

    static final String RT_PARENT_COMP = "acs-commons/components/parentcomponent";
    static final String RT_REGULAR_COMP = "acs-commons/components/regular";
    static final String RT_SUB_COMP = "acs-commons/components/subcomponent";


    @Mock
    SlingHttpServletRequest request;

    @Mock
    Resource resource;

    @Mock
    Resource jcrContentChild;

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    HttpCacheConfig config;

    @InjectMocks
    ResourceTypeHttpCacheConfigExtension extension;


    @Before
    public void setUp() throws Exception {
        when(request.getResource()).thenReturn(resource);
        when(resource.getChild(JcrConstants.JCR_CONTENT)).thenReturn(jcrContentChild);
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(jcrContentChild.getResourceResolver()).thenReturn(resourceResolver);
    }

    @Test
    public void test_path_negative() throws HttpCacheRepositoryAccessException {


        HashMap<String,Object> properties = new HashMap<>();
        properties.put(PROP_PATHS, new String[]{"/content/(.*)"});
        extension.activate(properties);

        when(resource.getPath()).thenReturn("/lolwut");
        assertFalse(extension.accepts(request, config));

    }

    @Test
    public void test_jcr_content() throws HttpCacheRepositoryAccessException {

        HashMap<String,Object> properties = new HashMap<>();
        properties.put(PROP_PATHS, new String[]{"/content/(.*)"});
        properties.put(PROP_RESOURCE_TYPES, new String[]{RT_PARENT_COMP});
        properties.put(PROP_CHECK_RESOURCE_SUPER_TYPE, TRUE);
        properties.put(PROP_CHECK_CONTENT_RESOURCE_TYPE, TRUE);
        extension.activate(properties);

        when(resource.getPath()).thenReturn("/content/acs-commons/path/to/page");
        when(resourceResolver.isResourceType(jcrContentChild, RT_PARENT_COMP)).thenReturn(true);

        assertTrue(extension.accepts(request, config));
        verify(resource, times(1)).getChild(JcrConstants.JCR_CONTENT);
    }

    @Test
    public void test_regex() throws HttpCacheRepositoryAccessException {
        HashMap<String,Object> properties = new HashMap<>();
        properties.put(PROP_PATHS, new String[]{"/content/(.*)"});
        properties.put(PROP_RESOURCE_TYPES, new String[]{"acs-commons/components/(.*)"});
        properties.put(PROP_CHECK_RESOURCE_SUPER_TYPE, FALSE);
        extension.activate(properties);

        when(resource.getPath()).thenReturn("/content/acs-commons/path/to/page/jcr:content/component");
        when(resource.getResourceType()).thenReturn(RT_REGULAR_COMP);

        assertTrue(extension.accepts(request, config));
        verify(resourceResolver, never()).isResourceType(resource, "acs-commons/components/(.*)");

    }

    @Test
    public void test_super_type_positive() throws HttpCacheRepositoryAccessException {


        HashMap<String,Object> properties = new HashMap<>();
        properties.put(PROP_PATHS, new String[]{"/content/(.*)"});
        properties.put(PROP_RESOURCE_TYPES, new String[]{RT_PARENT_COMP});
        properties.put(PROP_CHECK_RESOURCE_SUPER_TYPE, TRUE);
        extension.activate(properties);

        when(resource.getPath()).thenReturn("/content/acs-commons/path/to/page/jcr:content/component");
        when(resource.getResourceType()).thenReturn(RT_SUB_COMP);
        when(resourceResolver.isResourceType(resource, RT_PARENT_COMP)).thenReturn(true);

        assertTrue(extension.accepts(request, config));

        verify(resourceResolver, times(1)).isResourceType(resource, RT_PARENT_COMP);

    }

    @Test
    public void test_super_type_negative() throws HttpCacheRepositoryAccessException {


        HashMap<String,Object> properties = new HashMap<>();
        properties.put(PROP_PATHS, new String[]{"/content/(.*)"});
        properties.put(PROP_RESOURCE_TYPES, new String[]{RT_PARENT_COMP});
        properties.put(PROP_CHECK_RESOURCE_SUPER_TYPE, TRUE);
        extension.activate(properties);

        when(resource.getPath()).thenReturn("/content/acs-commons/path/to/page/jcr:content/component");
        when(resource.getResourceType()).thenReturn(RT_SUB_COMP);
        when(resourceResolver.isResourceType(resource, RT_PARENT_COMP)).thenReturn(false);

        assertFalse(extension.accepts(request, config));
        verify(resourceResolver, times(1)).isResourceType(resource, RT_PARENT_COMP);

    }
}