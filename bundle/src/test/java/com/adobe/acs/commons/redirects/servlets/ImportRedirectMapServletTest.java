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

import com.adobe.acs.commons.redirects.RedirectResourceBuilder;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
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
        // existing rules
        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/content/one")
                .setTarget("/content/two")
                .setStatusCode(302)
                .setUntilDate(new Calendar.Builder().setDate(2022, 9, 9).build())
                .setNotes("note-1")
                .setEvaluateURI(true)
                .setContextPrefixIgnored(true)
                .setCreatedBy("john.doe")
                .setTagIds(new String[]{"redirects:tag3"})
                .setProperty("custom-1", "123")
                .setNodeName("redirect-saved-1")
                .build();
        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/content/three")
                .setTarget("/content/four")
                .setStatusCode(301)
                .setNotes("note-2")
                .setModifiedBy("xyz")
                .setTagIds(new String[]{"redirects:tag3"})
                .setProperty("custom-2", "345")
                .setNodeName("redirect-saved-2")
                .build();

        // new rules in a spreadsheet. will be merged with the existing rules
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(
                wb.createDataFormat().getFormat("mmm d, yyyy"));
        Sheet sheet = wb.createSheet();
        sheet.createRow(0); //header.not used in import, can be all blank
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("/content/1");
        row1.createCell(1).setCellValue("/en/we-retail");
        row1.createCell(2).setCellValue(301);
        row1.createCell(3).setCellValue(new Calendar.Builder().setDate(1974, 01, 16).build());
        row1.getCell(3).setCellStyle(dateStyle);
        row1.createCell(4).setCellValue("note-abc");
        row1.createCell(7).setCellValue("redirects:tag1\nredirects:tag2");
        row1.createCell(12).setCellValue(new Calendar.Builder().setDate(2025, 02, 02).build());
        row1.getCell(12).setCellStyle(dateStyle);

        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("/content/2");
        row2.createCell(1).setCellValue("/en/we-retail");
        row2.createCell(2).setCellValue(301);

        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("/content/three");
        row3.createCell(1).setCellValue("/en/we-retail");
        row3.createCell(2).setCellValue(301);

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
        assertEquals("john.doe", rule1.getCreatedBy());
        assertEquals("123", res1.getValueMap().get("custom-1"));
        assertTrue(rule1.getEvaluateURI());
        assertTrue(rule1.getContextPrefixIgnored());
        assertArrayEquals(new String[]{"redirects:tag3"}, rule1.getTagIds());

        Resource res2 = rules.get("/content/three");
        assertEquals("redirect-saved-2", res2.getName()); // node name is preserved
        RedirectRule rule2 = res2.adaptTo(RedirectRule.class);
        assertEquals("/en/we-retail", rule2.getTarget());
        assertEquals(301, rule2.getStatusCode());
        assertFalse(rule2.getEvaluateURI());
        assertFalse(rule2.getContextPrefixIgnored());
        assertEquals("xyz", rule2.getModifiedBy());
        assertEquals("345", res2.getValueMap().get("custom-2"));

        RedirectRule rule3 = rules.get("/content/1").adaptTo(RedirectRule.class);
        assertEquals("/en/we-retail", rule3.getTarget());
        assertDateEquals("16 February 1974", rule3.getUntilDate());
        assertEquals("note-abc", rule3.getNote());
        assertFalse(rule3.getEvaluateURI());
        assertFalse(rule3.getContextPrefixIgnored());
        assertArrayEquals(new String[]{"redirects:tag1", "redirects:tag2"}, rule3.getTagIds());
        assertDateEquals("02 March 2025", rule3.getEffectiveFrom());

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
        rule2.put(RedirectRule.EVALUATE_URI_PROPERTY_NAME, true);
        rule2.put(RedirectRule.CONTEXT_PREFIX_IGNORED_PROPERTY_NAME, true);

        Collection<Map<String, Object>> rules = Arrays.asList(rule1, rule2);

        Resource root = context.resourceResolver().getResource(redirectStoragePath);
        servlet.update(root, rules, Collections.emptyMap());

        Map<String, Resource> redirects = servlet.getRules(root);
        ValueMap vm1 = redirects.get(rule1.get(RedirectRule.SOURCE_PROPERTY_NAME)).getValueMap();

        assertEquals(vm1.get(RedirectRule.SOURCE_PROPERTY_NAME), rule1.get(RedirectRule.SOURCE_PROPERTY_NAME));
        assertEquals(vm1.get(RedirectRule.TARGET_PROPERTY_NAME), rule1.get(RedirectRule.TARGET_PROPERTY_NAME));
        assertFalse(vm1.containsKey(RedirectRule.UNTIL_DATE_PROPERTY_NAME));
        assertFalse(vm1.containsKey(RedirectRule.NOTE_PROPERTY_NAME));
        assertFalse(vm1.containsKey(RedirectRule.EVALUATE_URI_PROPERTY_NAME));
        assertFalse(vm1.containsKey(RedirectRule.CONTEXT_PREFIX_IGNORED_PROPERTY_NAME));

        ValueMap vm2 = redirects.get(rule2.get(RedirectRule.SOURCE_PROPERTY_NAME)).getValueMap();
        assertEquals(vm2.get(RedirectRule.SOURCE_PROPERTY_NAME), rule2.get(RedirectRule.SOURCE_PROPERTY_NAME));
        assertEquals(vm2.get(RedirectRule.TARGET_PROPERTY_NAME), rule2.get(RedirectRule.TARGET_PROPERTY_NAME));
        assertEquals(vm2.get(RedirectRule.NOTE_PROPERTY_NAME), rule2.get(RedirectRule.NOTE_PROPERTY_NAME));
        assertEquals(vm2.get(RedirectRule.EVALUATE_URI_PROPERTY_NAME), rule2.get(RedirectRule.EVALUATE_URI_PROPERTY_NAME));
        assertEquals(vm2.get(RedirectRule.CONTEXT_PREFIX_IGNORED_PROPERTY_NAME), rule2.get(RedirectRule.CONTEXT_PREFIX_IGNORED_PROPERTY_NAME));
    }

}
