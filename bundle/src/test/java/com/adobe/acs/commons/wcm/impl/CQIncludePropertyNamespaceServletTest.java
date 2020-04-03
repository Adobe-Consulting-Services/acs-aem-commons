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

import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("checkstyle:abbreviationaswordinname")
@RunWith(MockitoJUnitRunner.class)
public class CQIncludePropertyNamespaceServletTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testNamePropertyUpdater_PropertyNames() throws Exception {
        final Map<String, Object> config = new HashMap<String, Object>();
        config.put(CQIncludePropertyNamespaceServlet.PROP_NAMESPACEABLE_PROPERTY_NAMES,
                new String[]{"fileName", "name", "noDotSlash"});

        final CQIncludePropertyNamespaceServlet servlet = new CQIncludePropertyNamespaceServlet();
        servlet.activate(config);

        final CQIncludePropertyNamespaceServlet.PropertyNamespaceUpdater visitor =
                servlet.new PropertyNamespaceUpdater("test");

        final JsonObject json = new JsonObject();

        json.addProperty(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.addProperty("name", "./myValue");
        json.addProperty("fileName", "./myFile");
        json.addProperty("jcr:description", "words");
        json.addProperty("noDotSlash", "no dot slash");

        visitor.accept(json);

        assertEquals("./test/myValue", json.get("name").getAsString());
        assertEquals("./test/myFile", json.get("fileName").getAsString());
        assertEquals("words", json.get("jcr:description").getAsString());
        assertEquals("test/no dot slash", json.get("noDotSlash").getAsString());
    }


    @Test
    public void testNamePropertyUpdater_PropertyValuePatterns() throws Exception {
        final Map<String, Object> config = new HashMap<String, Object>();
        config.put(CQIncludePropertyNamespaceServlet.PROP_NAMESPACEABLE_PROPERTY_VALUE_PATTERNS,
                new String[]{"^\\./.*"});

        final CQIncludePropertyNamespaceServlet servlet = new CQIncludePropertyNamespaceServlet();
        servlet.activate(config);

        final CQIncludePropertyNamespaceServlet.PropertyNamespaceUpdater visitor =
                servlet.new PropertyNamespaceUpdater("test");

        final JsonObject json = new JsonObject();

        json.addProperty(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.addProperty("name", "./myValue");
        json.addProperty("fileName", "./image/myFile");
        json.addProperty("jcr:description", "words");
        json.addProperty("noDotSlash", "no dot slash");

        visitor.accept(json);

        assertEquals("./test/myValue", json.get("name").getAsString());
        assertEquals("./test/image/myFile", json.get("fileName").getAsString());
        assertEquals("words", json.get("jcr:description").getAsString());
        assertEquals("no dot slash", json.get("noDotSlash").getAsString());
    }



    @Test
    public void testNamePropertyUpdater_MultiLevel() throws Exception {
        final Map<String, Object> config = new HashMap<String, Object>();
        config.put(CQIncludePropertyNamespaceServlet.PROP_NAMESPACEABLE_PROPERTY_VALUE_PATTERNS,
                new String[]{"^\\./.*"});
        config.put("namespace.multi-level", true);

        final CQIncludePropertyNamespaceServlet servlet = new CQIncludePropertyNamespaceServlet();
        servlet.activate(config);

        final CQIncludePropertyNamespaceServlet.PropertyNamespaceUpdater visitor =
                servlet.new PropertyNamespaceUpdater("test");

        final JsonObject json = new JsonObject();

        json.addProperty(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.addProperty("xtype", "cqinclude");
        json.addProperty("path", "/apps/test.cqinclude.namespace.level-1.json");

        visitor.accept(json);

        assertEquals("/apps/test.cqinclude.namespace.test%252Flevel-1.json", json.get("path").getAsString());
    }

    @Test
    public void testIsCqincludeNamspaceWidget() {
        final CQIncludePropertyNamespaceServlet.PropertyNamespaceUpdater pnu = new CQIncludePropertyNamespaceServlet().new PropertyNamespaceUpdater("my-namespace");

        final JsonObject json = new JsonObject();

        json.addProperty(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.addProperty("xtype", "cqinclude");
        json.addProperty("path", "/apps/test.cqinclude.namespace.test.json");

        assertEquals(true, pnu.isCqincludeNamespaceWidget(json));
    }

    @Test
    public void testMakeMultiLevel() {

        CQIncludePropertyNamespaceServlet.PropertyNamespaceUpdater pnu = new CQIncludePropertyNamespaceServlet().new PropertyNamespaceUpdater("my-namespace");

        final JsonObject json = new JsonObject();

        json.addProperty(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.addProperty("xtype", "cqinclude");
        json.addProperty("path", "/apps/test.cqinclude.namespace.test.json");

        JsonObject actual = pnu.makeMultiLevel(json);

        assertEquals("/apps/test.cqinclude.namespace.my-namespace%252Ftest.json", actual.get("path").getAsString());
    }
}