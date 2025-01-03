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

import com.adobe.acs.commons.redirects.models.ExportColumn;
import com.adobe.acs.commons.redirects.models.ImportLog;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import com.day.text.csv.Csv;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.servlets.impl.CsvRedirectExporter.DATE_FORMATTER;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;

public class CsvRedirectImporter implements RedirectReader {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ImportLog auditLog;

    public CsvRedirectImporter(ImportLog auditLog) {
        this.auditLog = auditLog;
    }

    public Collection<Map<String, Object>> read(InputStream is) throws IOException {
        long t0 = System.currentTimeMillis();
        Collection<Map<String, Object>> rules = new LinkedHashSet<>();
        Csv csv = new Csv();
        Iterator<String[]> it = csv.read(is, StandardCharsets.UTF_8.name());
        if (it.hasNext()) {
            String[] headerRow = it.next();
            Map<ExportColumn, Integer> cols = mapCsvColumns(headerRow);
            int rownum = 1;
            while (it.hasNext()) {
                String[] record = it.next();
                Map<String, Object> props = readCsvRedirect(record, rownum++, cols);
                if (props != null) {
                    rules.add(props);
                }
            }
        }
        log.debug("{} rules read from CSV in {}ms", rules.size(), System.currentTimeMillis() - t0);
        return rules;
    }

    Map<String, Object> readCsvRedirect(String[] record, int rownum, Map<ExportColumn, Integer> cols) {
        if (record.length < 3) {
            return null;
        }

        Map<String, Object> props = new HashMap<>();
        props.put(PROPERTY_RESOURCE_TYPE, REDIRECT_RULE_RESOURCE_TYPE);

        // Required fields
        String source = record[0];
        String target = record[1];
        String statusCode = record[2];

        if (source.trim().isEmpty()) {
            auditLog.warn("Row " + rownum,
                    "Column A is required and should contain redirect source");
            return null;
        }
        if (target.trim().isEmpty()) {
            auditLog.warn("Row " + rownum,
                    "Column B is required and should contain redirect target");
            return null;
        }
        if (!statusCode.matches("\\d+")) {
            auditLog.warn("Row " + rownum,
                    "Column C is required and should contain redirect status code");
            return null;
        }

        props.put(RedirectRule.SOURCE_PROPERTY_NAME, source);
        props.put(RedirectRule.TARGET_PROPERTY_NAME, target);
        props.put(RedirectRule.STATUS_CODE_PROPERTY_NAME, statusCode);

        Map<String, Object> optionalProps = readOptionalProperties(record, cols, rownum);
        props.putAll(optionalProps);

        return props;
    }

    Map<ExportColumn, Integer> mapCsvColumns(String[] row) {
        Map<ExportColumn, Integer> cols = new LinkedHashMap<>();
        for (int i = 0; i < row.length; i++) {
            if (i < 3) {
                // columns A, B and C are reserved for source, target and statusCode
                continue;
            }
            String title = row[i];
            for (ExportColumn col : ExportColumn.values()) {
                if (col.getTitle().equalsIgnoreCase(title) || col.getPropertyName().equalsIgnoreCase(title)) {
                    cols.put(col, i);
                }
            }
        }
        return cols;
    }

    Map<String, Object> readOptionalProperties(String[] row, Map<ExportColumn, Integer> cols, int rownum) {
        Map<String, Object> props = new HashMap<>();
        for (ExportColumn column : ExportColumn.values()) {
            if (column.ordinal() < 3 || !column.isImportable()) {
                // columns A, B and C are reserved
                continue;
            }
            if (cols.containsKey(column)) {
                int columnIndex = cols.get(column);
                if (columnIndex < row.length && !row[columnIndex].isEmpty()) {
                    Object value = null;
                    if (column.getPropertyType() == Boolean.class) {
                        value = Boolean.parseBoolean(row[columnIndex]);
                    } else if (column.getPropertyType() == Calendar.class) {
                        try {
                            LocalDate date = LocalDate.parse(row[columnIndex], DATE_FORMATTER);
                            value = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()));
                        } catch (DateTimeException e) {
                            String cellAddress = new CellReference(rownum, columnIndex).formatAsString();
                            auditLog.info(cellAddress, "Can't set '" + column.getTitle()
                                    + "' from a Date cell: '" + row[columnIndex] + "', ensure the value is formatted as MMM d, yyyy");
                        }
                    } else if (column.getPropertyType() == String[].class) {
                        value = row[columnIndex].split("\n");
                    } else {
                        value = row[columnIndex];
                    }
                    if (value != null) {
                        props.put(column.getPropertyName(), value);
                    }
                }
            }
        }
        return props;
    }
}
