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
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

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

        final JSONObject json = new JSONObject();

        json.put(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.put("name", "./myValue");
        json.put("fileName", "./myFile");
        json.put("jcr:description", "words");
        json.put("noDotSlash", "no dot slash");

        visitor.accept(json);

        assertEquals("./test/myValue", json.get("name"));
        assertEquals("./test/myFile", json.get("fileName"));
        assertEquals("words", json.get("jcr:description"));
        assertEquals("test/no dot slash", json.get("noDotSlash"));
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

        final JSONObject json = new JSONObject();

        json.put(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.put("name", "./myValue");
        json.put("fileName", "./image/myFile");
        json.put("jcr:description", "words");
        json.put("noDotSlash", "no dot slash");

        visitor.accept(json);

        assertEquals("./test/myValue", json.get("name"));
        assertEquals("./test/image/myFile", json.get("fileName"));
        assertEquals("words", json.get("jcr:description"));
        assertEquals("no dot slash", json.get("noDotSlash"));
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

        final JSONObject json = new JSONObject();

        json.put(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.put("xtype", "cqinclude");
        json.put("path", "/apps/test.cqinclude.namespace.level-1.json");

        visitor.accept(json);

        assertEquals("/apps/test.cqinclude.namespace.test%252Flevel-1.json", json.get("path"));
    }

    @Test
    public void testIsCqincludeNamspaceWidget() throws JSONException {
        final CQIncludePropertyNamespaceServlet.PropertyNamespaceUpdater pnu = new CQIncludePropertyNamespaceServlet().new PropertyNamespaceUpdater("my-namespace");

        final JSONObject json = new JSONObject();

        json.put(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.put("xtype", "cqinclude");
        json.put("path", "/apps/test.cqinclude.namespace.test.json");

        assertEquals(true, pnu.isCqincludeNamespaceWidget(json));
    }

    @Test
    public void testMakeMultiLevel() throws JSONException {

        CQIncludePropertyNamespaceServlet.PropertyNamespaceUpdater pnu = new CQIncludePropertyNamespaceServlet().new PropertyNamespaceUpdater("my-namespace");

        final JSONObject json = new JSONObject();

        json.put(JcrConstants.JCR_PRIMARYTYPE, "cq:Widget");
        json.put("xtype", "cqinclude");
        json.put("path", "/apps/test.cqinclude.namespace.test.json");

        JSONObject actual = pnu.makeMultiLevel(json);

        assertEquals("/apps/test.cqinclude.namespace.my-namespace%252Ftest.json", actual.getString("path"));
    }
}