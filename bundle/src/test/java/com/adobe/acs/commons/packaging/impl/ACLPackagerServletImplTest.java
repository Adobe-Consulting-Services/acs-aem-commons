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
import com.adobe.acs.commons.util.AemCapabilityHelper;
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
import org.junit.After;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ACLPackagerServletImplTest {

    static final String CONTENT_RESOURCE_PATH = "/etc/acs-commons/packages/acl-packager-test/jcr:content";
    static final String CONTENT_RESOURCE_TYPE = "acs-commons/components/utilities/packager/acl-packager";

    Crx2SuccessMockResourceResolver crx2ResourceResolver = new Crx2SuccessMockResourceResolver();
    OakSuccessMockResourceResolver oakResourceResolver = new OakSuccessMockResourceResolver();

    ErrorMockResourceResolver errorResourceResolver = new ErrorMockResourceResolver();

    @Spy
    MockResource crx2ContentResource = new MockResource(crx2ResourceResolver, CONTENT_RESOURCE_PATH, CONTENT_RESOURCE_TYPE);

    @Spy
    MockResource crx2ConfigurationResource = new MockResource(crx2ResourceResolver,
            CONTENT_RESOURCE_PATH + "/configuration", "");


    @Spy
    MockResource oakContentResource = new MockResource(oakResourceResolver, CONTENT_RESOURCE_PATH,
            CONTENT_RESOURCE_TYPE);

    @Spy
    MockResource oakConfigurationResource = new MockResource(oakResourceResolver,
            CONTENT_RESOURCE_PATH + "/configuration", "");


    @Mock
    PackageHelper packageHelper;

    @Mock
    JcrPackage jcrPackage;

    @Mock
    AemCapabilityHelper aemCapabilityHelper;

    @Mock
    Session session;

    @InjectMocks
    ACLPackagerServletImpl aclPackagerServlet;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("includePatterns", new String[]{});
        map.put("principalNames", new String[]{});
        map.put("conflictResolution", "IncrementVersion");

        ValueMap properties = new ValueMapDecorator(map);

        doReturn(properties).when(crx2ConfigurationResource).adaptTo(ValueMap.class);
        doReturn(properties).when(oakConfigurationResource).adaptTo(ValueMap.class);

        crx2ResourceResolver.addResource(crx2ContentResource);
        crx2ResourceResolver.addResource(crx2ConfigurationResource);

        oakResourceResolver.addResource(oakContentResource);
        oakResourceResolver.addResource(oakConfigurationResource);

        /* PackageHelper Mocks */
        when(packageHelper.getNextVersion(any(JcrPackageManager.class), any(String.class), any(String.class),
                any(String.class))).thenReturn(Version.create("1.8.0"));

        when(packageHelper.createPackage(any(Set.class), any(Session.class), any(String.class),
                any(String.class), any(String.class), any(PackageHelper.ConflictResolution.class),
                any(Map.class))).thenReturn(jcrPackage);

        when(packageHelper.getSuccessJSON(any(JcrPackage.class))).thenReturn("{\"status\": \"success\"}");
        when(packageHelper.getErrorJSON(any(String.class))).thenReturn("{\"status\": \"error\"}");
        when(packageHelper.getPathFilterSetPreviewJSON(any(Collection.class))).thenReturn("{\"status\": \"preview\"}");

    }

    @After
    public void tearDown() throws Exception {
        reset(packageHelper, session, jcrPackage);
    }

    @Test
    public void testDoPost_crx2_success() throws Exception {
        when(aemCapabilityHelper.isOak()).thenReturn(false);

        final String resourcePath = "/etc/acs-commons/packages/acl-packager-test/jcr:content";
        final String selectors = "package";
        final String extension = "json";
        final String suffix = "";
        final String queryString = "";

        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourcePath, selectors,
                extension, suffix, queryString);
        request.setResourceResolver(crx2ResourceResolver);
        request.setMethod("POST");

        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        request.setResourceResolver(crx2ResourceResolver);
        request.setResource(crx2ContentResource);

        aclPackagerServlet.doPost(request, response);

        final JSONObject actual = new JSONObject(response.getOutput().toString());
        assertEquals("success", actual.optString("status", "error"));
    }


    @Test
    public void testDoPost_oak_success() throws Exception {
        when(aemCapabilityHelper.isOak()).thenReturn(true);

        final String resourcePath = "/etc/acs-commons/packages/acl-packager-test/jcr:content";
        final String selectors = "package";
        final String extension = "json";
        final String suffix = "";
        final String queryString = "";

        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourcePath, selectors,
                extension, suffix, queryString);
        request.setResourceResolver(oakResourceResolver);
        request.setMethod("POST");

        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        request.setResourceResolver(oakResourceResolver);
        request.setResource(oakContentResource);

        aclPackagerServlet.doPost(request, response);

        final JSONObject actual = new JSONObject(response.getOutput().toString());
        assertEquals("success", actual.optString("status", "error"));
    }


    @Test
    public void testDoPost_preview() throws Exception {
        final String resourcePath = "/etc/acs-commons/packages/acl-packager-test/jcr:content";
        final String selectors = "package";
        final String extension = "json";
        final String suffix = "";
        final String queryString = "";

        PreviewMockSlingHttpServletRequest request = new PreviewMockSlingHttpServletRequest(resourcePath, selectors,
                extension, suffix, queryString);
        request.setMethod("POST");

        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        request.setResourceResolver(crx2ResourceResolver);
        request.setResource(crx2ContentResource);

        aclPackagerServlet.doPost(request, response);
        String tmp = response.getOutput().toString();
        System.out.println(tmp);
        final JSONObject actual = new JSONObject(tmp);

        assertEquals("preview", actual.optString("status", "error"));
    }

    @Test
    public void testDoPost_error() throws Exception {
        final String resourcePath = "/etc/acs-commons/packages/acl-packager-test/jcr:content";
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
        request.setResource(crx2ContentResource);

        aclPackagerServlet.doPost(request, response);

        final JSONObject actual = new JSONObject(response.getOutput().toString());
        assertEquals("error", actual.optString("status", "success"));
    }


    private class Crx2SuccessMockResourceResolver extends MockResourceResolver {
        final List<Resource> results;

        public Crx2SuccessMockResourceResolver() {
            super();

            results = new LinkedList<Resource>();
            results.add(new MockResource(this, "/content/dam/rep:policy", "rep:ACL"));
            results.add(new MockResource(this, "/content/acs-commons/rep:policy", "rep:ACL"));
            results.add(new MockResource(this, "/etc/workflow/packages/rep:policy", "rep:ACL"));
            results.add(new MockResource(this, "/var/audit/1/2/3/rep:policy", "rep:ACL"));
            results.add(new MockResource(this, "/home/groups/authors/rep:policy", "rep:ACL"));

            for(Resource resource : results) {
                this.addResource(resource);
            }

            this.addResource(new MockResource(this, "/content/dam/rep:policy/allow0", ""));
            this.addResource(new MockResource(this, "/content/dam/rep:policy/allow1", ""));
            this.addResource(new MockResource(this, "/content/acs-commons/rep:policy/deny0", ""));
            this.addResource(new MockResource(this, "/etc/workflow/packages/rep:policy/allow0", ""));
            this.addResource(new MockResource(this, "/var/audit/1/2/3/rep:policy/allow", ""));
            this.addResource(new MockResource(this, "/home/groups/authors/rep:policy/allow0", ""));
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

        @Override
        public boolean isResourceType(Resource resource, String resourceType) {
            return resource.getResourceType().equals(resourceType);
        }

        @SuppressWarnings("unchecked")
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return (AdapterType) session;
        }
    }

    private class OakSuccessMockResourceResolver extends MockResourceResolver {
        final List<Resource> results;

        public OakSuccessMockResourceResolver() {
            super();

            results = new LinkedList<Resource>();
            results.add(new MockResource(this, "/content/dam/rep:policy/allow0", ""));
            results.add(new MockResource(this, "/content/dam/rep:policy/allow1", ""));
            results.add(new MockResource(this, "/content/acs-commons/rep:policy/deny", ""));
            results.add(new MockResource(this, "/etc/workflow/packages/rep:policy/allow", ""));
            results.add(new MockResource(this, "/var/audit/1/2/3/rep:policy/deny", ""));
            results.add(new MockResource(this, "/home/groups/authors/rep:policy/allow", ""));

            for(Resource resource : results) {
                this.addResource(resource);
            }

            this.addResource(new MockResource(this, "/content/dam/rep:policy", "rep:ACL"));
            this.addResource(new MockResource(this, "/content/acs-commons/rep:policy", "rep:ACL"));
            this.addResource(new MockResource(this, "/etc/workflow/packages/rep:policy", "rep:ACL"));
            this.addResource(new MockResource(this, "/var/audit/1/2/3/rep:policy", "rep:ACL"));
            this.addResource(new MockResource(this, "/home/groups/authors/rep:policy", "rep:ACL"));
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

        @Override
        public boolean isResourceType(Resource resource, String resourceType) {
            return resource.getResourceType().equals(resourceType);
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
            results.add(new MockResource(this, "/content/dam/rep:policy", "rep:ACL"));
            results.add(new MockResource(this, "/content/acs-commons/rep:policy", "rep:ACL"));
            results.add(new MockResource(this, "/etc/workflow/packages/rep:policy", "rep:ACL"));
            results.add(new MockResource(this, "/var/audit/1/2/3/rep:policy", "rep:ACL"));
            results.add(new MockResource(this, "/home/groups/authors/rep:policy", "rep:ACL"));

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
