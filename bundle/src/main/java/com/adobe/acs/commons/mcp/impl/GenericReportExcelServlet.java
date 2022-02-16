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

import com.adobe.acs.commons.mcp.model.AbstractReport;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import com.adobe.acs.commons.mcp.model.GenericReport;
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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Export a generic report as an excel spreadsheet
 */
@SlingServlet(resourceTypes = { GenericReport.GENERIC_REPORT_RESOURCE_TYPE,
        GenericBlobReport.BLOB_REPORT_RESOURCE_TYPE }, extensions = { "xlsx", "xls" })
public class GenericReportExcelServlet extends SlingSafeMethodsServlet {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(GenericReportExcelServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        AbstractReport report = getReport(request.getResource());
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
                LOG.error("Error generating excel export for "+request.getResource().getPath(), ex);
                throw ex;
            }
        } else {
            String msg = String.format("Unable to process report stored at %s", request.getResource().getPath());
            throw new ServletException(msg);
        }
    }

    @SuppressWarnings("squid:S3776")
    private Workbook createSpreadsheet(AbstractReport report) {
        Workbook wb = new XSSFWorkbook();

        String name = report.getName();
        for (char ch : new char[]{'\\','/','*','[',']',':','?'}) {
            name = StringUtils.remove(name, ch);
        }
        Sheet sheet = wb.createSheet(name);
        sheet.createFreezePane(0, 1, 0, 1);

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(wb);
        for (int c = 0; c < report.getColumnNames().size(); c++) {
            Cell headerCell = headerRow.createCell(c);
            headerCell.setCellValue(report.getColumnNames().get(c));
            headerCell.setCellStyle(headerStyle);
        }

        List<ValueMap> rows = report.getRows();
        //make rows, don't forget the header row
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r+1);

            //make columns
            for (int c = 0; c < report.getColumns().size(); c++) {
                String col = report.getColumns().get(c);
                Cell cell = row.createCell(c);

                if (rows.get(r).containsKey(col)) {
                    Object val = rows.get(r).get(col);
                    if (val instanceof Number) {
                        Number n = (Number) val;
                        cell.setCellValue(n.doubleValue());
                    } else {
                        String sval = String.valueOf(val);
                        if (sval.startsWith("=")) {
                            cell.setCellFormula(sval.substring(1));
                        } else {
                            cell.setCellValue(sval);
                        }
                    }
                }
            }
        }
        int lastColumnIndex = report.getColumnNames().size();
        autosize(sheet, lastColumnIndex);
        sheet.setAutoFilter(new CellRangeAddress(0, 1 + rows.size(),0, lastColumnIndex - 1));
        return wb;
    }

    CellStyle createHeaderStyle(Workbook wb){
        XSSFCellStyle xstyle = (XSSFCellStyle)wb.createCellStyle();
        XSSFColor header = new XSSFColor(new Color(79, 129, 189));
        xstyle.setFillForegroundColor(header);
        xstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = (XSSFFont)wb.createFont();
        font.setColor(IndexedColors.WHITE.index);
        xstyle.setFont(font);
        return xstyle;
    }

    void autosize(Sheet sheet, int lastColumnIndex){
        for(int i = 0; i <= lastColumnIndex; i++ ) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Throwable e){
                // autosize depends on AWT stuff and can fail, but it should not be fatal
                LOG.warn("autoSizeColumn({}) failed: {}",i, e.getMessage());
            }
            int cw = sheet.getColumnWidth(i);
            // increase width to accommodate drop-down arrow in the header
            if (cw/256 < 20) {
                sheet.setColumnWidth(i, 256*12);
            } else if (cw/256 > 120) {
                sheet.setColumnWidth(i, 256*120);
            }
        }
    }

    /**
     * Retrieve the actual report from the path
     *
     * @param reportResource the resource from where to take the report
     * @return the report or null if there is no report
     */
    AbstractReport getReport(Resource reportResource) {
        AbstractReport result = reportResource.adaptTo(GenericReport.class);
        if (result != null && result.getRows() != null && result.getRows().size() > 0) {
            return result;
        }
        return reportResource.adaptTo(GenericBlobReport.class);

    }

    }
