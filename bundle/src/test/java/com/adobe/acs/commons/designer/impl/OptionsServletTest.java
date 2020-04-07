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
package com.adobe.acs.commons.designer.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

@RunWith(MockitoJUnitRunner.class)
public class OptionsServletTest {

    @Mock
    private HtmlLibraryManager manager;

    @InjectMocks
    public OptionsServlet servlet = new OptionsServlet();

    @Test
    public void testWithNoType() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
                "/apps/acs-commons/components/utilities/designer/clientlibsmanager/options", null, "json", null,
                null);
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals("[]", response.getOutput().toString());
    }

    @Test
    public void testWithBadType() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
                "/apps/acs-commons/components/utilities/designer/clientlibsmanager/options", "html", "json", null,
                null);
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals("[]", response.getOutput().toString());
    }

    @Test
    public void testWithNormalType() throws Exception {
        Map<String, ClientLibrary> libraries = new HashMap<String, ClientLibrary>();
        String jsOnlyCategory1 = RandomStringUtils.randomAlphanumeric(5);
        String jsOnlyCategory2 = RandomStringUtils.randomAlphanumeric(5);
        String bothCategory1 = RandomStringUtils.randomAlphanumeric(5);
        String bothCategory2 = RandomStringUtils.randomAlphanumeric(5);

        addLibrary(libraries, RandomStringUtils.random(10), new String[] { "js" }, new String[] { jsOnlyCategory1,
                jsOnlyCategory2 });
        addLibrary(libraries, RandomStringUtils.random(10), new String[] { "js" },
                new String[] { jsOnlyCategory2 });
        addLibrary(libraries, RandomStringUtils.random(10), new String[] { "js", "css" }, new String[] {
                bothCategory1, bothCategory2 });
        addLibrary(libraries, RandomStringUtils.random(10), new String[] { "js", "css" },
                new String[] { bothCategory2 });

        when(manager.getLibraries()).thenReturn(libraries);

        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
                "/apps/acs-commons/components/utilities/designer/clientlibsmanager/options", "js", "json", null,
                null);
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        JSONArray array = new JSONArray(response.getOutput().toString());
        assertEquals(4, array.length());
    }

    private void
            addLibrary(Map<String, ClientLibrary> libraries, String path, String[] types, String[] categories) {
        ClientLibrary library = mock(ClientLibrary.class);
        when(library.getTypes()).thenReturn(toLibraryTypeSet(types));
        when(library.getCategories()).thenReturn(categories);
        libraries.put(path, library);
    }

    private Set<LibraryType> toLibraryTypeSet(String[] types) {
        Set<LibraryType> set = new HashSet<LibraryType>();
        for (String type : types) {
            set.add(LibraryType.valueOf(type.toUpperCase()));
        }
        return set;
    }

}
