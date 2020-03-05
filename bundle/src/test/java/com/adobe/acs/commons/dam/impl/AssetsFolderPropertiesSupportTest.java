/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

package com.adobe.acs.commons.dam.impl;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.google.common.collect.ImmutableMap;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AssetsFolderPropertiesSupportTest {

    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Rule
    public final OsgiContext osgiContext = new OsgiContext();

    MockSlingHttpServletRequest request;

    MockSlingHttpServletResponse response;

    Resource slingFolderResource;

    Resource slingOrderedFolderResource;

    Resource invalidResourceTypeResource;

    Resource invalidPathResource;

    @Mock
    PostOperation postOperation;

    @InjectMocks
    AssetsFolderPropertiesSupport assetsFolderPropertiesSupport = new AssetsFolderPropertiesSupport();

    @Before
    public void setUp() throws Exception {

        request = new MockSlingHttpServletRequest(slingContext.resourceResolver(), osgiContext.bundleContext());
        response = new MockSlingHttpServletResponse();

        slingFolderResource = slingContext.create().resource("/content/dam/folder", ImmutableMap.<String, Object>builder()
                .put(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER)
                .build());

        slingOrderedFolderResource = slingContext.create().resource("/content/dam/folder/ordered", ImmutableMap.<String, Object>builder()
                .put(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_ORDERED_FOLDER)
                .build());

        invalidResourceTypeResource = slingContext.create().resource("/content/dam/folder/asset.png", ImmutableMap.<String, Object>builder()
                .put(JcrConstants.JCR_PRIMARYTYPE, "dam:Asset")
                .build());

        invalidPathResource
                = slingContext.create().resource("/content/site/pages", ImmutableMap.<String, Object>builder()
                .put(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER)
                .build());

        request.setResource(slingFolderResource);
        request.setParameterMap(ImmutableMap.<String, Object>builder().put(":operation", "dam.share.folder").build());
        request.setMethod("post");
    }

    @Test
    public void accepts_slingFolder() throws Exception {
        request.setResource(slingFolderResource);

        assertTrue(assetsFolderPropertiesSupport.accepts(request));
    }

    @Test
    public void accepts_slingOrderedFolder() throws Exception {
        request.setResource(slingOrderedFolderResource);

        assertTrue(assetsFolderPropertiesSupport.accepts(request));
    }

    @Test
    public void accepts_rejects_method() throws Exception {
        request.setMethod("get");

        assertFalse(assetsFolderPropertiesSupport.accepts(request));
    }

    @Test
    public void accepts_rejects_path() throws Exception {
        request.setResource(invalidPathResource);

        assertFalse(assetsFolderPropertiesSupport.accepts(request));
    }

    @Test
    public void accepts_rejects_resourceType() throws Exception {
        request.setResource(invalidResourceTypeResource);

        assertFalse(assetsFolderPropertiesSupport.accepts(request));
    }

    @Test
    public void accepts_rejects_operation() throws Exception {
        request.setParameterMap(ImmutableMap.<String, Object>builder().put(":operation", "not.dam.folder.share").build());

        assertFalse(assetsFolderPropertiesSupport.accepts(request));
    }

    @Test
    public void process() throws Exception {
        assetsFolderPropertiesSupport.process(new AssetsFolderPropertiesSupport.AssetsFolderPropertiesSupportRequest(request, null), new ArrayList<>());

        verify(postOperation, times(1)).run(any(AssetsFolderPropertiesSupport.AssetsFolderPropertiesSupportRequest.class), any(PostResponse.class), any(SlingPostProcessor[].class));
    }

    @Test
    public void process_skips() throws Exception {
        assetsFolderPropertiesSupport.process(request, new ArrayList<>());

        verify(postOperation, never()).run(eq(request), any(PostResponse.class), any(SlingPostProcessor[].class));
    }

    @Test
    public void doGet() throws Exception {
        final ValueMap graniteUiFormValues = new ValueMapDecorator(new HashMap<>());
        graniteUiFormValues.put("ootb", "ootb value");

        slingContext.create().resource("/content/dam/do-get/folder", ImmutableMap.<String, Object>builder().put("resource", "resource value").build());

        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(slingContext.resourceResolver(), osgiContext.bundleContext());
        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo)request.getRequestPathInfo();
        requestPathInfo.setResourcePath("wizard.html");
        requestPathInfo.setSuffix("/content/dam/do-get/folder");
        request.setAttribute("granite.ui.form.values", graniteUiFormValues);

        assetsFolderPropertiesSupport.doGet(request, response);

        ValueMap actual = (ValueMap) request.getAttribute("granite.ui.form.values");

        assertEquals("ootb value", actual.get("ootb", String.class));
        assertEquals("resource value", actual.get("resource", String.class));
    }
}