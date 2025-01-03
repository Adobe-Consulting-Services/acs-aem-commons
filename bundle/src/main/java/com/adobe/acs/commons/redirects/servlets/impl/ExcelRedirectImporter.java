/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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
package com.adobe.acs.commons.redirects.servlets.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirects.models.ExportColumn;
import com.adobe.acs.commons.redirects.models.ImportLog;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;

public class ExcelRedirectImporter implements RedirectReader {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ImportLog auditLog;

    public ExcelRedirectImporter(ImportLog auditLog) {
      this.auditLog = auditLog;
    }

    public Collection<Map<String, Object>> read(InputStream is) throws IOException {
        long t0 = System.currentTimeMillis();
        Collection<Map<String, Object>> rules = new LinkedHashSet<>();
        Workbook wb = new XSSFWorkbook(is);
        Sheet sheet = wb.getSheetAt(0);
        Iterator<Row> it = sheet.rowIterator();
        if (it.hasNext()) {
            Row headerRow = it.next();
            Map<ExportColumn, Integer> cols = mapXlsColumns(headerRow);

            while (it.hasNext()) {
                Row row = it.next();
                Map<String, Object> props = readXlsRedirect(row, cols);
                if (props != null) {
                    rules.add(props);
                } else {
                    log.debug("couldn't read redirect properties from row {} ", row.getRowNum());
                }
            }

        }

        log.debug("{} rules read from spreadsheet in {}ms", rules.size(), System.currentTimeMillis() - t0);
        return rules;
    }

    /**
     * Read columns from the header row (rownum=0) and map them to column definitions
     */
    Map<ExportColumn, Integer> mapXlsColumns(Row row) {
        Map<ExportColumn, Integer> cols = new LinkedHashMap<>();
        for (Cell cell : row) {
            if (cell.getColumnIndex() <= 2) {
                // columns A, B and C are reserved for source, target and statusCode
                continue;
            }
            if (cell.getCellType() == CellType.STRING) {
                String title = cell.getStringCellValue();
                for (ExportColumn col : ExportColumn.values()) {
                    if (col.getTitle().equalsIgnoreCase(title)) {
                        cols.put(col, cell.getColumnIndex());
                    }
                }
            }
        }
        return cols;
    }
   /**
     * read redirect properties from a spreadsheet row
     *
     * @return values to be merged with redirect's ValueMap
     */
    private Map<String, Object> readXlsRedirect(Row row, Map<ExportColumn, Integer> cols) {
        Map<String, Object> props = new HashMap<>();
        props.put(PROPERTY_RESOURCE_TYPE, REDIRECT_RULE_RESOURCE_TYPE);
        Cell c0 = row.getCell(0);
        if (c0 == null || c0.getCellType() != CellType.STRING) {
            auditLog.warn(new CellReference(row.getRowNum(), 0).formatAsString(),
                    "Cells A is required and should contain redirect source");
            return null;
        }
        Cell c1 = row.getCell(1);
        if (c1 == null || c1.getCellType() != CellType.STRING) {
            auditLog.warn(new CellReference(row.getRowNum(), 1).formatAsString(),
                    "Cells B is required and should contain redirect source");
            return null;
        }
        Cell c2 = row.getCell(2);
        if (c2 == null || c2.getCellType() != CellType.NUMERIC) {
            auditLog.warn(new CellReference(row.getRowNum(), 2).formatAsString(),
                    "Cells C is required and should contain redirect status code");
            return null;
        }
        String source = c0.getStringCellValue();
        props.put(RedirectRule.SOURCE_PROPERTY_NAME, source);
        String target = c1.getStringCellValue();
        props.put(RedirectRule.TARGET_PROPERTY_NAME, target);
        int statusCode = (int) c2.getNumericCellValue();
        props.put(RedirectRule.STATUS_CODE_PROPERTY_NAME, String.valueOf(statusCode));

        Map<String, Object> optionalProps = readOptionalProperties(row, cols);
        props.putAll(optionalProps);

        return props;
    }

    @SuppressWarnings("squid:S3776")
    private Map<String, Object> readOptionalProperties(Row row, Map<ExportColumn, Integer> cols) {
        Map<String, Object> props = new HashMap<>();
        for (ExportColumn column : ExportColumn.values()) {
            if (column.ordinal() < 3 || !column.isImportable()) {
                // columns A, B  and C are reserved
                continue;
            }
            if (cols.containsKey(column)) {
                int columnIndex = cols.get(column);
                Cell cell = row.getCell(columnIndex);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    Object value = null;
                    if (column.getPropertyType() == String[].class && cell.getCellType() == CellType.STRING) {
                        value = cell.getStringCellValue().split("\n");
                    } else if (column.getPropertyType() == String.class && cell.getCellType() == CellType.STRING) {
                        value = cell.getStringCellValue();
                    } else if (column.getPropertyType() == Boolean.class && cell.getCellType() == CellType.BOOLEAN) {
                        value = cell.getBooleanCellValue();
                    } else if (column.getPropertyType() == Calendar.class && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(cell.getDateCellValue());
                        value = calendar;
                    }
                    if (value != null) {
                        props.put(column.getPropertyName(), value);
                    } else {
                        String cellAddress = new CellReference(row.getRowNum(), cell.getColumnIndex()).formatAsString();
                        auditLog.info(cellAddress, "Can't set '" + column.getTitle() + "' from a "
                                + cell.getCellType().toString().toLowerCase() + " cell: '" + cell + "'");
                    }
                }
            }
        }
        return props;
    }
}