/*
 * Copyright 2018 Adobe.
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
package com.adobe.acs.commons.mcp.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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

    private String fileName = "unknown";
    private int rowCount;
    private transient List<String> headerRow;
    private transient List<Map<String, String>> dataRows;
    private final List<String> requiredColumns;

    public Spreadsheet(InputStream file, String... required) throws IOException {
        if (required == null || required.length == 0) {
            requiredColumns = Collections.EMPTY_LIST;
        } else {
            requiredColumns = Arrays.asList(required);
        }
        parseInputFile(file);
    }    

    public Spreadsheet(RequestParameter file, String... required) throws IOException {
        this(file.getInputStream(), required);
        fileName = file.getFileName();
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

        headerRow = readRow(rows.next()).stream()
                .map(s -> String.valueOf(s))
                .map(String::toLowerCase)
                .map(s -> s.replaceAll("[^0-9a-zA-Z:]+", "_"))
                .collect(Collectors.toList());

        Iterable<Row> remainingRows = () -> rows;
        dataRows = StreamSupport.stream(remainingRows.spliterator(), false)
                .map(this::buildRow)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<String> readRow(Row row) {
        Iterator<Cell> iterator = row.cellIterator();
        List<String> rowOut = new ArrayList<>();
        while (iterator.hasNext()) {
            Cell c = iterator.next();
            while (c.getColumnIndex() > rowOut.size()) {
                rowOut.add(null);
            }
            rowOut.add(getStringValueFromCell(c));
        }
        return rowOut;
    }

    private String getStringValueFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        int cellType = cell.getCellType();
        if (cellType == Cell.CELL_TYPE_FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        switch (cellType) {
            case Cell.CELL_TYPE_BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_BLANK:
                return null;
            case Cell.CELL_TYPE_NUMERIC:
                double number = cell.getNumericCellValue();
                if (Math.floor(number) == number) {
                    return Integer.toString((int) number);
                } else {
                    return Double.toString(cell.getNumericCellValue());
                }
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            default:
                return "???";
        }
    }

    private Optional<Map<String, String>> buildRow(Row row) {
        Map<String, String> out = new LinkedHashMap<>();
        List<String> data = readRow(row);
        boolean empty = true;
        for (int i = 0; i < data.size() && i < getHeaderRow().size(); i++) {
            if (data.get(i) != null && !data.get(i).trim().isEmpty()) {
                empty = false;
                out.put(getHeaderRow().get(i), data.get(i));
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
    public List<Map<String, String>> getDataRows() {
        return dataRows;
    }

    /**
     * @return the requiredColumns
     */
    public List<String> getRequiredColumns() {
        return requiredColumns;
    }
}
