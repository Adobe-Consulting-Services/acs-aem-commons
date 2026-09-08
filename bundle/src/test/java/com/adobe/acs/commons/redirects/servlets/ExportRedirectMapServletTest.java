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

import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import com.adobe.acs.commons.redirects.RedirectResourceBuilder;
import com.adobe.acs.commons.redirects.models.RedirectRule;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;

import static com.adobe.acs.commons.redirects.Asserts.assertDateEquals;
import static com.adobe.acs.commons.redirects.servlets.ExportRedirectMapServlet.CONTENT_TYPE_EXCEL;
import static org.junit.Assert.*;

/**
 * @author Yegor Kozlov
 */
public class ExportRedirectMapServletTest {
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ExportRedirectMapServlet servlet;
    private final String redirectStoragePath = "/conf/acs-commons/redirects";

    @Before
    public void setUp() throws PersistenceException {
        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/content/one")
                .setTarget("/content/two")
                .setStatusCode(302)
                .setUntilDate(new Calendar.Builder().setDate(2022, 9, 9).build())
                .setEffectiveFrom(new Calendar.Builder().setDate(2025, 2, 2).build())
                .setNotes("note-1")
                .setEvaluateURI(true)
                .setContextPrefixIgnored(true)
                .setTagIds(new String[]{"redirects:tag1"})
                .setCreatedBy("john.doe")
                .setModifiedBy("jane.doe")
                .setCreated(new Calendar.Builder().setDate(1974, 1, 16).build())
                .setModified(new Calendar.Builder().setDate(1976, 10, 22).build())
                .build();
        new RedirectResourceBuilder(context, redirectStoragePath)
                .setSource("/content/three")
                .setTarget("/content/four")
                .setStatusCode(301)
                .setTagIds(new String[]{"redirects:tag2"})
                .setModifiedBy("john.doe")
                .build();

        context.request().addRequestParameter("path", redirectStoragePath);
        servlet = new ExportRedirectMapServlet();
    }


    @Test
    public void testGet() throws ServletException, IOException {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(CONTENT_TYPE_EXCEL, response.getContentType());
        // read the generated spreadsheet
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(response.getOutput()));
        assertSpreadsheet(wb);
    }

    @Test
    public void testExport() {
        Resource resource = context.resourceResolver().getResource(redirectStoragePath);
        Collection<RedirectRule> rules = RedirectFilter.getRules(resource);

        Workbook wb = ExportRedirectMapServlet.export(rules);
        assertSpreadsheet(wb);
    }

    public void assertSpreadsheet(Workbook wb) {
        Sheet sheet = wb.getSheet("Redirects");
        assertNotNull(sheet);
        Row row1 = sheet.getRow(1);
        assertEquals("/content/one", row1.getCell(0).getStringCellValue());
        assertEquals("/content/two", row1.getCell(1).getStringCellValue());
        assertEquals(302, (int) row1.getCell(2).getNumericCellValue());
        assertDateEquals("09 October 2022", new Calendar.Builder().setInstant(row1.getCell(3).getDateCellValue()).build());
        assertDateEquals("02 March 2025", new Calendar.Builder().setInstant(row1.getCell(4).getDateCellValue()).build());
        assertEquals("note-1", row1.getCell(5).getStringCellValue());
        assertTrue(row1.getCell(6).getBooleanCellValue());
        assertTrue(row1.getCell(7).getBooleanCellValue());
        assertEquals("redirects:tag1", row1.getCell(8).getStringCellValue());
        assertDateEquals("16 February 1974", new Calendar.Builder().setInstant(row1.getCell(9).getDateCellValue()).build());
        assertEquals("john.doe", row1.getCell(10).getStringCellValue());
        assertDateEquals("22 November 1976", new Calendar.Builder().setInstant(row1.getCell(11).getDateCellValue()).build());
        assertEquals("jane.doe", row1.getCell(12).getStringCellValue());

        Row row2 = sheet.getRow(2);
        assertEquals("/content/three", row2.getCell(0).getStringCellValue());
        assertEquals("/content/four", row2.getCell(1).getStringCellValue());
        assertEquals(301, (int) row2.getCell(2).getNumericCellValue());
        assertFalse(row2.getCell(6).getBooleanCellValue());
        assertFalse(row2.getCell(7).getBooleanCellValue());
        assertEquals("redirects:tag2", row2.getCell(8).getStringCellValue());
        assertEquals("", row2.getCell(10).getStringCellValue());
        assertEquals("john.doe", row2.getCell(12).getStringCellValue());
    }
}