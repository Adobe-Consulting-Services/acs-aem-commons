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

import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.ACS_REDIRECTS_RESOURCE_TYPE;


/**
 * A servlet to export redirect configurations into an Excel spreadsheet
 *
 */
@Component(service = Servlet.class, immediate = true, name = "ExportRedirectMapServlet", property = {
        "sling.servlet.label=ACS AEM Commons - Export Redirects Servlet",
        "sling.servlet.methods=GET",
        "sling.servlet.selectors=export",
        "sling.servlet.resourceTypes=" + ACS_REDIRECTS_RESOURCE_TYPE
})
public class ExportRedirectMapServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(ExportRedirectMapServlet.class);
    private static final long serialVersionUID = -3564475196678277711L;

    static final String SPREADSHEETML_SHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        String path = request.getParameter("path");
        Resource root = request.getResourceResolver().getResource(path);
        log.debug("Requesting redirect maps from {}", path);

        Collection<RedirectRule> rules = RedirectFilter.getRules(root);
        XSSFWorkbook wb = export(rules);

        response.setContentType(SPREADSHEETML_SHEET);
        response.setHeader("Content-Disposition", "attachment;filename=\"acs-redirects.xlsx\" ");
        wb.write(response.getOutputStream());
    }

    static XSSFWorkbook export(Collection<RedirectRule> rules) {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = wb.createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);

        CellStyle dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(
                wb.createDataFormat().getFormat("mmm d, yyyy"));

        CellStyle lockedCellStyle = wb.createCellStyle();
        lockedCellStyle.setLocked(true); // readonly cell

        CellStyle cellWrapStyle = wb.createCellStyle();
        cellWrapStyle.setWrapText(true);

        Row headerRow;
        int rownum = 0;
        Sheet sheet = wb.createSheet("Redirects");
        headerRow = sheet.createRow(rownum++);
        headerRow.createCell(0).setCellValue("Source Url");
        headerRow.createCell(1).setCellValue("Target Url");
        headerRow.createCell(2).setCellValue("Status Code");
        headerRow.createCell(3).setCellValue("Off Time");
        headerRow.createCell(4).setCellValue("Notes");
        headerRow.createCell(5).setCellValue("Evaluate URI");
        headerRow.createCell(6).setCellValue("Ignore Context Prefix");
        headerRow.createCell(7).setCellValue("Tags");
        headerRow.createCell(8).setCellValue("Created");
        headerRow.createCell(9).setCellValue("Created By");
        headerRow.createCell(10).setCellValue("Modified");
        headerRow.createCell(11).setCellValue("Modified By");
        headerRow.createCell(12).setCellValue("On Time");
        for (Cell cell : headerRow) {
            cell.setCellStyle(headerStyle);
        }
        for (RedirectRule rule : rules) {
            Row row = sheet.createRow(rownum++);
            row.createCell(0).setCellValue(rule.getSource());
            row.createCell(1).setCellValue(rule.getTarget());
            row.createCell(2).setCellValue(rule.getStatusCode());
            Calendar untilDateTime = rule.getUntilDate();
            if (untilDateTime != null) {
                Cell cell = row.createCell(3);
                cell.setCellValue(untilDateTime);
                cell.setCellStyle(dateStyle);
            }
            row.createCell(4).setCellValue(rule.getNote());
            row.createCell(5).setCellValue(rule.getEvaluateURI());
            row.createCell(6).setCellValue(rule.getContextPrefixIgnored());

            Cell cell6 = row.createCell(7);
            String[] tagIds = rule.getTagIds();
            if(tagIds != null) {
                cell6.setCellValue(String.join("\n", tagIds));
            }
            cell6.setCellStyle(cellWrapStyle);

            Cell cell7 = row.createCell(8);
            cell7.setCellValue(rule.getCreated());
            cell7.setCellStyle(dateStyle);

            Cell cell8 = row.createCell(9);
            cell8.setCellValue(rule.getCreatedBy());
            cell8.setCellStyle(lockedCellStyle);

            Cell cell9 = row.createCell(10);
            cell9.setCellValue(rule.getModified());
            cell9.setCellStyle(dateStyle);

            Cell cell10 = row.createCell(11);
            cell10.setCellValue(rule.getModifiedBy());
            cell10.setCellStyle(lockedCellStyle);

            Calendar effectiveFrom = rule.getEffectiveFrom();
            if (effectiveFrom != null) {
                Cell cell = row.createCell(12);
                cell.setCellValue(effectiveFrom);
                cell.setCellStyle(dateStyle);
            }
        }
        sheet.setAutoFilter(new CellRangeAddress(0, rownum - 1, 0, 10));
        sheet.setColumnWidth(0, 256 * 50);
        sheet.setColumnWidth(1, 256 * 50);
        sheet.setColumnWidth(2, 256 * 15);
        sheet.setColumnWidth(3, 256 * 12);
        sheet.setColumnWidth(4, 256 * 100);
        sheet.setColumnWidth(5, 256 * 20);
        sheet.setColumnWidth(6, 256 * 20);
        sheet.setColumnWidth(7, 256 * 25);
        sheet.setColumnWidth(8, 256 * 12);
        sheet.setColumnWidth(9, 256 * 30);
        sheet.setColumnWidth(10, 256 * 12);
        sheet.setColumnWidth(11, 256 * 30);
        sheet.setColumnWidth(12, 256 * 12);

        return wb;
    }

}
