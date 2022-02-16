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

import com.adobe.acs.commons.data.CompositeVariant;
import com.adobe.acs.commons.data.Spreadsheet;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.day.crx.JcrConstants;
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

import static com.adobe.acs.commons.data.Spreadsheet.ROW_NUMBER;
import static javax.jcr.Property.JCR_PRIMARY_TYPE;

/**
 * Read node and metadata from a spreadsheet and update underlying node storage
 * with provided data.
 */
public class DataImporter extends ProcessDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(DataImporter.class);
    private static final String PATH = "path";
    private static final String SLASH = "/";

    public enum MergeMode {
        CREATE_NODES_AND_OVERWRITE_PROPERTIES(true, true, true, false),
        CREATE_NODES_AND_OVERWRITE_PROPERTIES_AND_APPEND_ARRAYS(true, true, true, true),
        CREATE_NODES_AND_MERGE_PROPERTIES(true, true, false, false),
        CREATE_ONLY_SKIP_EXISTING(true, false, false, false),
        OVERWRITE_EXISTING_ONLY(false, true, true, false),
        OVERWRITE_EXISTING_ONLY_AND_APPEND_ARRAYS(false, true, true, true),
        MERGE_EXISTING_ONLY(false, true, false, false),
        DO_NOTHING(false, false, false, false);

        boolean create = false;
        boolean update = false;
        boolean overwriteProps = false; // Note that this is moot if update is false
        boolean appendArrays = false; // Note that this is moot if update is false

        MergeMode(boolean c, boolean u, boolean o, boolean a) {
            create = c;
            update = u;
            overwriteProps = o;
            appendArrays = a;
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
    private MergeMode mergeMode = MergeMode.CREATE_NODES_AND_OVERWRITE_PROPERTIES;

    @FormField(
            name = "Structure node type",
            description = "Type assigned to new nodes (ignored if spreadsheet has a jcr:primaryType column) -- for ordered folders use sling:OrderedFolder",
            options = {"default=sling:Folder"}
    )
    private String defaultNodeType = "sling:Folder";

    @FormField(
            name = "Include jcr:content nodes",
            description = "If checked, jcr:content nodes are created/updated under nodes",
            component = CheckboxComponent.class
    )
    private boolean includeJcrContent = false;

    @FormField(
            name = "jcr:content node type",
            description = "Type assigned to new jcr:content child nodes (ignored if spreadsheet has a jcr:content/jcr:primaryType column)",
            options = {"default=nt:unstructured"}
    )
    private String defaultJcrContentType = "nt:unstructured";

    @FormField(
            name = "Convert header names",
            description = "If checked, property names in the header are converted to lower-case and non-compatible characters are converted to underscores",
            component = CheckboxComponent.class
    )
    private boolean enableHeaderNameConversion = false;

    @FormField(
            name = "Dry run",
            description = "If checked, no import happens.  Useful for data validation",
            component = CheckboxComponent.class,
            options = "checked"
    )
    boolean dryRunMode = true;

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

    private EnumMap<ReportColumns, Object> createdNodes
            = trackActivity(TOTAL, "Create", 0);
    private EnumMap<ReportColumns, Object> updatedNodes
            = trackActivity(TOTAL, "Updated", 0);
    private EnumMap<ReportColumns, Object> skippedNodes
            = trackActivity(TOTAL, "Skipped", 0);
    private EnumMap<ReportColumns, Object> noChangeNodes
            = trackActivity(TOTAL, "No Change", 0);

    @SuppressWarnings("squid:S00115")
    public static enum ReportColumns {
        item, action, count
    }

    Spreadsheet data;
    private List<EnumMap<ReportColumns, Object>> reportRows;

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
        if (data == null && importFile != null) {
            try {
                data = new Spreadsheet(enableHeaderNameConversion, importFile, PATH).buildSpreadsheet();
                if (presortData) {
                    Collections.sort(data.getDataRowsAsCompositeVariants(), (a, b) -> b.get(PATH).toString().compareTo(a.get(PATH).toString()));
                }
                instance.getInfo().setDescription("Import " + data.getFileName() + " (" + data.getRowCount() + " rows)");
            } catch (IOException ex) {
                LOG.error("Unable to process import", ex);
                instance.getInfo().setDescription("Import " + data.getFileName() + " (failed)");
                throw new RepositoryException("Unable to parse input file", ex);
            }
        }
        instance.defineCriticalAction("Import Data", rr, this::importData);
    }

    private transient GenericBlobReport report = new GenericBlobReport();

    @Override
    public synchronized void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
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
                    updateMetadata(path, rr, row);
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

    /**
     * Create missing node at the given path with the properties from the passed row.
     * If properties are pre-appended with "jcr:content/", create jcr:content node.
     *
     * @param path Path of node.
     * @param rr   ResourceResolver.
     * @param row  Row from XLSX file.
     * @throws PersistenceException PersistenceException
     */
    private void createMissingNode(String path, ResourceResolver rr, Map<String, CompositeVariant> row) throws PersistenceException {
        LOG.debug("Start of createMissingNode for node {}", path);

        String parentPath = StringUtils.substringBeforeLast(path, SLASH);
        Map<String, Object> resourceProperties = new HashMap<>();
        resourceProperties.put(JcrConstants.JCR_PRIMARYTYPE, defaultNodeType);
        Resource parent = ResourceUtil.getOrCreateResource(rr, parentPath, resourceProperties, defaultNodeType, true);

        String nodeName = StringUtils.substringAfterLast(path, SLASH);
        if (!row.containsKey(JCR_PRIMARY_TYPE) && !row.containsKey(JcrConstants.JCR_PRIMARYTYPE)) {
            row.put(JcrConstants.JCR_PRIMARYTYPE, new CompositeVariant(defaultNodeType));
        }
        Map<String, Object> nodeProps = createPropertyMap(row);
        rr.refresh();
        Resource main = rr.create(parent, nodeName, nodeProps);

        if (includeJcrContent) {
            if (!row.containsKey(JcrConstants.JCR_CONTENT + SLASH + JcrConstants.JCR_PRIMARYTYPE)) {
                row.put(JcrConstants.JCR_CONTENT + SLASH + JcrConstants.JCR_PRIMARYTYPE, new CompositeVariant(defaultJcrContentType));
            }
            Map<String, Object> jcrContentProps = createJcrContentPropertyMap(row);
            if (!jcrContentProps.isEmpty()) {
                rr.create(main, JcrConstants.JCR_CONTENT, jcrContentProps);
            }
        }

        LOG.debug("End of createMissingNode for node {}", path);
    }

    /**
     * Get the ModifiableValueMap of the resource and update with the properties from the row.
     * If jcr:content/jcr:primaryType is provided, get the jcr:content resource and update.
     *
     * @param path     Path of node.
     * @param rr       ResourceResolver
     * @param nodeInfo Map of properties from the row.
     * @throws PersistenceException PersistenceException
     */
    private void updateMetadata(String path, ResourceResolver rr, Map<String, CompositeVariant> nodeInfo) throws PersistenceException, RepositoryException {
        LOG.debug("Start of updateMetaData");

        Resource resource = rr.getResource(path);

        populateMetadataFromRow(resource, createPropertyMap(nodeInfo));

        if (includeJcrContent) {
            Map<String, Object> jcrContentProps = createJcrContentPropertyMap(nodeInfo);
            Resource jcrContent = resource.getChild(JcrConstants.JCR_CONTENT);
            if (jcrContent == null) {
                if (!jcrContentProps.containsKey(JcrConstants.JCR_CONTENT + SLASH + JcrConstants.JCR_PRIMARYTYPE)) {
                    jcrContentProps.put(JcrConstants.JCR_PRIMARYTYPE, defaultJcrContentType);
                }
                rr.create(resource, JcrConstants.JCR_CONTENT, jcrContentProps);
            } else {
                populateMetadataFromRow(jcrContent, jcrContentProps);
            }
        }

        if (rr.hasChanges()) {
            incrementCount(updatedNodes, 1);
            if (detailedReport) {
                trackActivity(path, "Updated Properties", null);
            }
            if (!dryRunMode) {
                rr.commit();
            }
            rr.revert();
            rr.refresh();
        } else {
            if (detailedReport) {
                trackActivity(path, "No Change", null);
            }
            incrementCount(noChangeNodes, 1);
        }

        LOG.debug("End of updateMetadata");
    }

    /**
     * Append the new values to the already existing values in the array
     *
     * @param resourceProperties ModifiableValueMap of resource.
     * @param entry Key-value pair of property from the row
     */
    private void appendArray(ModifiableValueMap resourceProperties, Map.Entry entry) {
        Object[] currentArray = resourceProperties.get((String) entry.getKey(), Object[].class);
        ArrayList<Object> currentList = new ArrayList<>(Arrays.asList(currentArray));
        currentList.addAll(Arrays.asList((Object[]) entry.getValue()));
        resourceProperties.put((String) entry.getKey(), currentList.toArray());
    }

    /**
     * Update the resource with the properties from the row.
     *
     * @param resource Resource object of which the properties are to be modified.
     * @param nodeInfo Map of properties from the row.
     */
    private void populateMetadataFromRow(Resource resource, Map<String, Object> nodeInfo) throws RepositoryException {
        LOG.debug("Start of populateMetadataFromRow");

        ModifiableValueMap resourceProperties = resource.adaptTo(ModifiableValueMap.class);
        Node node = resource.adaptTo(Node.class);
        for (Map.Entry entry : nodeInfo.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();

            if (key != null && (mergeMode.overwriteProps || !resourceProperties.containsKey(key))) {
                if (node.hasProperty(key) && node.getProperty(key).isMultiple() && mergeMode.appendArrays) {
                    appendArray(resourceProperties, entry);
                } else if (value != null) {
                    resourceProperties.put(key, value);
                }
            }
        }

        LOG.debug("End of populateMetadataFromRow");
    }

    /**
     * Create map of properties for node.
     *
     * @param row Row of data from XLSX.
     * @return Map of property names and values.
     */
    private Map<String, Object> createPropertyMap(Map<String, CompositeVariant> row) {
        return row.entrySet().stream()
                .filter(e -> !e.getKey().equals(ROW_NUMBER) && !e.getKey().equals(PATH) && e.getValue() != null && !e.getKey().contains(SLASH))
                .collect(
                        Collectors.toMap(
                                e -> e.getKey(),
                                e -> e.getValue().toPropertyValue()
                        )
                );
    }

    /**
     * Create map of properties for jcr:content node.
     *
     * @param row Row of data from XLSX.
     * @return Map of property names and values.
     */
    private Map<String, Object> createJcrContentPropertyMap(Map<String, CompositeVariant> row) {
        return row.entrySet().stream()
                .filter(e -> e.getKey().startsWith(JcrConstants.JCR_CONTENT))
                .collect(
                        Collectors.toMap(
                                e -> e.getKey().replace(JcrConstants.JCR_CONTENT + SLASH, ""),
                                e -> e.getValue().toPropertyValue()
                        )
                );
    }
}
