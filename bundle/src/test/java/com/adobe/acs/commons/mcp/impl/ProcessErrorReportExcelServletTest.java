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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.model.ArchivedProcessFailure;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import com.adobe.acs.commons.util.impl.ActivatorHelper;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic code coverage for the error report servlet
 */
public class ProcessErrorReportExcelServletTest {

    private static final String RESOURCE_PATH = "/content/child";

    public ProcessErrorReportExcelServletTest() {
    }

    ManagedProcess process;
    ProcessErrorReportExcelServlet servlet;
    List<ArchivedProcessFailure> failures;

    private ActivatorHelper activatorHelper = new ActivatorHelper();

    @Rule
    public final AemContext context = new AemContext(activatorHelper.afterSetup(), activatorHelper.beforeTeardown(),
            ResourceResolverType.JCR_MOCK);

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
        final int overB2Width = servlet.getColumnBlockSize() * servlet.getMinColumnBlockCount();
        final int overC3Width = servlet.getColumnBlockSize() * (servlet.getMaxColumnBlockCount() + 1);
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

    @Test
    public void testAutosizeException() {
        final Sheet sheet = mock(Sheet.class);
        final CompletableFuture<Integer> newWidth = new CompletableFuture<>();
        final int autosizeCol = 1;

        doThrow(new RuntimeException("This is a totally expected mock error")).when(sheet).autoSizeColumn(anyInt());
        doAnswer(call -> {
            if (call.getArgument(0, Integer.class).equals(autosizeCol)) {
                return newWidth.getNow(servlet.getColumnBlockSize() * (servlet.getMaxColumnBlockCount() + 1));
            }
            return servlet.getColumnBlockSize() * servlet.getMinColumnBlockCount();
        }).when(sheet).getColumnWidth(anyInt());
        doAnswer(call -> {
            if (call.getArgument(0, Integer.class).equals(autosizeCol)) {
                newWidth.complete(call.getArgument(1));
            }
            return true;
        }).when(sheet).setColumnWidth(anyInt(), anyInt());

        final int expectAutosizedWith = servlet.getColumnBlockSize() * servlet.getMaxColumnBlockCount();
        servlet.autosize(sheet, autosizeCol);
        assertTrue("width was set", newWidth.isDone());
        assertEquals("expect width", expectAutosizedWith, sheet.getColumnWidth(autosizeCol));
    }

    @Test
    public void testDoGet() throws Exception {
        this.context.create().resource(RESOURCE_PATH, "prop", "resourceval");
        this.context.registerAdapter(Resource.class, ManagedProcess.class, process);
        this.context.currentResource(RESOURCE_PATH);
        servlet.doGet(this.context.request(), this.context.response());
    }

    @Test(expected = IllegalStateException.class)
    public void testDoGet_innerThrow() throws Exception {
        this.context.create().resource(RESOURCE_PATH, "prop", "resourceval");
        this.context.registerAdapter(Resource.class, ManagedProcess.class, process);
        this.context.currentResource(RESOURCE_PATH);
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
        doThrow(IllegalStateException.class).when(response).getOutputStream();
        servlet.doGet(this.context.request(), response);
    }

    @Test(expected = ServletException.class)
    public void testDoGet_outerThrow() throws Exception {
        this.context.create().resource(RESOURCE_PATH, "prop", "resourceval");
        this.context.currentResource(RESOURCE_PATH);
        servlet.doGet(this.context.request(), this.context.response());
    }
}
