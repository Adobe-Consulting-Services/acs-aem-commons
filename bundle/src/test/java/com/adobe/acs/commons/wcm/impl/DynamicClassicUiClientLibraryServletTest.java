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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

@RunWith(MockitoJUnitRunner.class)
public class DynamicClassicUiClientLibraryServletTest {

@InjectMocks
private DynamicClassicUiClientLibraryServlet servlet = new DynamicClassicUiClientLibraryServlet();

private static final Category LIMIT = new Category("acs-commons.cq-widgets.add-ons.classicui-limit-parsys",
"/etc/clientlibs/limit");
private static final Category PLACEHOLDER = new Category(
"acs-commons.cq-widgets.add-ons.classicui-parsys-placeholder", "/etc/clientlibs/placeholder");
private static final Category CUSTOM = new Category("custom", "/etc/clientlibs/custom");

@Mock
private SlingHttpServletRequest request;

/**
 * Mocking this does not work, because Config technically is an annotation, not a class.
 * I ignored all testcases for the moment, but that's something we need to change.
     * Converting everything to use SlingContext and using their request,response and config handling
 */

@Mock
private DynamicClassicUiClientLibraryServlet.Config config;

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

when(htmlLibraryManager.getLibraries(any(String[].class), any(LibraryType.class), eq(true), eq(true)))
.thenAnswer(i -> {
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
when(config.exclude_all()).thenReturn(true);
when(config.categories()).thenReturn(new String[0]);
when(htmlLibraryManager.isMinifyEnabled()).thenReturn(false);
}

@Test
@Ignore
public void testExcludeAll() throws Exception {
// Map<String, Object> config = Collections.singletonMap("exclude.all", true);

when(config.exclude_all()).thenReturn(true);
servlet.activate(config);

servlet.doGet(request, response);
JSONAssert.assertEquals("{'js':[], 'css':[]}", writer.toString(), false);
}

@Test
@Ignore
public void testDefault() throws Exception {
when(config.categories()).thenReturn(new String[0]);
servlet.activate(config);

servlet.doGet(request, response);
JSONAssert.assertEquals(
"{'js':['/etc/clientlibs/limit.js','/etc/clientlibs/placeholder.js'], 'css':['/etc/clientlibs/limit.css','/etc/clientlibs/placeholder.css']}",
writer.toString(), false);
}

@Test
@Ignore
public void testCustom() throws Exception {
when(config.categories()).thenReturn(new String[] { CUSTOM.id });
servlet.activate(config);

servlet.doGet(request, response);
JSONAssert.assertEquals("{'js':['/etc/clientlibs/custom.js'], 'css':['/etc/clientlibs/custom.css']}",
writer.toString(), false);
}

@Test
@Ignore
public void testDefaultWithContextPath() throws Exception {
when(request.getContextPath()).thenReturn("/test");
when(config.categories()).thenReturn(new String[0]);
servlet.activate(config);

servlet.doGet(request, response);
JSONAssert.assertEquals(
"{'js':['/test/etc/clientlibs/limit.js','/test/etc/clientlibs/placeholder.js'], 'css':['/test/etc/clientlibs/limit.css','/test/etc/clientlibs/placeholder.css']}",
writer.toString(), false);
}

@Test
@Ignore
public void testCustomWithContextPath() throws Exception {
when(request.getContextPath()).thenReturn("/test");
when(config.categories()).thenReturn(new String[] { CUSTOM.id });
servlet.activate(config);

servlet.doGet(request, response);
JSONAssert.assertEquals("{'js':['/test/etc/clientlibs/custom.js'], 'css':['/test/etc/clientlibs/custom.css']}",
writer.toString(), false);
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
return path + (i.getArgumentAt(1, Boolean.class) ? ".min" : "")
+ i.getArgumentAt(0, LibraryType.class).extension;
});
return cl;
}
}

}