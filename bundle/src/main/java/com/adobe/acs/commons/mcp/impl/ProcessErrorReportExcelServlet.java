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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.model.ArchivedProcessFailure;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import com.day.cq.commons.jcr.JcrUtil;
import java.awt.Color;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.apache.poi.ss.usermodel.CreationHelper;

/**
 * Export a generic report as an excel spreadsheet
 */
@SlingServlet(resourceTypes = ProcessInstance.RESOURCE_TYPE, selectors = "errors", extensions = {"xlsx", "xls"})
public class ProcessErrorReportExcelServlet extends SlingSafeMethodsServlet {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ProcessErrorReportExcelServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        ManagedProcess report = request.getResource().adaptTo(ManagedProcess.class);
        if (report != null) {
            String title = report.getName();
            String fileName = JcrUtil.createValidName(title) + ".xlsx";

            Workbook workbook = createSpreadsheet(report);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            try (ServletOutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            } catch (Exception ex) {
                LOG.error("Error generating excel export for " + request.getResource().getPath(), ex);
                throw ex;
            }
        } else {
            LOG.error("Unable to process report stored at " + request.getResource().getPath());
            throw new ServletException("Unable to process report stored at " + request.getResource().getPath());
        }
    }

    @SuppressWarnings("squid:S3776")
    protected Workbook createSpreadsheet(ManagedProcess report) {
        Workbook wb = new XSSFWorkbook();

        String name = report.getName();
        for (char ch : new char[]{'\\', '/', '*', '[', ']', ':', '?'}) {
            name = StringUtils.remove(name, ch);
        }
        Sheet sheet = wb.createSheet(name);
        sheet.createFreezePane(0, 1, 0, 1);

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dateStyle = wb.createCellStyle();
        CreationHelper createHelper = wb.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyy/mm/dd h:mm:ss"));
        
        for (String columnName : Arrays.asList("Time", "Path", "Error", "Stack trace")) {
            Cell headerCell = headerRow.createCell(headerRow.getPhysicalNumberOfCells());
            headerCell.setCellValue(columnName);
            headerCell.setCellStyle(headerStyle);
        }

        Collection<ArchivedProcessFailure> rows = report.getReportedErrorsList();
        //make rows, don't forget the header row
        for (ArchivedProcessFailure error : rows) {
            Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());
            Cell c;

            c = row.createCell(0);
            c.setCellValue(error.time);
            c.setCellStyle(dateStyle);
            c = row.createCell(1);
            c.setCellValue(error.nodePath);
            c = row.createCell(2);
            c.setCellValue(error.error);
            c = row.createCell(3);
            c.setCellValue(error.stackTrace);
        }
        autosize(sheet, 4);
        sheet.setAutoFilter(new CellRangeAddress(0, 1 + rows.size(), 0, 3));
        return wb;
    }

    CellStyle createHeaderStyle(Workbook wb) {
        XSSFCellStyle xstyle = (XSSFCellStyle) wb.createCellStyle();
        XSSFColor header = new XSSFColor(new Color(79, 129, 189));
        xstyle.setFillForegroundColor(header);
        xstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setColor(IndexedColors.WHITE.index);
        xstyle.setFont(font);
        return xstyle;
    }

    void autosize(Sheet sheet, int lastColumnIndex) {
        for (int i = 0; i <= lastColumnIndex; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Exception e) {
                // autosize depends on AWT stuff and can fail, but it should not be fatal
                LOG.warn("autoSizeColumn(" + i + ") failed: " + e.getMessage());
            }
            int cw = sheet.getColumnWidth(i);
            // increase width to accommodate drop-down arrow in the header
            if (cw / 256 < 20) {
                sheet.setColumnWidth(i, 256 * 12);
            } else if (cw / 256 > 120) {
                sheet.setColumnWidth(i, 256 * 120);
            }
        }
    }
}
