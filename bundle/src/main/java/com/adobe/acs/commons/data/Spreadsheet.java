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
package com.adobe.acs.commons.data;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.request.RequestParameter;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Simple abstraction of reading a single spreadsheet of values. Expects a
 * header row of named columns (case-sensitive) If provided, will also filter
 * data rows missing required columns to prevent processing errors.
 */
@ProviderType
public class Spreadsheet {

    public static final String DEFAULT_DELIMITER = ",";
    public static final String ROW_NUMBER = "~~ROWNUM~~";
    private String fileName = "unknown";
    private int rowCount;
    private transient List<Map<String, CompositeVariant>> dataRows;
    private final List<String> requiredColumns;
    private Map<String, Optional<Class>> headerTypes;
    private List<String> headerRow;
    private final Map<String, String> delimiters;
    private boolean enableHeaderNameConversion = true;
    private InputStream inputStream;
    private List<String> caseInsensitiveHeaders = new ArrayList<>();

    /**
     * Simple constructor used for unit testing purposes
     *
     * @param convertHeaderNames If true, header names are converted
     * @param headerArray        List of strings for header columns
     */
    public Spreadsheet(boolean convertHeaderNames, String... headerArray) {
        this.enableHeaderNameConversion = convertHeaderNames;
        headerTypes = Arrays.stream(headerArray)
                .collect(Collectors.toMap(this::convertHeaderName, this::detectTypeFromName));
        headerRow = Arrays.asList(headerArray);
        requiredColumns = Collections.emptyList();
        dataRows = new ArrayList<>();
        delimiters = new HashMap<>();
    }

    /**
     * Simple constructor used for unit testing purposes
     *
     * @param convertHeaderNames     If true, header names are converted
     * @param caseInsensitiveHeaders Header names that will be ignored during conversion
     * @param headerArray            List of strings for header columns
     */
    public Spreadsheet(boolean convertHeaderNames, List<String> caseInsensitiveHeaders, String... headerArray) {
        this(convertHeaderNames, headerArray);
        Optional.ofNullable(caseInsensitiveHeaders).ifPresent(this.caseInsensitiveHeaders::addAll);
    }

    public Spreadsheet(boolean convertHeaderNames, InputStream file, String... required) {
        dataRows = new ArrayList<>();
        delimiters = new HashMap<>();
        this.enableHeaderNameConversion = convertHeaderNames;
        if (required == null || required.length == 0) {
            requiredColumns = Collections.emptyList();
        } else {
            requiredColumns = Arrays.stream(required)
                    .map(this::convertHeaderName)
                    .collect(Collectors.toList());
        }
        this.headerRow = new ArrayList<>();
        this.inputStream = file;
    }

    public Spreadsheet(boolean convertHeaderNames, RequestParameter file, String... required) throws IOException {
        this(convertHeaderNames, file.getInputStream(), required);
        fileName = file.getFileName();
    }

    public Spreadsheet(InputStream file, String... required) {
        this(true, file, required);
    }

    public Spreadsheet(RequestParameter file, String... required) throws IOException {
        this(true, file, required);
    }

    public Spreadsheet(RequestParameter file, List<String> caseInsensitiveHeaders, String... required) throws IOException {
        this(true, file, required);
        Optional.ofNullable(caseInsensitiveHeaders).ifPresent(this.caseInsensitiveHeaders::addAll);
    }
    
    /**
     * Parse out the input file synchronously for easier unit test validation.
     * This overload will implicitly use the default JVM locale for numeric and date/time conversions.
     * 
     * @return List of files that will be imported, including any renditions
     * @throws IOException if the file couldn't be read
     */
    public Spreadsheet buildSpreadsheet() throws IOException {
        return buildSpreadsheet(Locale.getDefault());
    }
    
    /**
     * Parse out the input file synchronously for easier unit test validation
     *
     * @param locale The locale to be used for numeric and date/time conversions.
     * @return List of files that will be imported, including any renditions
     * @throws IOException if the file couldn't be read
     */
    public Spreadsheet buildSpreadsheet(Locale locale) throws IOException {

        XSSFWorkbook workbook = new XSSFWorkbook(this.inputStream);

        final XSSFSheet sheet = workbook.getSheetAt(0);
        rowCount = sheet.getLastRowNum();
        final Iterator<Row> rows = sheet.rowIterator();

        Row firstRow = rows.next();
        headerRow = readRow(firstRow, locale).stream()
                .map(v -> v != null ? convertHeaderName(v.toString()) : null)
                .collect(Collectors.toList());
        headerTypes = readRow(firstRow, locale).stream()
                .map(Variant::toString)
                .collect(Collectors.toMap(
                        this::convertHeaderName,
                        this::detectTypeFromName,
                        this::upgradeToArray
                ));

        Iterable<Row> remainingRows = () -> rows;
        dataRows = StreamSupport.stream(remainingRows.spliterator(), false)
                .map(row -> buildRow(row, locale))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return this;
    }

    private List<Variant> readRow(Row row, Locale locale) {
        Iterator<Cell> iterator = row.cellIterator();
        List<Variant> rowOut = new ArrayList<>();
        while (iterator.hasNext()) {
            Cell c = iterator.next();
            while (c.getColumnIndex() > rowOut.size()) {
                rowOut.add(null);
            }
            Variant val = new Variant(c, locale);
            rowOut.add(val.isEmpty() ? null : val);
        }
        return rowOut;
    }

    @SuppressWarnings("squid:S3776")
    private Optional<Map<String, CompositeVariant>> buildRow(Row row, Locale locale) {
        Map<String, CompositeVariant> out = new LinkedHashMap<>();
        out.put(ROW_NUMBER, new CompositeVariant(row.getRowNum()));
        List<Variant> data = readRow(row, locale);
        boolean empty = true;
        for (int i = 0; i < data.size() && i < getHeaderRow().size(); i++) {
            String colName = getHeaderRow().get(i);
            if (colName != null && data.get(i) != null && !data.get(i).isEmpty()) {
                empty = false;
                if (!out.containsKey(colName)) {
                    Class type = headerTypes.get(colName).orElse(data.get(i).getBaseType());
                    if (type == Object.class) {
                        type = data.get(i).getBaseType();
                    } else if (type == Object[].class) {
                        type = getArrayType(Optional.of(data.get(i).getBaseType())).get();
                    }
                    out.put(colName, new CompositeVariant(type));
                }
                Optional<Class> type = headerTypes.get(colName);
                if (type.isPresent() && type.get().isArray()) {
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
        return Collections.unmodifiableList(headerRow);
    }

    /**
     * @return the dataRows
     */
    public List<Map<String, CompositeVariant>> getDataRowsAsCompositeVariants() {
        return Collections.unmodifiableList(dataRows);
    }

    /**
     * Append data to the sheet.
     *
     * @param dataRows the data to append
     */
    public void appendData(List<Map<String, CompositeVariant>> dataRows) {
        Optional.ofNullable(dataRows).ifPresent(newData -> this.dataRows.addAll(newData));
    }

    public Long getRowNum(Map<String, CompositeVariant> row) {
        if (row.containsKey(ROW_NUMBER)) {
            return (Long) row.get(ROW_NUMBER).getValueAs(Long.class);
        } else {
            return (long) dataRows.indexOf(row);
        }
    }

    /**
     * @return the requiredColumns
     */
    public List<String> getRequiredColumns() {
        return Collections.unmodifiableList(requiredColumns);
    }

    public String convertHeaderName(String str) {
        String name;
        if (str.contains("@")) {
            name = StringUtils.substringBefore(str, "@");
        } else {
            name = str;
        }
        if (name.contains("[")) {
            name = StringUtils.substringBefore(name, "[");
        }
        if (enableHeaderNameConversion && isHeaderCaseInsensitive(name)) {
            name = String.valueOf(name).toLowerCase().replaceAll("[^0-9a-zA-Z:\\-]+", "_");
        }

        return name;
    }

    private boolean isHeaderCaseInsensitive(String name) {
        return CollectionUtils.isEmpty(caseInsensitiveHeaders)
                || caseInsensitiveHeaders.stream().anyMatch(s -> s.equalsIgnoreCase(name));
    }

    /**
     * Look for type hints in the name of a column to extract a usable type.
     * Also look for array hints as well. <br>
     * Possible formats:
     * <ul>
     * <li>column-name - A column named "column-name" </li>
     * <li>col@int - An integer column named "col" </li>
     * <li>col2@int[] - An integer array colum named "col2", assumes standard
     * delimiter (,) </li>
     * <li>col3@string[] or col3@[] - A String array named "col3", assumes
     * standard delimiter (,)</li>
     * <li>col4@string[||] - A string array where values are using a custom
     * delimiter (||)</li>
     * </ul>
     *
     * @param name
     * @return
     */
    private Optional<Class> detectTypeFromName(String name) {
        boolean isArray = false;
        Class detectedClass = Object.class;
        if (name.contains("@")) {
            String typeStr = StringUtils.substringAfter(name, "@");
            if (typeStr.contains("[")) {
                typeStr = StringUtils.substringBefore(typeStr, "[");
            }
            detectedClass = getClassFromName(typeStr);
        }
        if (name.endsWith("]")) {
            isArray = true;
            String delimiter = StringUtils.substringBetween(name, "[", "]");
            if (!StringUtils.isEmpty(delimiter)) {
                String colName = convertHeaderName(name);
                delimiters.put(colName, delimiter);
            }
        }
        if (isArray) {
            return getArrayType(Optional.of(detectedClass));
        } else {
            return Optional.of(detectedClass);
        }
    }

    private Class getClassFromName(String typeStr) {
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
                return Calendar.class;
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
     * Consider if a column is seen twice then that column type should be
     * considered an array. Because String is a default assumption when no type
     * is specified, any redefinition of a column to a more specific type will
     * be then assumed for that property altogether.
     *
     * @param a
     * @param b
     * @return
     */
    private Optional<Class> upgradeToArray(Optional<Class> a, Optional<Class> b) {
        if (!a.isPresent()) {
            return b;
        }
        if (!b.isPresent()) {
            return a;
        }
        if (a.get().equals(b.get()) || b.get() == Object.class) {
            return getArrayType(a);
        } else {
            return getArrayType(b);
        }
    }

    private static Optional<Class> getArrayType(Optional<Class> clazz) {
        if (!clazz.isPresent()) {
            return Optional.empty();
        } else if (clazz.get().isArray()) {
            return clazz;
        } else {
            return Optional.of(Array.newInstance(clazz.get(), 0).getClass());
        }
    }
}
