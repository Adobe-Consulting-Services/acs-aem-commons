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
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
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
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ACLPackagerServletImplTest {

    static final String CONTENT_RESOURCE_PATH = "/etc/acs-commons/packages/acl-packager-test/jcr:content";
    static final String CONTENT_RESOURCE_TYPE = "acs-commons/components/utilities/packager/acl-packager";

    @Spy
    SuccessMockResourceResolver resourceResolver = new SuccessMockResourceResolver();

    @Spy
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

    @Mock Session session;

    @Mock
    UserManager userManager;

    @InjectMocks
    ACLPackagerServletImpl aclPackagerServlet;

    @Before
    public void setUp() throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("includePatterns", new String[]{});
        map.put("principalNames", new String[]{});
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
        when(packageHelper.getPreviewJSON(any(Set.class))).thenReturn("{\"status\": \"preview\"}");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testDoPost_success() throws Exception {
        final String resourcePath = "/etc/acs-commons/packages/acl-packager-test/jcr:content";
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

        aclPackagerServlet.doPost(request, response);

        final JSONObject actual = new JSONObject(response.getOutput().toString());
        assertEquals("success", actual.optString("status", "error"));
    }

    @Test
    public void testDoPost_includePrincipals() throws Exception {
        when(resourceResolver.adaptTo(UserManager.class)).thenReturn(userManager);

        Authorizable justin = mock(Authorizable.class);
        when(justin.getPath()).thenReturn("/home/users/justin");
        when(userManager.getAuthorizable("justin")).thenReturn(justin);
        when(resourceResolver.getResource("/home/users/justin")).thenReturn(new MockResource(resourceResolver, "/home/users/justin", "rep:User"));

        final String resourcePath = "/etc/acs-commons/packages/acl-packager-test/jcr:content";
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

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("includePatterns", new String[]{});
        map.put("principalNames", new String[]{ "justin" });
        map.put("includePrincipals", true);
        map.put("conflictResolution", "IncrementVersion");

        ValueMap properties = new ValueMapDecorator(map);

        doReturn(properties).when(configurationResource).adaptTo(ValueMap.class);

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

        request.setResourceResolver(resourceResolver);
        request.setResource(contentResource);

        aclPackagerServlet.doPost(request, response);
        final JSONObject actual = new JSONObject(response.getOutput().toString());
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
        request.setResource(contentResource);

        aclPackagerServlet.doPost(request, response);

        final JSONObject actual = new JSONObject(response.getOutput().toString());
        assertEquals("error", actual.optString("status", "success"));
    }


    private class SuccessMockResourceResolver extends MockResourceResolver {
        final List<Resource> results;

        public SuccessMockResourceResolver() {
            super();

            results = new LinkedList<Resource>();
            results.add(new MockResource(this, "/content/dam/rep:policy", ""));
            results.add(new MockResource(this, "/content/acs-commons/rep:policy", ""));
            results.add(new MockResource(this, "/etc/workflow/packages/rep:policy", ""));
            results.add(new MockResource(this, "/var/audit/1/2/3/rep:policy", ""));
            results.add(new MockResource(this, "/home/groups/authors/rep:policy", ""));

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

        public Iterator<Resource> findResources(String query, String language) {
            return this.results.iterator();
        }

        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return (AdapterType) session;
        }
    }

    private class ErrorMockResourceResolver extends MockResourceResolver {
        final List<Resource> results;

        public ErrorMockResourceResolver() {
            super();

            results = new LinkedList<Resource>();
            results.add(new MockResource(this, "/content/dam/rep:policy", ""));
            results.add(new MockResource(this, "/content/acs-commons/rep:policy", ""));
            results.add(new MockResource(this, "/etc/workflow/packages/rep:policy", ""));
            results.add(new MockResource(this, "/var/audit/1/2/3/rep:policy", ""));
            results.add(new MockResource(this, "/home/groups/authors/rep:policy", ""));

            for(Resource resource : results) {
                this.addResource(resource);
            }
        }

        public Iterator<Resource> findResources(String query, String language) {
            return this.results.iterator();
        }

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

    private List<String> getRootPathsFromResponse(final MockSlingHttpServletResponse response) throws JSONException {
        final List<String> rootPaths = new ArrayList<String>();
        final JSONObject actual = new JSONObject(response.getOutput().toString());

        JSONArray filterSets = actual.optJSONArray("filterSets");
        if(filterSets != null) {

            for (int i = 0; i < filterSets.length(); i++) {
                final JSONObject jsonObject = filterSets.optJSONObject(i);
                if (jsonObject != null) {
                    final String rootPath = jsonObject.optString("rootPath");
                    if (StringUtils.isNotBlank(rootPath)) {
                        rootPaths.add(rootPath);
                    }
                }
            }
        }

        return rootPaths;
    }

}
