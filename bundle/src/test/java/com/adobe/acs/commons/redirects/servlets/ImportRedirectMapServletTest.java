/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.models.RedirectRule;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;

import static com.adobe.acs.commons.redirects.Asserts.assertDateEquals;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static org.junit.Assert.*;

public class ImportRedirectMapServletTest {
    @Rule
    public SlingContext context = new SlingContext(
            ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ImportRedirectMapServlet servlet;
    private String redirectStoragePath = "/conf/acs-commons/redirects";

    @Before
    public void setUp() {
        servlet = new ImportRedirectMapServlet();
        context.request().addRequestParameter("path", redirectStoragePath);
        context.addModelsForClasses(RedirectRule.class);
        context.build().resource(redirectStoragePath);
    }

    @Test
    public void testImport() throws ServletException, IOException {
        List<RedirectRule> excelRules = Arrays.asList(
                new RedirectRule.Builder()
                        .setSource("/content/1")
                        .setTarget("/en/we-retail")
                        .setStatusCode(301)
                        .setUntilDate(new Calendar.Builder().setDate(1974, 01, 16).build())
                        .setNotes("note-abc")
                        .setTagIds(new String[]{"redirects:tag1", "redirects:tag2"})
                        .build(),
                new RedirectRule.Builder()
                        .setSource("/content/2")
                        .setTarget("/en/we-retail")
                        .setStatusCode(301)
                        .build(),
                // this one will overlay the existing rule in the repository
                new RedirectRule.Builder()
                        .setSource("/content/three")
                        .setTarget("/en/we-retail")
                        .setStatusCode(301)
                        .build()
        );

        ResourceBuilder rb = context.build().resource(redirectStoragePath).siblingsMode();
        rb.resource("redirect-saved-1",
                "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                RedirectRule.SOURCE_PROPERTY_NAME, "/content/one",
                RedirectRule.TARGET_PROPERTY_NAME, "/content/two",
                RedirectRule.STATUS_CODE_PROPERTY_NAME, 302,
                RedirectRule.UNTIL_DATE_PROPERTY_NAME, new Calendar.Builder().setDate(2022, 9, 9).build(),
                RedirectRule.NOTE_PROPERTY_NAME, "note-1",
                RedirectRule.CONTEXT_PREFIX_IGNORED, true,
                "jcr:created", "john.doe",
                "custom-1", "123",
                "cq:tags", "redirects:tag3"
        );
        rb.resource("redirect-saved-2",
                "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                RedirectRule.SOURCE_PROPERTY_NAME, "/content/three",
                RedirectRule.TARGET_PROPERTY_NAME, "/content/four",
                RedirectRule.STATUS_CODE_PROPERTY_NAME, 301,
                RedirectRule.UNTIL_DATE_PROPERTY_NAME, null,
                RedirectRule.NOTE_PROPERTY_NAME, "note-2",
                RedirectRule.CONTEXT_PREFIX_IGNORED, false,
                "jcr:created", "xyz",
                "custom-2", "345"
        );

        XSSFWorkbook wb = ExportRedirectMapServlet.export(excelRules);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        byte[] excelBytes = out.toByteArray();

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.addRequestParameter("file", excelBytes, "binary/data");

        servlet.doPost(request, response);

        Resource storageRoot = context.resourceResolver().getResource(redirectStoragePath);
        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 4, rules.size());

        Resource res1 = rules.get("/content/one");
        assertEquals("redirect-saved-1", res1.getName()); // node name is preserved
        RedirectRule rule1 = res1.adaptTo(RedirectRule.class);
        assertEquals("/content/two", rule1.getTarget());
        assertDateEquals("09 October 2022", rule1.getUntilDate());
        assertEquals("note-1", rule1.getNote());
        assertEquals("john.doe", res1.getValueMap().get("jcr:created"));
        assertEquals("123", res1.getValueMap().get("custom-1"));
        assertArrayEquals(new String[]{"redirects:tag3"}, rule1.getTagIds());

        Resource res2 = rules.get("/content/three");
        assertEquals("redirect-saved-2", res2.getName()); // node name is preserved
        RedirectRule rule2 = res2.adaptTo(RedirectRule.class);
        assertEquals("/en/we-retail", rule2.getTarget());
        assertEquals(301, rule2.getStatusCode());
        assertFalse(rule2.getContextPrefixIgnored());
        assertEquals("xyz", res2.getValueMap().get("jcr:created"));
        assertEquals("345", res2.getValueMap().get("custom-2"));

        RedirectRule rule3 = rules.get("/content/1").adaptTo(RedirectRule.class);
        assertEquals("/en/we-retail", rule3.getTarget());
        assertDateEquals("16 February 1974", rule3.getUntilDate());
        assertEquals("note-abc", rule3.getNote());
        assertArrayEquals(new String[]{"redirects:tag1", "redirects:tag2"}, rule3.getTagIds());

        RedirectRule rule4 = rules.get("/content/2").adaptTo(RedirectRule.class);
        assertEquals("/en/we-retail", rule4.getTarget());
        assertEquals(null, rule4.getUntilDate());

    }


    @Test
    public void testUpdate() throws IOException {
        Map<String, Object> rule1 = new HashMap<>();
        rule1.put("sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE);
        rule1.put(RedirectRule.SOURCE_PROPERTY_NAME, "/a1");
        rule1.put(RedirectRule.TARGET_PROPERTY_NAME, "/b1");
        rule1.put(RedirectRule.STATUS_CODE_PROPERTY_NAME, 301);

        Map<String, Object> rule2 = new HashMap<>();
        rule2.put("sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE);
        rule2.put(RedirectRule.SOURCE_PROPERTY_NAME, "/a2");
        rule2.put(RedirectRule.TARGET_PROPERTY_NAME, "/b2");
        rule2.put(RedirectRule.STATUS_CODE_PROPERTY_NAME, 302);
        rule2.put(RedirectRule.UNTIL_DATE_PROPERTY_NAME, Calendar.getInstance());
        rule2.put(RedirectRule.NOTE_PROPERTY_NAME, "note");
        Collection<Map<String, Object>> rules = Arrays.asList(rule1, rule2);

        Resource root = context.resourceResolver().getResource(redirectStoragePath);
        servlet.update(root, rules, Collections.emptyMap());

        Map<String, Resource> redirects = servlet.getRules(root);
        ValueMap vm1 = redirects.get(rule1.get(RedirectRule.SOURCE_PROPERTY_NAME)).getValueMap();

        assertEquals(vm1.get(RedirectRule.SOURCE_PROPERTY_NAME), rule1.get(RedirectRule.SOURCE_PROPERTY_NAME));
        assertEquals(vm1.get(RedirectRule.TARGET_PROPERTY_NAME), rule1.get(RedirectRule.TARGET_PROPERTY_NAME));
        assertFalse(vm1.containsKey(RedirectRule.UNTIL_DATE_PROPERTY_NAME));
        assertFalse(vm1.containsKey(RedirectRule.NOTE_PROPERTY_NAME));

        ValueMap vm2 = redirects.get(rule2.get(RedirectRule.SOURCE_PROPERTY_NAME)).getValueMap();
        assertEquals(vm2.get(RedirectRule.SOURCE_PROPERTY_NAME), rule2.get(RedirectRule.SOURCE_PROPERTY_NAME));
        assertEquals(vm2.get(RedirectRule.TARGET_PROPERTY_NAME), rule2.get(RedirectRule.TARGET_PROPERTY_NAME));
        assertEquals(vm2.get(RedirectRule.NOTE_PROPERTY_NAME), rule2.get(RedirectRule.NOTE_PROPERTY_NAME));
    }

}
