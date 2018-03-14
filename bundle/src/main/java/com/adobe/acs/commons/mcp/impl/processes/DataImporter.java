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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import static javax.jcr.Property.JCR_PRIMARY_TYPE;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read node and metadata from a spreadsheet and update underlying node storage with provided data.
 */
public class DataImporter extends ProcessDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(DataImporter.class);
    private static final String PATH = "path";

    public enum MergeMode {
        create_and_overwrite_all(true, true, true),
        merge_only(true, true, false),
        create_missing_no_merge(true, false, false),
        overwrite_existing(false, true, true),
        update_existing(false, true, false),
        do_nothing(false, false, false);

        boolean create = false;
        boolean update = false;
        boolean overwriteProps = false; // Note that this is moot if update is false

        MergeMode(boolean c, boolean u, boolean o) {
            create = c;
            update = u;
            overwriteProps = o;
        }
    };

    @FormField(
            name = "Import data file",
            description = "Data file containing import data",
            component = FileUploadComponent.class,
            required = true
    )
    private transient RequestParameter importFile;

    @FormField(
            name = "Existing action",
            description = "What to do if an asset exists",
            component = RadioComponent.EnumerationSelector.class,
            options = {"default=create_and_overwrite_all", "vertical"}
    )
    private MergeMode mergeMode = MergeMode.create_and_overwrite_all;

    @FormField(
            name = "Structure node type",
            description = "Type assigned to missing nodes (unless specified via jcr:primayType column)",
            options = {"default=sling:Folder"}
    )
    private String defaultNodeType = "sling:Folder";

    @FormField(
            name = "Dry run",
            description = "If checked, no import happens.  Useful for data validation",
            component = CheckboxComponent.class
    )
    private boolean dryRunMode = false;

    @FormField(
            name = "Detailed report",
            description = "If checked, information about every asset is recorded",
            component = CheckboxComponent.class,
            options = "checked"
    )
    private boolean detailedReport = true;

    private String fileName;
    private int rowCount;
    transient List<String> fileHeader;
    transient List<Map<String, String>> nodeData;

    EnumMap<ReportColumns, Object> createdNodes
            = trackActivity("Total", "Create", 0);
    EnumMap<ReportColumns, Object> updatedNodes
            = trackActivity("Total", "Updated", 0);
    EnumMap<ReportColumns, Object> skippedNodes
            = trackActivity("Total", "Skipped", 0);
    EnumMap<ReportColumns, Object> noChangeNodes
            = trackActivity("Total", "No Change", 0);

    @SuppressWarnings("squid:S00115")
    public static enum ReportColumns {
        item, action, count
    }

    List<EnumMap<ReportColumns, Object>> reportRows;

    protected synchronized EnumMap<ReportColumns, Object> trackActivity(String item, String action, Integer count) {
        if (reportRows == null) {
            reportRows = Collections.synchronizedList(new ArrayList<>());
        }
        EnumMap<ReportColumns, Object> reportRow = new EnumMap<>(ReportColumns.class);
        reportRow.put(ReportColumns.item, item);
        reportRow.put(ReportColumns.action, action);
        reportRow.put(ReportColumns.count, count);
        reportRows.add(reportRow);
        return reportRow;
    }

    protected void incrementCount(EnumMap<ReportColumns, Object> row, int amt) {
        synchronized (row) {
            row.put(ReportColumns.count, (int) row.getOrDefault(ReportColumns.count, 0) + amt);
        }
    }

    @Override
    public void init() throws RepositoryException {
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        try {
            parseInputFile();
            instance.getInfo().setDescription("Import " + fileName + " (" + rowCount + " rows)");
        } catch (IOException ex) {
            LOG.error("Unable to process import", ex);
            instance.getInfo().setDescription("Import " + fileName + " (failed)");
            throw new RepositoryException("Unable to parse input file", ex);
        }
        instance.defineCriticalAction("Import Data", rr, this::importData);
    }

    transient private GenericReport report = new GenericReport();

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        report.setRows(reportRows, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    private void importData(ActionManager manager) {
        nodeData.forEach((row) -> {
            manager.deferredWithResolver(rr -> {
                String path = row.get(PATH);
                Resource r = rr.getResource(path);
                if (r == null) {
                    if (mergeMode.create) {
                        if (!dryRunMode) {
                            String parentPath = StringUtils.substringBeforeLast(path, "/");
                            Resource parent = ResourceUtil.getOrCreateResource(rr, parentPath, defaultNodeType, defaultNodeType, true);
                            String nodeName = StringUtils.substringAfterLast(path, "/");
                            if (!row.containsKey(JCR_PRIMARY_TYPE)) {
                                row.put("JCR_TYPE", defaultNodeType);
                            }
                            Map<String, Object> nodeProps = new HashMap(row);
                            rr.refresh();
                            rr.create(parent, nodeName, nodeProps);
                        }
                        incrementCount(createdNodes, 1);
                        if (detailedReport) {
                            trackActivity(path, "Created", null);
                        }
                    } else {
                        incrementCount(skippedNodes, 1);
                        if (detailedReport) {
                            trackActivity(path, "Skipped missing", null);
                        }
                    }
                } else if (mergeMode.update) {
                    updateMetadata(rr, row);
                } else {
                    incrementCount(skippedNodes, 1);
                    if (detailedReport) {
                        trackActivity(path, "Skipped", null);
                    }
                }
            });
        });
    }

    /**
     * Parse out the input file synchronously for easier unit test validation
     *
     * @return List of files that will be imported, including any renditions
     * @throws IOException if the file couldn't be read
     */
    private void parseInputFile() throws IOException {
        try (InputStream binaryData = importFile.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(binaryData);

            final XSSFSheet sheet = workbook.getSheetAt(0);
            fileName = importFile.getFileName();
            rowCount = sheet.getLastRowNum();
            final Iterator<Row> rows = sheet.rowIterator();

            fileHeader = readRow(rows.next()).stream()
                    .map(s -> String.valueOf(s))
                    .map(String::toLowerCase)
                    .map(s -> s.replaceAll("[^0-9a-zA-Z:]+", "_"))
                    .collect(Collectors.toList());

            Iterable<Row> remainingRows = () -> rows;
            nodeData = StreamSupport.stream(remainingRows.spliterator(), false)
                    .map(this::buildRow)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            Collections.sort(nodeData, (a, b) -> a.get(PATH).compareTo(b.get(PATH)));
        }
    }

    private void updateMetadata(ResourceResolver rr, Map<String, String> nodeInfo) throws PersistenceException {
        ModifiableValueMap resourceProperties = rr.getResource(nodeInfo.get(PATH)).adaptTo(ModifiableValueMap.class);
        for (String prop : fileHeader) {
            if (!prop.equals(PATH)
                    && (mergeMode.overwriteProps || !resourceProperties.containsKey(prop))) {
                String value = nodeInfo.get(prop);
                if (value == null) {
                    nodeInfo.remove(prop);
                } else {
                    if (value.contains(",")) {
                        resourceProperties.put(prop, value.split(","));
                    } else {
                        resourceProperties.put(prop, value);
                    }
                }
            }
        }
        if (rr.hasChanges()) {
            incrementCount(updatedNodes, 1);
            if (detailedReport) {
                trackActivity(nodeInfo.get(PATH), "Updated Properties", null);
            }
            if (!dryRunMode) {
                rr.commit();
            }
            rr.refresh();
        } else {
            if (detailedReport) {
                trackActivity(nodeInfo.get(PATH), "No Change", null);
            }
            incrementCount(noChangeNodes, 1);
        }
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
        for (int i = 0; i < data.size() && i < fileHeader.size(); i++) {
            if (data.get(i) != null && !data.get(i).trim().isEmpty()) {
                empty = false;
                out.put(fileHeader.get(i), data.get(i));
            }
        }
        if (empty || !out.containsKey(PATH)) {
            return Optional.empty();
        } else {
            return Optional.of(out);
        }
    }
}
