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
package com.adobe.acs.commons.mcp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Various basic tests of the spreadsheet utility class
 */
public class SpreadsheetTest {

    public SpreadsheetTest() {
    }

    static XSSFWorkbook testWorkbook;
    static String[] header = new String[]{"path", "title", "someOtherCol", "int-val@integer", "string-list1@string[]", "string-list2@string[;]",
        "double-val@double", "array", "array", "array", "date-val@date"};
    static ByteArrayOutputStream workbookData = new ByteArrayOutputStream();
    static Date testDate = new Date();

    @BeforeClass
    public static void setUp() throws IOException {
        testWorkbook = new XSSFWorkbook();
        XSSFSheet sheet = testWorkbook.createSheet("sheet 1");
        createRow(sheet, header);
        createRow(sheet, "/test/a1", "A-1");
        createRow(sheet, "/test/a2", "A-2", "val");
        createRow(sheet, "/test/a1/a1a", "A-1-A", "val");
        createRow(sheet, "/test/a3/a3a", "A-3-A", "val");
        XSSFRow valuesRow = createRow(sheet, "/some/types", "Types", "...", "12345", "one,two,three", "four;five;six", 
                "12.345", "One Value", null, "Another Value");
        XSSFCell dateCell = valuesRow.createCell(10);
        dateCell.setCellValue(testDate);
        testWorkbook.write(workbookData);
        workbookData.close();
    }

    /**
     * Test of getFileName method, of class Spreadsheet.
     */
    @Test
    public void testGetFileName() throws IOException {
        Spreadsheet instance = new Spreadsheet(new ByteArrayInputStream(workbookData.toByteArray()));
        String expResult = "unknown";
        String result = instance.getFileName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getRowCount method, of class Spreadsheet.
     */
    @Test
    public void testGetRowCount() throws IOException {
        Spreadsheet instance = new Spreadsheet(new ByteArrayInputStream(workbookData.toByteArray()));
        int expResult = 5;
        int result = instance.getRowCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHeaderRow method, of class Spreadsheet.
     */
    @Test
    public void testGetHeaderRow() throws IOException {
        Spreadsheet instance = new Spreadsheet(false, new ByteArrayInputStream(workbookData.toByteArray()));
        List<String> expResult = Arrays.asList(header);
        List<String> result = instance.getHeaderRow();
        assertTrue("Header row should match", result.containsAll(expResult));
    }

    /**
     * Test of getDataRows method, of class Spreadsheet.
     */
    @Test
    public void testGetDataRows() throws IOException {
        Spreadsheet instance = new Spreadsheet(new ByteArrayInputStream(workbookData.toByteArray()));
        List<Map<String, CompositeVariant>> result = instance.getDataRows();
        assertEquals("/test/a1", result.get(0).get("path").toPropertyValue());
        assertEquals("/test/a3/a3a", result.get(3).get("path").toString());
    }

    /**
     * Test of getRequiredColumns method, of class Spreadsheet.
     */
    @Test
    public void testRequiredColumnsNoConversion() throws IOException {
        Spreadsheet instance = new Spreadsheet(false, new ByteArrayInputStream(workbookData.toByteArray()), "someOtherCol");
        List<String> required = instance.getRequiredColumns();
        assertEquals("someOtherCol", required.get(0));
        List<Map<String, CompositeVariant>> result = instance.getDataRows();
        assertEquals("/test/a2", result.get(0).get("path").toString());
    }

    /**
     * Test of getRequiredColumns method, of class Spreadsheet.
     */
    @Test
    public void testRequiredColumnsWithConversion() throws IOException {
        Spreadsheet instance = new Spreadsheet(true, new ByteArrayInputStream(workbookData.toByteArray()), "someOtherCol");
        List<String> required = instance.getRequiredColumns();
        assertEquals("someothercol", required.get(0));
        List<Map<String, CompositeVariant>> result = instance.getDataRows();
        assertEquals("/test/a2", result.get(0).get("path").toString());
    }

    @Test
    public void testVariantTypes() throws IOException {
        Spreadsheet instance = new Spreadsheet(true, new ByteArrayInputStream(workbookData.toByteArray()));
        Map<String, CompositeVariant> values = instance.getDataRows().get(4);
        assertEquals((Integer) 12345, values.get("int-val").toPropertyValue());
        assertArrayEquals(new String[]{"one", "two", "three"}, (Object[]) values.get("string-list1").toPropertyValue());
        assertArrayEquals(new String[]{"four", "five", "six"}, (Object[]) values.get("string-list2").toPropertyValue());
        assertArrayEquals(new String[]{"One Value", "Another Value"}, (Object[]) values.get("array").toPropertyValue());
        assertEquals(12.345, (Double) values.get("double-val").toPropertyValue(), 0.000001);
        assertEquals(testDate, values.get("date-val").toPropertyValue());
    }

    private static XSSFRow createRow(XSSFSheet sheet, String... values) {
        int rowNum = sheet.getPhysicalNumberOfRows();
        XSSFRow row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                row.createCell(i);
            } else {
                row.createCell(i).setCellValue(values[i]);
            }
        }
        return row;
    }
}