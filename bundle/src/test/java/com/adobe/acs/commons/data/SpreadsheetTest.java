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
package com.adobe.acs.commons.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
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

    private static List<String> CASE_INSENSITIVE_HEADERS = Arrays.asList("Source", "Rendition", "Target",
                                                                               "Original");
    static XSSFWorkbook testWorkbook;
    static String[] header = new String[]{"path", "title", "someOtherCol", "int-val@integer", "string-list1@string[]", "string-list2@string[;]",
        "double-val@double", "array", "array", "array", "date-val@date"};
    static String[] headerNames = new String[]{"path", "title", "someOtherCol", "int-val", "string-list1", "string-list2",
        "double-val", "array", "array", "array", "date-val"};
    static ByteArrayOutputStream workbookData = new ByteArrayOutputStream();
    static Calendar testDate = Calendar.getInstance();
    static Spreadsheet dataTypesSheet;

    @BeforeClass
    public static void setUp() throws IOException {
        testWorkbook = new XSSFWorkbook();
        XSSFSheet sheet = testWorkbook.createSheet("sheet 1");
        createRow(sheet, header);
        createRow(sheet, "/test/a1", "A-1");
        createRow(sheet, "/test/a2", "A-2", "val");
        createRow(sheet, "/test/a1/a1a", "A-1-A", "val");
        createRow(sheet, "/test/a3/a3a", "A-3-A", "val");
        createRow(sheet, null, null, null);
        XSSFRow valuesRow = createRow(sheet, "/some/types", "Types", "...", "12345", "one,two,three", "four;five;six",
                "12.345", "One Value", null, "Another Value");
        XSSFCell dateCell = valuesRow.createCell(10);
        dateCell.setCellValue(testDate);
        CellStyle dateStyle = testWorkbook.createCellStyle();
        dateStyle.setDataFormat(testWorkbook.createDataFormat().getFormat("YYYY-mm-dd"));
        dateCell.setCellStyle(dateStyle);
        testWorkbook.write(workbookData);
        workbookData.close();

        InputStream dataTypesFile = SpreadsheetTest.class.getResourceAsStream("/com/adobe/acs/commons/data/spreadsheet-data-types.xlsx");
        dataTypesSheet = new Spreadsheet(false, dataTypesFile).buildSpreadsheet(Locale.US);
    }

    /**
     * Test of getFileName method, of class Spreadsheet.
     */
    @Test
    public void testGetFileName() throws IOException {
        Spreadsheet instance = new Spreadsheet(new ByteArrayInputStream(workbookData.toByteArray())).buildSpreadsheet();
        String expResult = "unknown";
        String result = instance.getFileName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getRowCount method, of class Spreadsheet.
     */
    @Test
    public void testGetRowCount() throws IOException {
        Spreadsheet instance = new Spreadsheet(new ByteArrayInputStream(workbookData.toByteArray())).buildSpreadsheet();
        int expResult = 6;
        int result = instance.getRowCount();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHeaderRow method, of class Spreadsheet.
     */
    @Test
    public void testGetHeaderRow() throws IOException {
        Spreadsheet instance = new Spreadsheet(false, new ByteArrayInputStream(workbookData.toByteArray()))
                .buildSpreadsheet();
        List<String> expResult = Arrays.asList(headerNames);
        List<String> result = instance.getHeaderRow();
        assertTrue("Header row should match", result.containsAll(expResult));
    }

    /**
     * Test of getDataRows method, of class Spreadsheet.
     */
    @Test
    public void testGetDataRows() throws IOException {
        Spreadsheet instance = new Spreadsheet(new ByteArrayInputStream(workbookData.toByteArray())).buildSpreadsheet();
        List<Map<String, CompositeVariant>> result = instance.getDataRowsAsCompositeVariants();
        assertEquals("/test/a1", result.get(0).get("path").toPropertyValue());
        assertEquals("/test/a3/a3a", result.get(3).get("path").toString());
    }

    /**
     * Test of getRequiredColumns method, of class Spreadsheet.
     */
    @Test
    public void testRequiredColumnsNoConversion() throws IOException {
        Spreadsheet instance = new Spreadsheet(false, new ByteArrayInputStream(workbookData.toByteArray()), "someOtherCol")
                .buildSpreadsheet();
        List<String> required = instance.getRequiredColumns();
        assertEquals("someOtherCol", required.get(0));
        List<Map<String, CompositeVariant>> result = instance.getDataRowsAsCompositeVariants();
        assertEquals("/test/a2", result.get(0).get("path").toString());
    }

    /**
     * Test of getRequiredColumns method, of class Spreadsheet.
     */
    @Test
    public void testRequiredColumnsWithConversion() throws IOException {
        Spreadsheet instance = new Spreadsheet(true, new ByteArrayInputStream(workbookData.toByteArray()), "someOtherCol")
                .buildSpreadsheet();
        List<String> required = instance.getRequiredColumns();
        assertEquals("someothercol", required.get(0));
        List<Map<String, CompositeVariant>> result = instance.getDataRowsAsCompositeVariants();
        assertEquals("/test/a2", result.get(0).get("path").toString());
    }

    @Test
    public void testVariantTypes() throws IOException {
        Spreadsheet instance = new Spreadsheet(true, new ByteArrayInputStream(workbookData.toByteArray()))
                .buildSpreadsheet();
        Map<String, CompositeVariant> values = instance.getDataRowsAsCompositeVariants().get(4);
        assertNotNull("Should have a row of data", values);
        assertNotNull("Should have a column int-val", values.get("int-val"));
        assertEquals((Integer) 12345, values.get("int-val").toPropertyValue());
        assertArrayEquals(new String[]{"one", "two", "three"}, (Object[]) values.get("string-list1").toPropertyValue());
        assertArrayEquals(new String[]{"four", "five", "six"}, (Object[]) values.get("string-list2").toPropertyValue());
        assertArrayEquals(new String[]{"One Value", "Another Value"}, (Object[]) values.get("array").toPropertyValue());
        assertEquals(12.345, (Double) values.get("double-val").toPropertyValue(), 0.000001);
        assertEquals(testDate, values.get("date-val").toPropertyValue());
    }

    @Test
    public void testSheetTypesAsStrings() {
        assertEquals(2, dataTypesSheet.getRowCount());
        for (int i = 0; i < dataTypesSheet.getRowCount(); i++) {
            Map<String, CompositeVariant> row = dataTypesSheet.getDataRowsAsCompositeVariants().get(i);
            assertEquals("123", row.get("Integer").toString());
            assertEquals("123", row.get("Integer string").toString());
            assertEquals("123.456", row.get("Floating point").toString());
            assertEquals("123.456", row.get("Floating point string").toString());
            assertEquals("11/26/85", row.get("Short date").toString());
            assertEquals("Tuesday, November 26, 1985", row.get("Long date").toString());
            assertEquals("9:00:00 AM", row.get("Time").toString());
            assertEquals("110.00%", row.get("Percent").toString());
            assertEquals("This is just a regular string", row.get("String").toString());
            assertEquals("1/1/2000", row.get("date1").toString());
            assertEquals("1/1/00 12:00 AM", row.get("date2").toString());
            assertEquals("Saturday, January 01, 2000", row.get("date3").toString());
            assertEquals("2000-01-01T14:47:41.922-05:00", row.get("date4").toString());
        }
    }

    @Test
    public void testSheetTypesAsNativeValues() {
        Calendar flux = Calendar.getInstance();
        flux.set(Calendar.YEAR, 1985);
        flux.set(Calendar.MONTH, Calendar.NOVEMBER);
        flux.set(Calendar.DAY_OF_MONTH, 26);
        flux.set(Calendar.HOUR_OF_DAY, 0);
        flux.set(Calendar.MINUTE, 0);
        flux.set(Calendar.SECOND, 0);
        flux.set(Calendar.MILLISECOND, 0);
        Date fluxCapacitorBirthday = flux.getTime();
        // Note time formatted cells are always relative to Dec 31, 1899 in Excel.
        Calendar timeCal = Calendar.getInstance();
        timeCal.set(Calendar.YEAR, 1899);
        timeCal.set(Calendar.MONTH, Calendar.DECEMBER);
        timeCal.set(Calendar.DAY_OF_MONTH, 31);
        timeCal.set(Calendar.HOUR_OF_DAY, 9);
        timeCal.set(Calendar.MINUTE, 0);
        timeCal.set(Calendar.SECOND, 0);
        timeCal.set(Calendar.MILLISECOND, 0);
        Date someTime = timeCal.getTime();

        assertEquals(2, dataTypesSheet.getRowCount());
        for (int i = 0; i < dataTypesSheet.getRowCount(); i++) {
            Map<String, CompositeVariant> row = dataTypesSheet.getDataRowsAsCompositeVariants().get(i);
            assertEquals(123, row.get("Integer").getValueAs(Integer.class));
            assertEquals(123, row.get("Integer string").getValueAs(Integer.class));
            assertEquals(123.456, (double) row.get("Floating point").getValueAs(Double.class), 0.0001);
            assertEquals(123.456, (double) row.get("Floating point string").getValueAs(Double.class), 0.0001);
            assertEquals(fluxCapacitorBirthday, row.get("Short date").getValueAs(Date.class));
            assertEquals(fluxCapacitorBirthday, row.get("Long date").getValueAs(Date.class));
            assertEquals(someTime, row.get("Time").getValueAs(Date.class));
            assertEquals(1.1, (double) row.get("Percent").getValueAs(Double.class), 0.0001);
            assertEquals("This is just a regular string", row.get("String").toPropertyValue());

            // According to JCR 2.0 spec 3.6.1.8, Dates are stored as Calendar objects
            Calendar date1 = (Calendar) row.get("date1").toPropertyValue();
            assertEquals(Calendar.JANUARY, date1.get(Calendar.MONTH));
            assertEquals(2000L, date1.get(Calendar.YEAR));
            Calendar date2 = (Calendar) row.get("date2").toPropertyValue();
            assertEquals(Calendar.JANUARY, date2.get(Calendar.MONTH));
            assertEquals(2000L, date2.get(Calendar.YEAR));
            Calendar date3 = (Calendar) row.get("date3").toPropertyValue();
            assertEquals(Calendar.JANUARY, date3.get(Calendar.MONTH));
            assertEquals(2000L, date3.get(Calendar.YEAR));
            Calendar date4 = (Calendar) row.get("date4").toPropertyValue();
            assertEquals(Calendar.JANUARY, date4.get(Calendar.MONTH));
            assertEquals(2000L, date4.get(Calendar.YEAR));
        }
    }

    @Test
    public void testHeadersWithCaseInsensitivityList() {
        Spreadsheet spreadsheet = new Spreadsheet(true, CASE_INSENSITIVE_HEADERS);

        assertEquals("source", spreadsheet.convertHeaderName("SoUrCe"));
        assertEquals("rendition", spreadsheet.convertHeaderName("RenDiTion"));
        assertEquals("target", spreadsheet.convertHeaderName("TarGeT"));
        assertEquals("original", spreadsheet.convertHeaderName("OrIgInal"));
        assertEquals("camelCaseProperty", spreadsheet.convertHeaderName("camelCaseProperty"));
    }

    @Test
    public void testHeadersWithoutCaseInsensitivityList() {
        Spreadsheet spreadsheet = new Spreadsheet(true);

        assertEquals("source", spreadsheet.convertHeaderName("SoUrCe"));
        assertEquals("rendition", spreadsheet.convertHeaderName("RenDiTion"));
        assertEquals("target", spreadsheet.convertHeaderName("TarGeT"));
        assertEquals("original", spreadsheet.convertHeaderName("OrIgInal"));
        assertEquals("test:camelcase", spreadsheet.convertHeaderName("test:camelCase"));
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
