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

import java.util.ArrayList;
import java.util.List;

import com.adobe.acs.commons.mcp.model.ArchivedProcessFailure;
import com.adobe.acs.commons.mcp.model.ManagedProcess;

import java.util.Date;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

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
        addFailure("path1", "error1", "stacktrace1");
        addFailure("path2", "error2", "stacktrace2");
        addFailure("path3", "error3", "stacktrace3");
        addFailure("path4", "error4", "stacktrace4");
        Workbook wb = servlet.createSpreadsheet(process);
        assertNotNull("Created workbook", wb);
        assertEquals("Created one sheet", 1, wb.getNumberOfSheets());
        assertEquals("Created correct number of rows", 5, wb.getSheetAt(0).getPhysicalNumberOfRows());
    }

}
