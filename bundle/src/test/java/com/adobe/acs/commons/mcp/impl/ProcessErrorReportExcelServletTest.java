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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.model.ArchivedProcessFailure;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic code coverage for the error report servlet
 */
public class ProcessErrorReportExcelServletTest {

    public ProcessErrorReportExcelServletTest() {
    }

    ManagedProcess process;
    ProcessErrorReportExcelServlet servlet;
    List<ArchivedProcessFailure> failures;

    @Before
    public void setUp() {
        servlet = new ProcessErrorReportExcelServlet();
        failures = new ArrayList<>();
        process = mock(ManagedProcess.class);
        when(process.getReportedErrorsList()).thenReturn(failures);
        when(process.getName()).thenReturn("Test Report");
    }

    private void addFailure(String path, String error, String stacktrace) {
        ArchivedProcessFailure fail = new ArchivedProcessFailure();
        fail.error = error;
        fail.nodePath = path;
        fail.time = new Date();
        fail.stackTrace = stacktrace;
        failures.add(fail);
    }

    /**
     * Generate a worksheet, just confirming that there are no blatant runtime errors and the data was recorded in the output
     */
    @Test
    public void createSpreadsheet() throws Exception {
        final int overB2Width = 256 * 20;
        final int overC3Width = 256 * 121;
        addFailure("path1", "error1", "stacktrace1");
        addFailure("path2", repeatUntilMinLengthReached("error2", overB2Width), "stacktrace2");
        addFailure("path3", "error3", repeatUntilMinLengthReached("stacktrace3", overC3Width));
        addFailure("path4", "error4", "stacktrace4");
        Workbook wb = servlet.createSpreadsheet(process);
        assertNotNull("Created workbook", wb);
        assertEquals("Created one sheet", 1, wb.getNumberOfSheets());
        assertEquals("Created correct number of rows", 5, wb.getSheetAt(0).getPhysicalNumberOfRows());
        final String b2String = wb.getSheetAt(0).getRow(2).getCell(2).getStringCellValue();
        assertTrue("expect cell starts with repeated: " + b2String,
                b2String.startsWith("error2error2") && b2String.length() >= overB2Width);

        final String c3String = wb.getSheetAt(0).getRow(3).getCell(3).getStringCellValue();
        assertTrue("expect cell starts with repeated: " + c3String,
                c3String.startsWith("stacktrace3stacktrace3") && c3String.length() >= overC3Width);
        final int cWidth = wb.getSheetAt(0).getColumnWidth(3);

        assertTrue("expect C column width is less than requested: " + cWidth + " < " + overC3Width,
                cWidth < overC3Width);


    }

    static String repeatUntilMinLengthReached(final String value, final int minLength) {
        final int dividend = minLength / value.length();
        final int repeat = dividend * value.length() >= minLength ? dividend : dividend + 1;
        return StringUtils.repeat(value, repeat);
    }

}
