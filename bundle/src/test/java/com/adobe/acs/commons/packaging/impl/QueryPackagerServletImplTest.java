/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.packaging.impl;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageManager;
import com.day.jcr.vault.packaging.Version;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Session;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueryPackagerServletImplTest {

    static final String CONTENT_RESOURCE_PATH = "/etc/acs-commons/packages/query-packager-test/jcr:content";
    static final String CONTENT_RESOURCE_TYPE = "acs-commons/components/utilities/packager/query-packager";

    SuccessMockResourceResolver resourceResolver = new SuccessMockResourceResolver();
    ErrorMockResourceResolver errorResourceResolver = new ErrorMockResourceResolver();

    @Spy
    MockResource contentResource = new MockResource(resourceResolver, CONTENT_RESOURCE_PATH, CONTENT_RESOURCE_TYPE);

    @Spy
    MockResource configurationResource = new MockResource(resourceResolver,
            CONTENT_RESOURCE_PATH + "/configuration", "");

    @Mock
    PackageHelper packageHelper;

    @Mock
    JcrPackage jcrPackage;

    @Mock
    Session session;

    @InjectMocks
    QueryPackagerServletImpl queryPackagerServlet;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("includePatterns", new String[]{});
        map.put("conflictResolution", "IncrementVersion");

        ValueMap properties = new ValueMapDecorator(map);

        doReturn(properties).when(configurationResource).adaptTo(ValueMap.class);

        resourceResolver.addResource(contentResource);
        resourceResolver.addResource(configurationResource);

        /* PackageHelper Mocks */
        when(packageHelper.getNextVersion(any(JcrPackageManager.class), any(String.class), any(String.class),
                any(String.class))).thenReturn(Version.create("1.8.0"));

        when(packageHelper.createPackage(any(Set.class), any(Session.class), any(String.class),
                any(String.class), any(String.class), any(PackageHelper.ConflictResolution.class),
                any(Map.class))).thenReturn(jcrPackage);

        when(packageHelper.getSuccessJSON(any(JcrPackage.class))).thenReturn("{\"status\": \"success\"}");
        when(packageHelper.getErrorJSON(any(String.class))).thenReturn("{\"status\": \"error\"}");
        when(packageHelper.getPreviewJSON(any(Collection.class))).thenReturn("{\"status\": \"preview\"}");
    }

    @Test
    public void testDoPost_success() throws Exception {
        final String resourcePath = "/etc/acs-commons/packages/query-packager-test/jcr:content";
        final String selectors = "package";
        final String extension = "json";
        final String suffix = "";
        final String queryString = "";

        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourcePath, selectors,
                extension, suffix, queryString);
        request.setResourceResolver(resourceResolver);
        request.setMethod("POST");

        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        request.setResourceResolver(resourceResolver);
        request.setResource(contentResource);

        queryPackagerServlet.doPost(request, response);

        final JSONObject actual = new JSONObject(response.getOutput().toString());
        assertEquals("success", actual.optString("status", "error"));
    }


    @Test
    public void testDoPost_preview() throws Exception {
        final String resourcePath = "/etc/acs-commons/packages/query-packager-test/jcr:content";
        final String selectors = "package";
        final String extension = "json";
        final String suffix = "";
        final String queryString = "";

        PreviewMockSlingHttpServletRequest request = new PreviewMockSlingHttpServletRequest(resourcePath, selectors,
                extension, suffix, queryString);
        request.setMethod("POST");

        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        request.setResourceResolver(resourceResolver);
        request.setResource(contentResource);

        queryPackagerServlet.doPost(request, response);
        String tmp = response.getOutput().toString();
        final JSONObject actual = new JSONObject(tmp);

        assertEquals("preview", actual.optString("status", "error"));
    }

    @Test
    public void testDoPost_error() throws Exception {
        final String resourcePath = "/etc/acs-commons/packages/query-packager-test/jcr:content";
        final String selectors = "package";
        final String extension = "json";
        final String suffix = "";
        final String queryString = "";

        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourcePath, selectors,
                extension, suffix, queryString);
        request.setResourceResolver(errorResourceResolver);
        request.setMethod("POST");

        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        request.setResourceResolver(errorResourceResolver);
        request.setResource(contentResource);

        queryPackagerServlet.doPost(request, response);

        final JSONObject actual = new JSONObject(response.getOutput().toString());
        assertEquals("error", actual.optString("status", "success"));
    }


    private class SuccessMockResourceResolver extends MockResourceResolver {
        final List<Resource> results;

        public SuccessMockResourceResolver() {
            super();

            results = new LinkedList<Resource>();
            results.add(new MockResource(this, "/content/one", ""));
            results.add(new MockResource(this, "/content/two", ""));
            results.add(new MockResource(this, "/content/three", ""));

            for(Resource resource : results) {
                this.addResource(resource);
            }
        }

        @Override
        public Iterable<Resource> getChildren(final Resource resource) {
            return null;
        }

        public Iterator<Resource> findResources(String query, String language) {
            return this.results.iterator();
        }

        @Override
        public void delete(final Resource resource) throws PersistenceException {

        }

        @Override
        public Resource create(final Resource resource, final String s, final Map<String, Object> stringObjectMap) throws PersistenceException {
            return null;
        }

        @Override
        public void revert() {

        }

        @Override
        public void commit() throws PersistenceException {

        }

        @Override
        public boolean hasChanges() {
            return false;
        }

        @SuppressWarnings("unchecked")
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return (AdapterType) session;
        }
    }

    private class ErrorMockResourceResolver extends MockResourceResolver {
        final List<Resource> results;

        public ErrorMockResourceResolver() {
            super();

            results = new LinkedList<Resource>();

        }

        @Override
        public Iterable<Resource> getChildren(final Resource resource) {
            return null;
        }

        public Iterator<Resource> findResources(String query, String language) {
            return this.results.iterator();
        }

        @Override
        public void delete(final Resource resource) throws PersistenceException {

        }

        @Override
        public Resource create(final Resource resource, final String s, final Map<String, Object> stringObjectMap) throws PersistenceException {
            return null;
        }

        @Override
        public void revert() {

        }

        @Override
        public void commit() throws PersistenceException {

        }

        @Override
        public boolean hasChanges() {
            return false;
        }

        @SuppressWarnings("unchecked")
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return (AdapterType) session;
        }
    }

    private class PreviewMockSlingHttpServletRequest extends MockSlingHttpServletRequest {

        public PreviewMockSlingHttpServletRequest(final String resourcePath, final String selectors, final String extension, final String suffix, final String queryString) {
            super(resourcePath, selectors, extension, suffix, queryString);
        }

        public String getParameter(String name) {
            if("preview".equals(name)) {
                return "true";
            } else {
                return null;
            }
        }
    }

}