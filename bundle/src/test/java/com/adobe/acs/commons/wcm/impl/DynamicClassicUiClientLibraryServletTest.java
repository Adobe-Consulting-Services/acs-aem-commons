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
package com.adobe.acs.commons.wcm.impl;

import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;
import com.day.cq.commons.Externalizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DynamicClassicUiClientLibraryServletTest {

    @InjectMocks
    private DynamicClassicUiClientLibraryServlet servlet = new DynamicClassicUiClientLibraryServlet();

    private static final Category LIMIT = new Category("acs-commons.cq-widgets.add-ons.classicui-limit-parsys", "/etc/clientlibs/limit");
    private static final Category PLACEHOLDER = new Category("acs-commons.cq-widgets.add-ons.classicui-parsys-placeholder", "/etc/clientlibs/placeholder");
    private static final Category CUSTOM = new Category("custom", "/etc/clientlibs/custom");

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private HtmlLibraryManager htmlLibraryManager;

    private StringWriter writer;

    @Before
    public void setup() throws Exception {
        writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.map(any(SlingHttpServletRequest.class), anyString())).then(i -> {
            SlingHttpServletRequest request = i.getArgumentAt(0, SlingHttpServletRequest.class);
            String path = i.getArgumentAt(1, String.class);
            if (request != null && StringUtils.isNotBlank(request.getContextPath())) {
                return request.getContextPath().concat(path);
            } else {
                return path;
            }
        });

        when(htmlLibraryManager.getLibraries(any(String[].class), any(LibraryType.class), eq(true), eq(true))).thenAnswer(i -> {
            Set<ClientLibrary> result = new HashSet<>();
            for (String category : i.getArgumentAt(0, String[].class)) {
                if (category.equals(LIMIT.id)) {
                    result.add(LIMIT.getClientLibrary());
                } else if (category.equals(PLACEHOLDER.id)) {
                    result.add(PLACEHOLDER.getClientLibrary());
                } else if (category.equals(CUSTOM.id)) {
                    result.add(CUSTOM.getClientLibrary());
                }
            }
            return result;
        });

        when(htmlLibraryManager.isMinifyEnabled()).thenReturn(false);
    }

    @Test
    public void testExcludeAll() throws Exception {
        Map<String, Object> config = Collections.singletonMap("exclude.all", true);
        servlet.activate(config);

        servlet.doGet(request, response);
        JSONAssert.assertEquals("{'js':[], 'css':[]}", writer.toString(), false);
    }

    @Test
    public void testDefault() throws Exception {
        servlet.activate(Collections.emptyMap());

        servlet.doGet(request, response);
        JSONAssert.assertEquals("{'js':['/etc/clientlibs/limit.js','/etc/clientlibs/placeholder.js'], 'css':['/etc/clientlibs/limit.css','/etc/clientlibs/placeholder.css']}", writer.toString(), false);
    }

    @Test
    public void testCustom() throws Exception {
        servlet.activate(Collections.singletonMap("categories", new String[] { CUSTOM.id }));

        servlet.doGet(request, response);
        JSONAssert.assertEquals("{'js':['/etc/clientlibs/custom.js'], 'css':['/etc/clientlibs/custom.css']}", writer.toString(), false);
    }

    @Test
    public void testDefaultWithContextPath() throws Exception {
        when(request.getContextPath()).thenReturn("/test");
        servlet.activate(Collections.emptyMap());

        servlet.doGet(request, response);
        JSONAssert.assertEquals("{'js':['/test/etc/clientlibs/limit.js','/test/etc/clientlibs/placeholder.js'], 'css':['/test/etc/clientlibs/limit.css','/test/etc/clientlibs/placeholder.css']}", writer.toString(), false);
    }

    @Test
    public void testCustomWithContextPath() throws Exception {
        when(request.getContextPath()).thenReturn("/test");
        servlet.activate(Collections.singletonMap("categories", new String[] { CUSTOM.id }));

        servlet.doGet(request, response);
        JSONAssert.assertEquals("{'js':['/test/etc/clientlibs/custom.js'], 'css':['/test/etc/clientlibs/custom.css']}", writer.toString(), false);
    }

    private static class Category {
        private String id;
        private String path;

        private Category(String id, String path) {
            this.id = id;
            this.path = path;
        }

        private ClientLibrary getClientLibrary() {
            ClientLibrary cl = mock(ClientLibrary.class);
            when(cl.getIncludePath(any(LibraryType.class), anyBoolean())).then(i -> {
                return path + (i.getArgumentAt(1, Boolean.class) ? ".min" : "") + i.getArgumentAt(0, LibraryType.class).extension;
            });
            return cl;
        }
    }

}