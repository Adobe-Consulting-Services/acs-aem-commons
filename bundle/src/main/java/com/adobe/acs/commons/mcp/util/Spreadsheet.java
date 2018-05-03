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
package com.adobe.acs.commons.mcp.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.request.RequestParameter;

/**
 * Simple abstraction of reading a single spreadsheet of values. Expects a header row of named columns (case-sensitive)
 * If provided, will also filter data rows missing required columns to prevent processing errors.
 */
public class Spreadsheet {

    public static final String DEFAULT_DELIMITER = ",";
    public static final String ROW_NUMBER = "~~ROWNUM~~";
    private String fileName = "unknown";
    private int rowCount;
    private transient List<Map<String, CompositeVariant>> dataRows;
    private final List<String> requiredColumns;
    private Map<String, Class> headerTypes;
    private List<String> headerRow;
    private final Map<String, String> delimiters;
    private boolean enableHeaderNameConversion = true;

    /**
     * Simple constructor used for unit testing purposes
     *
     * @param convertHeaderNames If true, header names are converted
     * @param headerArray List of strings for header columns
     */
    public Spreadsheet(boolean convertHeaderNames, String... headerArray) {
        this.enableHeaderNameConversion = convertHeaderNames;
        headerTypes = Arrays.stream(headerArray).collect(Collectors.toMap(this::convertHeaderName, this::detectTypeFromName));
        headerRow = new ArrayList(headerTypes.keySet());
        requiredColumns = Collections.EMPTY_LIST;
        dataRows = new ArrayList<>();
        delimiters = new HashMap<>();
    }

    public Spreadsheet(boolean convertHeaderNames, InputStream file, String... required) throws IOException {
        delimiters = new HashMap<>();
        this.enableHeaderNameConversion = convertHeaderNames;
        if (required == null || required.length == 0) {
            requiredColumns = Collections.EMPTY_LIST;
        } else {
            requiredColumns = Arrays.stream(required).map(this::convertHeaderName).collect(Collectors.toList());
        }
        parseInputFile(file);
    }

    public Spreadsheet(boolean convertHeaderNames, RequestParameter file, String... required) throws IOException {
        this(convertHeaderNames, file.getInputStream(), required);
        fileName = file.getFileName();
    }

    public Spreadsheet(InputStream file, String... required) throws IOException {
        this(true, file, required);
    }

    public Spreadsheet(RequestParameter file, String... required) throws IOException {
        this(true, file, required);
    }

    /**
     * Parse out the input file synchronously for easier unit test validation
     *
     * @return List of files that will be imported, including any renditions
     * @throws IOException if the file couldn't be read
     */
    private void parseInputFile(InputStream file) throws IOException {

        XSSFWorkbook workbook = new XSSFWorkbook(file);

        final XSSFSheet sheet = workbook.getSheetAt(0);
        rowCount = sheet.getLastRowNum();
        final Iterator<Row> rows = sheet.rowIterator();

        Row firstRow = rows.next();
        headerRow = readRow(firstRow).stream()
                .map(Variant::toString)
                .map(this::convertHeaderName)
                .collect(Collectors.toList());
        headerTypes = readRow(firstRow).stream()
                .map(Variant::toString)
                .collect(Collectors.toMap(
                        this::convertHeaderName,
                        this::detectTypeFromName,
                        this::upgradeToArray
                ));

        Iterable<Row> remainingRows = () -> rows;
        dataRows = StreamSupport.stream(remainingRows.spliterator(), false)
                .map(this::buildRow)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<Variant> readRow(Row row) {
        Iterator<Cell> iterator = row.cellIterator();
        List<Variant> rowOut = new ArrayList<>();
        while (iterator.hasNext()) {
            Cell c = iterator.next();
            while (c.getColumnIndex() > rowOut.size()) {
                rowOut.add(null);
            }
            Variant val = new Variant(c);
            rowOut.add(val.isEmpty() ? null : val);
        }
        return rowOut;
    }

    @SuppressWarnings("squid:S3776")
    private Optional<Map<String, CompositeVariant>> buildRow(Row row) {
        Map<String, CompositeVariant> out = new LinkedHashMap<>();
        out.put(ROW_NUMBER, new CompositeVariant(row.getRowNum()));
        List<Variant> data = readRow(row);
        boolean empty = true;
        for (int i = 0; i < data.size() && i < getHeaderRow().size(); i++) {
            String colName = getHeaderRow().get(i);
            if (data.get(i) != null && !data.get(i).isEmpty()) {
                empty = false;
                if (!out.containsKey(colName)) {
                    out.put(colName, new CompositeVariant(headerTypes.get(colName)));
                }
                if (headerTypes.get(colName).isArray()) {
                    String[] values = data.get(i).toString().split(Pattern.quote(delimiters.getOrDefault(colName, DEFAULT_DELIMITER)));
                    for (String value : values) {
                        if (value != null && !value.isEmpty()) {
                            out.get(colName).addValue(value.trim());
                        }
                    }
                } else {
                    out.get(colName).addValue(data.get(i));
                }                    
            }
        }
        if (empty || (!requiredColumns.isEmpty() && !out.keySet().containsAll(requiredColumns))) {
            return Optional.empty();
        } else {
            return Optional.of(out);
        }
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the rowCount
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * @return the headerRow
     */
    public List<String> getHeaderRow() {
        return headerRow;
    }

    /**
     * @return the dataRows
     */
    public List<Map<String, CompositeVariant>> getDataRows() {
        return dataRows;
    }
    
    public Long getRowNum(Map<String, CompositeVariant> row) {
        return (Long) row.get(ROW_NUMBER).getValueAs(Long.class);
    }

    /**
     * @return the requiredColumns
     */
    public List<String> getRequiredColumns() {
        return requiredColumns;
    }

    public String convertHeaderName(String str) {
        if (enableHeaderNameConversion) {
            if (str.contains("@")) {
                str = StringUtils.substringBefore(str, "@");
            }
            return String.valueOf(str).toLowerCase().replaceAll("[^0-9a-zA-Z:\\-]+", "_");
        } else {
            return String.valueOf(str);
        }
    }

    /**
     * Look for type hints in the name of a column to extract a usable type. Also look for array hints as well. <br>
     * Possible formats: 
     * <ul>
     * <li>column-name - A column named "column-name" </li>
     * <li>col@int - An integer column named "col" </li>
     * <li>col2@int[] - An integer array colum named "col2", assumes standard delimiter (,) </li>
     * <li>col3@string[] or col3@[] - A String array named "col3", assumes standard delimiter (,)</li>
     * <li>col4@string[||] - A string array where values are using a custom delimiter (||)</li>
     * </ul>
     * 
     * @param name
     * @return
     */
    protected Class detectTypeFromName(String name) {
        boolean isArray = false;
        Class detectedClass = String.class;
        if (name.contains("@")) {
            String typeStr = StringUtils.substringAfter(name, "@");
            if (name.endsWith("]")) {
                String colName = convertHeaderName(name);
                isArray = true;
                String delimiter = StringUtils.substringBetween(name, "[", "]");
                typeStr = StringUtils.substringBefore("[", delimiter);
                if (!StringUtils.isEmpty(delimiter)) {
                    delimiters.put(colName, delimiter);
                }
            }
            detectedClass = getClassFromName(typeStr);
        }
        if (isArray) {
            return getArrayType(detectedClass);
        } else {
            return detectedClass;
        }
    }

    protected Class getClassFromName(String typeStr) {
        switch (typeStr.toLowerCase()) {
            case "int":
            case "integer":
                return Integer.TYPE;
            case "long":
                return Long.TYPE;
            case "double":
            case "number":
                return Double.TYPE;
            case "date":
            case "calendar":
            case "cal":
            case "time":
                return Date.class;
            case "boolean":
            case "bool":
                return Boolean.TYPE;
            case "string":
            case "str":
            default:
                return String.class;
        }
    }

    /**
     * Consider if a column is seen twice then that column type should be considered an array. Because String is a
     * default assumption when no type is specified, any redefinition of a column to a more specific type will be then
     * assumed for that property altogether.
     *
     * @param a
     * @param b
     * @return
     */
    protected Class upgradeToArray(Class a, Class b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.equals(b) || b == String.class) {
            return getArrayType(a);
        } else {
            return getArrayType(b);
        }
    }

    public static Class getArrayType(Class clazz) {
        if (clazz.isArray()) {
            return clazz;
        } else {
            return Array.newInstance(clazz, 0).getClass();
        }
    }    
}
