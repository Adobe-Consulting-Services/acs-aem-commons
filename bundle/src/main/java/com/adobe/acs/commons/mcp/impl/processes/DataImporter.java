/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.data.CompositeVariant;
import com.adobe.acs.commons.data.Spreadsheet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.Property.JCR_PRIMARY_TYPE;

/**
 * Read node and metadata from a spreadsheet and update underlying node storage with provided data.
 */
public class DataImporter extends ProcessDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(DataImporter.class);
    private static final String PATH = "path";

    public enum MergeMode {
        CREATE_AND_OVERWRITE_PROPERTIES(true, true, true),
        CREATE_AND_MERGE_PROPERTIES(true, true, false),
        CREATE_ONLY_SKIP_EXISTING(true, false, false),
        OVERWRITE_EXISTING_ONLY(false, true, true),
        MERGE_EXISTING_ONLY(false, true, false),
        DO_NOTHING(false, false, false);

        boolean create = false;
        boolean update = false;
        boolean overwriteProps = false; // Note that this is moot if update is false

        MergeMode(boolean c, boolean u, boolean o) {
            create = c;
            update = u;
            overwriteProps = o;
        }
    }

    @FormField(
            name = "Excel File",
            description = "Provide the .xlsx file that defines the nodes being imported",
            component = FileUploadComponent.class,
            options = {"mimeTypes=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "required"}
    )
    private transient RequestParameter importFile;

    @FormField(
            name = "Existing action",
            description = "What to do if an asset exists",
            component = RadioComponent.EnumerationSelector.class,
            options = {"default=create_and_overwrite_properties", "vertical"}
    )
    private MergeMode mergeMode = MergeMode.CREATE_AND_OVERWRITE_PROPERTIES;

    @FormField(
            name = "Structure node type",
            description = "Type assigned to new nodes (ignored if spreadsheet has a jcr:primayType column) -- for ordered folders use sling:OrderedFolder",
            options = {"default=sling:Folder"}
    )
    private String defaultNodeType = "sling:Folder";
    
    @FormField(
            name = "Convert header names",
            description = "If checked, property names in the header are converted to lower-case and non-compatible characters are converted to underscores",
            component = CheckboxComponent.class
    )
    private boolean enableHeaderNameConversion = false;    

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

    @FormField(
            name = "Import in sorted order",
            description = "If checked, nodes will be imported in the order determined by their paths",
            component = CheckboxComponent.class,
            options = "checked"
    )
    private boolean presortData = true;
    public static final String TOTAL = "Total";

    EnumMap<ReportColumns, Object> createdNodes
            = trackActivity(TOTAL, "Create", 0);
    EnumMap<ReportColumns, Object> updatedNodes
            = trackActivity(TOTAL, "Updated", 0);
    EnumMap<ReportColumns, Object> skippedNodes
            = trackActivity(TOTAL, "Skipped", 0);
    EnumMap<ReportColumns, Object> noChangeNodes
            = trackActivity(TOTAL, "No Change", 0);

    @SuppressWarnings("squid:S00115")  
    public static enum ReportColumns {
        item, action, count
    }

    Spreadsheet data;
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

    @SuppressWarnings("squid:S2445")
    protected void incrementCount(EnumMap<ReportColumns, Object> row, int amt) {
        synchronized (row) {
            row.put(ReportColumns.count, (int) row.getOrDefault(ReportColumns.count, 0) + amt);
        }
    }

    @Override
    public void init() throws RepositoryException {
        // Nothing to do here
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        try {
            data = new Spreadsheet(enableHeaderNameConversion, importFile, PATH);
            if (presortData) {
                Collections.sort(data.getDataRowsAsCompositeVariants(), (a, b) -> b.get(PATH).toString().compareTo(a.get(PATH).toString()));
            }
            instance.getInfo().setDescription("Import " + data.getFileName() + " (" + data.getRowCount() + " rows)");
        } catch (IOException ex) {
            LOG.error("Unable to process import", ex);
            instance.getInfo().setDescription("Import " + data.getFileName() + " (failed)");
            throw new RepositoryException("Unable to parse input file", ex);
        }
        instance.defineCriticalAction("Import Data", rr, this::importData);
    }

    private transient GenericReport report = new GenericReport();

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        report.setRows(reportRows, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    private void importData(ActionManager manager) {
        data.getDataRowsAsCompositeVariants().forEach((row) -> {
            manager.deferredWithResolver(rr -> {
                String path = row.get(PATH).toString();
                Resource r = rr.getResource(path);
                if (r == null) {
                    handleMissingNode(path, rr, row);
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

    public void handleMissingNode(String path, ResourceResolver rr, Map<String, CompositeVariant> row) throws PersistenceException {
        if (mergeMode.create) {
            if (!dryRunMode) {
                createMissingNode(path, rr, row);
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
    }

    public void createMissingNode(String path, ResourceResolver rr, Map<String, CompositeVariant> row) throws PersistenceException {
        String parentPath = StringUtils.substringBeforeLast(path, "/");
        Resource parent = ResourceUtil.getOrCreateResource(rr, parentPath, defaultNodeType, defaultNodeType, true);
        String nodeName = StringUtils.substringAfterLast(path, "/");
        if (!row.containsKey(JCR_PRIMARY_TYPE)) {
            row.put("JCR_TYPE", new CompositeVariant(defaultNodeType));
        }
        Map<String, Object> nodeProps = new HashMap(row);
        rr.refresh();
        rr.create(parent, nodeName, nodeProps);
    }

    private void updateMetadata(ResourceResolver rr, Map<String, CompositeVariant> nodeInfo) throws PersistenceException {
        ModifiableValueMap resourceProperties = rr.getResource(nodeInfo.get(PATH).toString()).adaptTo(ModifiableValueMap.class);
        populateMetadataFromRow(resourceProperties, nodeInfo);
        if (rr.hasChanges()) {
            incrementCount(updatedNodes, 1);
            if (detailedReport) {
                trackActivity(nodeInfo.get(PATH).toString(), "Updated Properties", null);
            }
            if (!dryRunMode) {
                rr.commit();
            }
            rr.refresh();
        } else {
            if (detailedReport) {
                trackActivity(nodeInfo.get(PATH).toString(), "No Change", null);
            }
            incrementCount(noChangeNodes, 1);
        }
    }

    public void populateMetadataFromRow(ModifiableValueMap resourceProperties, Map<String, CompositeVariant> nodeInfo) {
        for (String prop : data.getHeaderRow()) {
            if (!prop.equals(PATH)
                    && (mergeMode.overwriteProps || !resourceProperties.containsKey(prop))) {
                CompositeVariant value = nodeInfo.get(prop);
                if (value == null || value.isEmpty()) {
                    nodeInfo.remove(prop);
                } else {
                    resourceProperties.put(prop, value.toPropertyValue());
                }
            }
        }
    }
}
