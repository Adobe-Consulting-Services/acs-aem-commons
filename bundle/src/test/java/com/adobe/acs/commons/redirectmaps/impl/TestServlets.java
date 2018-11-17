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
package com.adobe.acs.commons.redirectmaps.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.tika.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirectmaps.models.RedirectConfigModel;
import com.adobe.acs.commons.redirectmaps.models.RedirectMapModel;
import com.day.cq.commons.jcr.JcrConstants;
import javax.servlet.WriteListener;

import junitx.util.PrivateAccessor;

public class TestServlets {

    private static final Logger log = LoggerFactory.getLogger(TestServlets.class);

    @Mock
    private SlingHttpServletRequest mockSlingRequest;

    @Mock
    private SlingHttpServletResponse mockSlingResponse;

    @Mock
    private Resource mockResource;

    @Mock
    private Resource mockFileResource;

    @Mock
    private Resource mockMapContentResource;

    @Mock
    private ModifiableValueMap contentProperties;

    private String value = null;

    private ModifiableValueMap mvm = new ModifiableValueMap() {

        @Override
        public <T> T get(String name, Class<T> type) {

            return null;
        }

        @Override
        public <T> T get(String name, T defaultValue) {

            return null;
        }

        @Override
        public Object get(Object key) {

            return null;
        }

        @Override
        public int size() {

            return 0;
        }

        @Override
        public boolean isEmpty() {

            return false;
        }

        @Override
        public boolean containsKey(Object key) {

            return false;
        }

        @Override
        public boolean containsValue(Object value) {

            return false;
        }

        @Override
        public Object put(String key, Object v) {
            if (key.equals(JcrConstants.JCR_DATA)) {
                if (v instanceof InputStream) {
                    try {
                        value = IOUtils.toString((InputStream) v);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    value = String.valueOf(v);
                }
            }
            return v;
        }

        @Override
        public Object remove(Object key) {

            return null;
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> m) {

        }

        @Override
        public void clear() {

        }

        @Override
        public Set<String> keySet() {

            return null;
        }

        @Override
        public Collection<Object> values() {

            return null;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {

            return null;
        }

    };

    private ServletOutputStream os = new ServletOutputStream() {

        @Override
        public void write(int b) throws IOException {

        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener wl) {
            // Do nothing
        }

    };

    @InjectMocks
    private RedirectMapModel model;

    private List<RedirectConfigModel> redirectConfigs = new ArrayList<RedirectConfigModel>() {
        private static final long serialVersionUID = 1L;

        {
            add(new RedirectConfigModel() {

                @Override
                public String getDomain() {
                    return "www.adobe.com";
                }

                @Override
                public String getPath() {
                    return "/content/adobe";
                }

                @Override
                public String getProperty() {
                    return "vanity";
                }

                @Override
                public String getProtocol() {
                    return "https";
                }

                @Override
                public Resource getResource() {
                    return mock(Resource.class);
                }
            });
        }
    };

    @Before
    public void init() throws IOException, NoSuchFieldException {
        log.info("init");

        MockitoAnnotations.initMocks(this);

        final MockResourceResolver mockResolver = new MockResourceResolver() {
            public Iterator<Resource> findResources(String query, String language) {
                return new ArrayList<Resource>().iterator();
            }

            @SuppressWarnings("checkstyle:abbreviationaswordinname")
            public String getUserID() {
                return "admin";
            }

        };
        mockResolver.addResource(mockResource);

        doReturn(contentProperties).when(mockResource).adaptTo(ModifiableValueMap.class);
        doReturn(null).when(contentProperties).put(org.mockito.Matchers.anyString(), org.mockito.Matchers.any());

        log.debug("Setting up the request...");
        doReturn(mockResource).when(mockSlingRequest).getResource();
        doReturn(mockResolver).when(mockResource).getResourceResolver();
        doReturn("0").when(mockSlingRequest).getParameter("idx");
        doReturn("/source").when(mockSlingRequest).getParameter("source");
        doReturn("/target").when(mockSlingRequest).getParameter("target");
        doReturn("1").when(mockSlingRequest).getParameter("edit-id");
        doReturn("/edit-source").when(mockSlingRequest).getParameter("edit-source");
        doReturn("/edit-target").when(mockSlingRequest).getParameter("edit-target");
        doReturn(mockResolver).when(mockSlingRequest).getResourceResolver();
        doReturn(os).when(mockSlingResponse).getOutputStream();

        log.debug("Setting up the resource /etc");
        doReturn("/etc").when(mockResource).getPath();
        mockResolver.addResource(mockResource);

        doReturn(mockFileResource).when(mockResource).getChild(RedirectMapModel.MAP_FILE_NODE);
        doReturn(model).when(mockResource).adaptTo(RedirectMapModel.class);

        log.debug("Seetting up the resource /etc/redirectMap.txt");
        doReturn("/etc/redirectMap.txt").when(mockFileResource).getPath();
        mockResolver.addResource(mockFileResource);
        doReturn(IOUtils.toInputStream("/source1 /target1\n/source2 /target2")).when(mockFileResource)
                .adaptTo(InputStream.class);

        log.debug("Setting up the resource /etc/redirectMap.txt/jcr:content");
        doReturn(mockMapContentResource).when(mockFileResource).getChild(JcrConstants.JCR_CONTENT);
        doReturn("/etc/redirectMap.txt/jcr:content").when(mockMapContentResource).getPath();
        doReturn(mvm).when(mockMapContentResource).adaptTo(ModifiableValueMap.class);
        mockResolver.addResource(mockMapContentResource);

        log.debug("Setting up the model...");
        PrivateAccessor.setField(model, "redirects", redirectConfigs);
        PrivateAccessor.setField(model, "redirectMap", mockFileResource);
        PrivateAccessor.setField(model, "resourceResolver", mockResolver);
    }

    @Test
    public void testAddEntryServlet() throws ServletException, IOException {
        log.info("testAddEntryServlet");
        AddEntryServlet servlet = new AddEntryServlet();
        servlet.doPost(mockSlingRequest, mockSlingResponse);

        assertTrue(value.contains("/source /target"));
        log.info(value);
        log.info("Test successful!");
    }

    @Test
    public void testRemoveEntryServlet() throws ServletException, IOException {
        log.info("testRemoveEntryServlet");
        RemoveEntryServlet servlet = new RemoveEntryServlet();
        servlet.doPost(mockSlingRequest, mockSlingResponse);

        assertFalse(value.contains("/source1 /target1"));
        log.info(value);
        log.info("Test successful!");
    }
    
    @Test
    public void testUpdateServlet() throws ServletException, IOException {
        log.info("testRemoveEntryServlet");
        UpdateEntryServlet servlet = new UpdateEntryServlet();
        servlet.doPost(mockSlingRequest, mockSlingResponse);

        assertFalse(value.contains("/source2 /target2"));
        assertTrue(value.contains("/edit-source /edit-target"));
        log.info(value);
        log.info("Test successful!");
    }
}
