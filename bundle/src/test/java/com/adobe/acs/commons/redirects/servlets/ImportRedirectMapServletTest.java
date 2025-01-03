/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.RedirectResourceBuilder;
import com.adobe.acs.commons.redirects.models.ExportColumn;
import com.adobe.acs.commons.redirects.models.ImportLog;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import java.util.Collections;

import static com.adobe.acs.commons.redirects.Asserts.assertDateEquals;
import static com.adobe.acs.commons.redirects.models.RedirectRule.CASE_INSENSITIVE_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.RedirectRule.NOTE_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.RedirectRule.SOURCE_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.RedirectRule.TAGS_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.RedirectRule.TARGET_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.servlets.ExportRedirectMapServlet.CONTENT_TYPE_EXCEL;
import static com.adobe.acs.commons.redirects.servlets.ExportRedirectMapServlet.export;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class ImportRedirectMapServletTest {
    @Rule
    public SlingContext context = new SlingContext(
            ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ImportRedirectMapServlet servlet;
    private String redirectStoragePath = "/conf/acs-commons/redirects";
    private Resource storageRoot;

    @Before
    public void setUp() {
        servlet = new ImportRedirectMapServlet();
        context.request().addRequestParameter("path", redirectStoragePath);
        context.addModelsForClasses(RedirectRule.class);
        storageRoot = context.create().resource(redirectStoragePath);
    }

    /**
     * Import a spreadsheet with ony 3 required columns:
     *   - Cell A - source
     *   - Cell B - target
     *   - Cell C - statusCode
     */
    @Test
    public void testImportOnlyRequiredColumns() throws ServletException, IOException {

        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(
                wb.createDataFormat().getFormat("mmm d, yyyy"));
        Sheet sheet = wb.createSheet();
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(ExportColumn.SOURCE.getTitle());
        headerRow.createCell(1).setCellValue(ExportColumn.TARGET.getTitle());
        headerRow.createCell(2).setCellValue(ExportColumn.STATUS_CODE.getTitle());

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("/content/1");
        row1.createCell(1).setCellValue("/en/we-retail");
        row1.createCell(2).setCellValue(301);

        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("/content/2");
        row2.createCell(1).setCellValue("/en/we-retail");
        row2.createCell(2).setCellValue(302);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        byte[] excelBytes = out.toByteArray();

        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        // Execute
        servlet.doPost(context.request(), context.response());

        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 2, rules.size());

        RedirectRule rule1 = rules.get("/content/1").adaptTo(RedirectRule.class);
        assertEquals("/content/1", rule1.getSource());
        assertEquals("/en/we-retail", rule1.getTarget());
        assertEquals(301, rule1.getStatusCode());

        RedirectRule rule2 = rules.get("/content/2").adaptTo(RedirectRule.class);
        assertEquals("/content/2", rule2.getSource());
        assertEquals("/en/we-retail", rule2.getTarget());
        assertEquals(302, rule2.getStatusCode());

        // read ImportLog from the output json and assert there were no issues
        ImportLog importLog = new ObjectMapper().readValue(context.response().getOutputAsString(), ImportLog.class);
        assertEquals("ImportLog",0, importLog.getLog().size());
        assertTrue(importLog.getPath(), context.resourceResolver().getResource(importLog.getPath()) != null);
    }

    /**
     * Rows that don't have valid source/target/statusCode should be skipped
     */
    @Test
    public void testIgnoreInvalidRows() throws ServletException, IOException {

        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(
                wb.createDataFormat().getFormat("mmm d, yyyy"));
        Sheet sheet = wb.createSheet();
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(ExportColumn.SOURCE.getTitle());
        headerRow.createCell(1).setCellValue(ExportColumn.TARGET.getTitle());
        headerRow.createCell(2).setCellValue(ExportColumn.STATUS_CODE.getTitle());

        Row row1 = sheet.createRow(1);
        //row1.createCell(0).setCellValue("/content/1");
        row1.createCell(1).setCellValue("/en/we-retail");
        row1.createCell(2).setCellValue(301);

        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("/content/1");
        //row2.createCell(1).setCellValue("/en/we-retail");
        row2.createCell(2).setCellValue(301);

        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("/content/1");
        row3.createCell(1).setCellValue("/en/we-retail");
        //row3.createCell(2).setCellValue(301);

        Row row4 = sheet.createRow(4);
        row4.createCell(0).setCellValue(true);
        row4.createCell(1).setCellValue("/en/we-retail");
        row4.createCell(2).setCellValue(301);

        Row row5 = sheet.createRow(5);
        row5.createCell(0).setCellValue("/content/1");
        row5.createCell(1).setCellValue(true);
        row5.createCell(2).setCellValue(301);

        Row row6 = sheet.createRow(6);
        row6.createCell(0).setCellValue("/content/1");
        row6.createCell(1).setCellValue("/en/we-retail");
        row6.createCell(2).setCellValue(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        byte[] excelBytes = out.toByteArray();

        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        // Execute
        servlet.doPost(context.request(), context.response());

        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 0, rules.size());

        // read ImportLog from the output json and assert that every 6 input rows had issues
        ImportLog importLog = new ObjectMapper().readValue(context.response().getOutputAsString(), ImportLog.class);
        List<ImportLog.Entry> logEntries = importLog.getLog();
        assertEquals(6, logEntries.size());
        assertTrue(importLog.getPath(), context.resourceResolver().getResource(importLog.getPath()) != null);
    }

    /**
     * redirects in the input spreadsheet are keyed by source path. Dupes will be collapsed.
     */
    @Test
    public void testDuplicatesRedirects() throws ServletException, IOException {

        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(
                wb.createDataFormat().getFormat("mmm d, yyyy"));
        Sheet sheet = wb.createSheet();
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(ExportColumn.SOURCE.getTitle());
        headerRow.createCell(1).setCellValue(ExportColumn.TARGET.getTitle());
        headerRow.createCell(2).setCellValue(ExportColumn.STATUS_CODE.getTitle());

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("/en/we-retail/about");
        row1.createCell(1).setCellValue("/en/we-retail");
        row1.createCell(2).setCellValue(302);

        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("/en/we-retail/about");
        row2.createCell(1).setCellValue("/en/we-retail/contact-us");
        row2.createCell(2).setCellValue(302);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        byte[] excelBytes = out.toByteArray();

        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        // Execute
        servlet.doPost(context.request(), context.response());

        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 1, rules.size());

        // read ImportLog from the output json and assert there were no issues
        ImportLog importLog = new ObjectMapper().readValue(context.response().getOutputAsString(), ImportLog.class);
        assertEquals("ImportLog",0, importLog.getLog().size());
        assertTrue(importLog.getPath(), context.resourceResolver().getResource(importLog.getPath()) != null);
    }

    /**
     * User can change the order of optional columns (starting with D), e.g.
     *   - Cell D - Evaluate URI
     *   - Cell E - Notes
     *   ...
     */
    @Test
    public void testImportMixedColumns() throws ServletException, IOException {

        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(
                wb.createDataFormat().getFormat("mmm d, yyyy"));
        Sheet sheet = wb.createSheet();
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(ExportColumn.SOURCE.getTitle());
        headerRow.createCell(1).setCellValue(ExportColumn.TARGET.getTitle());
        headerRow.createCell(2).setCellValue(ExportColumn.STATUS_CODE.getTitle());
        headerRow.createCell(3).setCellValue(ExportColumn.EVALUATE_URI.getTitle());
        headerRow.createCell(4).setCellValue(ExportColumn.OFF_TIME.getTitle());
        headerRow.createCell(5).setCellValue(ExportColumn.ON_TIME.getTitle());
        headerRow.createCell(6).setCellValue(ExportColumn.NOTES.getTitle());
        headerRow.createCell(7).setCellValue(ExportColumn.IGNORE_CONTEXT_PREFIX.getTitle());
        headerRow.createCell(8).setCellValue(ExportColumn.TAGS.getTitle());

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("/content/1");
        row1.createCell(1).setCellValue("/en/we-retail");
        row1.createCell(2).setCellValue(301);
        row1.createCell(3).setCellValue(true);
        row1.createCell(4).setCellValue(new Calendar.Builder().setDate(1974, 01, 16).build());
        row1.getCell(4).setCellStyle(dateStyle);
        row1.createCell(5).setCellValue(new Calendar.Builder().setDate(2025, 02, 02).build());
        row1.getCell(5).setCellStyle(dateStyle);
        row1.createCell(6).setCellValue("note-abc");
        row1.createCell(7).setCellValue(true);
        row1.createCell(8).setCellValue("redirects:tag1\nredirects:tag2");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        byte[] excelBytes = out.toByteArray();

        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        // Execute
        servlet.doPost(context.request(), context.response());

        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 1, rules.size());

        RedirectRule rule = rules.get("/content/1").adaptTo(RedirectRule.class);
        assertEquals("/en/we-retail", rule.getTarget());
        assertEquals(301, rule.getStatusCode());
        assertDateEquals("16 February 1974", rule.getUntilDate());
        assertDateEquals("02 March 2025", rule.getEffectiveFrom());
        assertEquals("note-abc", rule.getNote());
        assertTrue(rule.getEvaluateURI());
        assertTrue(rule.getContextPrefixIgnored());
        assertArrayEquals(new String[]{"redirects:tag1", "redirects:tag2"}, rule.getTagIds());

        // read ImportLog from the output json and assert there were no issues
        ImportLog importLog = new ObjectMapper().readValue(context.response().getOutputAsString(), ImportLog.class);
        assertEquals("ImportLog",0, importLog.getLog().size());
        assertTrue(importLog.getPath(), context.resourceResolver().getResource(importLog.getPath()) != null);
    }

    @Test
    public void testExcelImport() throws ServletException, IOException {
        // Setup mock Excel file
        byte[] excelBytes = createMockExcelFile();
        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        assertEquals("number of redirects before import ", 0, servlet.getRules(storageRoot).size());

        // Execute
        servlet.doPost(context.request(), context.response());

        // Verify
        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 1, rules.size());
        assertNotNull(rules.get("/old"));
    }

    @Test
    public void testReplaceExistingRules() throws ServletException, IOException {
        // Setup
        byte[] excelBytes = createMockExcelFile();
        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );
        context.request().addRequestParameter("replace", "true");

        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/content/one")
                .setTarget("/content/two")
                .setStatusCode(302)
                .build();

        assertEquals("number of redirects before import ", 1, servlet.getRules(storageRoot).size());

        // Execute
        servlet.doPost(context.request(), context.response());

        // Verify
        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 1, rules.size());
        assertNotNull(rules.get("/old"));
    }

    @Test
    public void testMergeExistingRules() throws ServletException, IOException {
        // Setup
        byte[] excelBytes = createMockExcelFile();
        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/content/one")
                .setTarget("/content/two")
                .setStatusCode(302)
                .build();
        assertEquals("number of redirects before import ", 1, servlet.getRules(storageRoot).size());

        // Execute
        servlet.doPost(context.request(), context.response());

        // Verify
        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 2, rules.size());
        assertNotNull(rules.get("/old"));
        assertNotNull(rules.get("/content/one"));
    }

    @Test
    public void testUpdateExistingRule() throws ServletException, IOException {
        // Setup
        byte[] excelBytes = createMockExcelFile();
        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        // Setup existing rule
        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/old")
                .setTarget("/content/two")
                .setNotes("hello")
                .setCaseInsensitive(true)
                .setTagIds(new String[]{"tag:one"})
                .build();

        assertEquals("number of redirects before import ", 1, servlet.getRules(storageRoot).size());

        // Execute
        servlet.doPost(context.request(), context.response());

        // Verify
        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 1, rules.size());
        Resource rule = rules.get("/old");
        assertEquals("/old", rule.getValueMap().get(SOURCE_PROPERTY_NAME));
        assertEquals("/new", rule.getValueMap().get(TARGET_PROPERTY_NAME));
        assertEquals("hello", rule.getValueMap().get(NOTE_PROPERTY_NAME));
        assertEquals(true, rule.getValueMap().get(CASE_INSENSITIVE_PROPERTY_NAME));
        assertArrayEquals(new String[]{"tag:one"}, (String[])rule.getValueMap().get(TAGS_PROPERTY_NAME));
    }

    @Test
    public void testShardingLargeImport() throws ServletException, IOException {
        // Setup large Excel with over 1000 rules
        List<RedirectRule> xlsRules = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            RedirectRule rule = spy(new RedirectRule());
            doReturn("/old-" + i).when(rule).getSource();
            doReturn("/new-" + i).when(rule).getTarget();
            doReturn(301).when(rule).getStatusCode();
            doReturn(null).when(rule).getCreatedBy();
            doReturn(null).when(rule).getModifiedBy();
            xlsRules.add(rule);
        }
        byte[] excelBytes = createMockExcelFile(xlsRules);
        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        assertEquals("number of redirects before import ", 0, servlet.getRules(storageRoot).size());

        // Execute
        servlet.doPost(context.request(), context.response());

        // Verify
        Iterator<Resource> it = storageRoot.listChildren();
        assertEquals("shard-0", it.next().getName());
        assertEquals("shard-1", it.next().getName());
        assertFalse(it.hasNext());

        Map<String, Resource> rules = servlet.getRules(storageRoot); // rules keyed by source
        assertEquals("number of redirects after import ", 1500, rules.size());
    }

    @Test
    public void testImportWithValidation() throws ServletException, IOException {
        // Setup CSV with invalid rule
        RedirectRule rule = spy(new RedirectRule());
        doReturn("").when(rule).getSource();
        doReturn("/new").when(rule).getTarget();
        doReturn(301).when(rule).getStatusCode();
        doReturn(null).when(rule).getCreatedBy();
        doReturn(null).when(rule).getModifiedBy();

        byte[] excelBytes = createMockExcelFile(Collections.singletonList(rule));
        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        // Execute
        servlet.doPost(context.request(), context.response());

        // Verify
        String jsonResponse = context.response().getOutputAsString();
        ImportLog auditLog = new ObjectMapper().readValue(jsonResponse, ImportLog.class);
        ImportLog.Entry entry = auditLog.getLog().get(0); // Should contain validation warning
        assertEquals("A2", entry.getCell());
        assertEquals(ImportLog.Level.WARN, entry.getLevel());
        assertEquals("Cell A is required and should contain redirect source", entry.getMsg());
    }

    @Test
    public void testAuditLogCreation() throws ServletException, IOException {
        // Setup
        byte[] excelBytes = createMockExcelFile();
        setupFileUpload(excelBytes, CONTENT_TYPE_EXCEL );

        // Execute
        servlet.doPost(context.request(), context.response());

        // Verify
        String jsonResponse = context.response().getOutputAsString();
        String auditLogPath = new ObjectMapper().readValue(jsonResponse, ImportLog.class).getPath();
        Resource auditResource = context.resourceResolver().getResource(auditLogPath);
        assertNotNull(auditResource); // Should contain audit log path
    }

    private void setupFileUpload(byte[] content, String contentType) throws IOException {
        context.request().addRequestParameter("file", content, contentType);
    }

    private byte[] createMockExcelFile() throws IOException {
        RedirectRule rule = spy(new RedirectRule());
        doReturn("/old").when(rule).getSource();
        doReturn("/new").when(rule).getTarget();
        doReturn(301).when(rule).getStatusCode();
        doReturn(null).when(rule).getCreatedBy();
        doReturn(null).when(rule).getModifiedBy();

        Workbook wb = export(Collections.singletonList(rule));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        return out.toByteArray();
    }

    private byte[] createMockExcelFile(List<RedirectRule> rules) throws IOException {
        Workbook wb = export(rules);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        out.close();
        return out.toByteArray();
    }
}
