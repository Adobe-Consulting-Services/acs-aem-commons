/*
 * Copyright 2017 Adobe.
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
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.impl.BasicResourceDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

/**
 * Creates Asset Folder definitions (node and Title) based on a well defined Excel document.
 */
public class AssetFolderCreator extends ProcessDefinition implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(AssetFolderCreator.class);
    private static final long serialVersionUID = 4393712954263547160L;

    public static final String NAME = "Asset Folder Creator";

    private final Map<String, ResourceDefinitionBuilder> resourceDefinitionBuilders;

    public enum AssetFolderBuilder {
        TITLE_TO_NODE_NAME,
        TITLE_AND_NODE_NAME,
        LOWERCASE_WITH_DASHES,
        NONE
    };

    enum FolderType {
        UNORDERED_FOLDER,
        ORDERED_FOLDER
    }

    public AssetFolderCreator(Map<String, ResourceDefinitionBuilder> resourceDefinitionBuilders) {
        this.resourceDefinitionBuilders = resourceDefinitionBuilders;
    }

    @FormField(
            name = "Excel File",
            description = "Provide the .xlsx file that defines the Asset Folder taxonomy",
            component = FileUploadComponent.class,
            options = {"mimeTypes=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "required"}
    )
    public InputStream excelFile = null;

    @FormField(
            name = "Folder Type",
            description = "",
            component = SelectComponent.EnumerationSelector.class,
            options = {"default=UNORDERED_FOLDER", "required"}
    )
    public FolderType assetFolderType = FolderType.UNORDERED_FOLDER;

    @FormField(
            name = "Primary Creator",
            description = "This will be used first and the Fallback will only be used if this fails to generate a valid Asset Folder definition.",
            component = SelectComponent.EnumerationSelector.class,
            options = {"default=TITLE_AND_NODE_NAME", "required"}
    )
    public AssetFolderBuilder primary = AssetFolderBuilder.TITLE_AND_NODE_NAME;

    @FormField(
            name = "Fallback Creator",
            description = "This is only invoked when the Primary cannot generate a valid Asset Folder definition. If this can also not generate a valid Asset Folder definition then the row will be skipped.",
            component = SelectComponent.EnumerationSelector.class,
            options = {"default=LOWERCASE_WITH_DASHES", "required"}
    )
    public AssetFolderBuilder fallback = AssetFolderBuilder.LOWERCASE_WITH_DASHES;

    @Override
    public void init() throws RepositoryException {

    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        report.setName(instance.getName());
        instance.getInfo().setDescription(String.format("Create Asset Folders using [ %s / %s ]", StringUtil.getFriendlyName(primary.name()), StringUtil.getFriendlyName(fallback.name())));

        instance.defineCriticalAction("Parse Asset Folder definitions", rr, this::parseAssetFolderDefinitions);
        instance.defineCriticalAction("Create Asset Folders", rr, this::createAssetFolders);
    }

    volatile HashMap<String, AssetFolderDefinition> assetFolderDefinitions = new LinkedHashMap<>();

    /**
     * Parses the input Excel file and creates a list of AssetFolderDefinition objects to process.
     *
     * @param manager the action manager
     * @throws IOException
     */
    public void parseAssetFolderDefinitions(ActionManager manager) throws Exception {
        manager.withResolver(rr -> {
            final XSSFWorkbook workbook = new XSSFWorkbook(excelFile);
            // Close the inputstream to prevent resource leakage
            excelFile.close();

            final XSSFSheet sheet = workbook.getSheetAt(0);
            final Iterator<Row> rows = sheet.rowIterator();

            while(rows.hasNext()) {
                final Row row = rows.next();
                final Iterator<Cell> cells = row.cellIterator();

                // The previousAssetFolderPath is reset on each new row.
                String previousAssetFolderPath = null;

                while (cells.hasNext()) {
                    final Cell cell = cells.next();

                    final String cellValue = StringUtils.trimToNull(cell.getStringCellValue());
                    if (StringUtils.isBlank(cellValue)) {
                        // Hitting a blank cell means its the end of this row; don't process anything past this
                        break;
                    }

                    // Generate a asset folder definition that will in turn be used to drive the asset folder definition creation
                    AssetFolderDefinition assetFolderDefinition = getAssetFolderDefinition(primary, cellValue, previousAssetFolderPath);

                    if (assetFolderDefinition == null) {
                        assetFolderDefinition = getAssetFolderDefinition(fallback, cellValue, previousAssetFolderPath);
                    }

                    if (assetFolderDefinition == null) {
                        log.warn("Could not find a Asset Folder Converter that accepts value [ {} ]; skipping...", cellValue);
                        // Record parse failure
                        record(ReportRowSatus.FAILED_TO_PARSE, "", cellValue);
                        // Break to next Row
                        break;
                    } else {
                        /* Prepare for next Cell */
                        previousAssetFolderPath = assetFolderDefinition.getPath();

                        if (assetFolderDefinitions.get(assetFolderDefinition.getId()) == null) {
                            assetFolderDefinitions.put(assetFolderDefinition.getId(), assetFolderDefinition);
                        }
                    }
                }
            };
            log.info("Finished Parsing and collected [ {} ] asset folders for creation.", assetFolderDefinitions.size());
        });
    }

    /**
     * Perform the asset folder creation based on the successfully parsed values in parseAssetFolderDefinitions(..).
     *
     * @param manager the action manager
     */
    public void createAssetFolders(ActionManager manager) {
        assetFolderDefinitions.values().stream().forEach(assetFolderDefinition -> {
            try {
                manager.withResolver(rr -> {
                    ReportRowSatus status;

                    try {
                        Resource folder = rr.getResource(assetFolderDefinition.getPath());
                        if (folder == null) {
                            final Map<String, Object> folderProperties = new HashMap<>();
                            folderProperties.put(JcrConstants.JCR_PRIMARYTYPE, assetFolderDefinition.getNodeType());
                            folder = rr.create(rr.getResource(assetFolderDefinition.getParentPath()),
                                    assetFolderDefinition.getName(),
                                    folderProperties);

                            status = ReportRowSatus.CREATED;
                        } else {
                            status = ReportRowSatus.UPDATED_FOLDER_TITLES;
                        }

                        Resource jcrContent = folder.getChild(JcrConstants.JCR_CONTENT);
                        if (jcrContent == null) {
                            final Map<String, Object> jcrContentProperties = new HashMap<>();
                            jcrContentProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                            rr.create(folder, JcrConstants.JCR_CONTENT, jcrContentProperties);
                        }

                        setTitles(folder, assetFolderDefinition);
                        record(status, assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());
                        log.debug("Created Asset Folder [ {} -> {} ]", assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());
                    } catch (Exception e) {
                        record(ReportRowSatus.FAILED_TO_CREATE, assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());
                        log.error("Unable to create Asset Folder [ {} -> {} ]", assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());
                    }
                });
            } catch (Exception e) {
                log.error("Unable to import asset folders via ACS Commons MCP - Asset Folder Creator", e);
            }
        });
    }

    /**
     * Generates the Asset Folder Definition.
     * @param assetFolderBuilder The asset folder builder to use to construct the AssetFolderDefinition.
     * @param value The value to convert into a AssetFolderDefinition.
     * @param previousAssetFolderPath The previous Asset Folder processed
     * @return a valid AssetFolderDefinition, or null if a valid AssetFolderDefinition cannot be generated.
     */
    private AssetFolderDefinition getAssetFolderDefinition(final AssetFolderBuilder assetFolderBuilder, final String value, String previousAssetFolderPath) {
        final ResourceDefinitionBuilder resourceDefinitionBuilder = resourceDefinitionBuilders.get(assetFolderBuilder.name());

        if (resourceDefinitionBuilder != null && resourceDefinitionBuilder.accepts(value)) {
            return new AssetFolderDefinition(resourceDefinitionBuilder.convert(value), previousAssetFolderPath, assetFolderType);
        }

        return null;
    }

    private void setTitles(final Resource folder, final AssetFolderDefinition assetFolderDefinition) throws RepositoryException {
        if (folder == null) {
            log.error("Asset Folder resource [ {} ] is null", assetFolderDefinition.getPath());
            return;
        }

        Resource jcrContent = folder.getChild(JcrConstants.JCR_CONTENT);
        if (jcrContent == null) {
            log.error("Asset Folder [ {} ] does not have a jcr:content child", assetFolderDefinition.getPath());
            return;
        }

        final ModifiableValueMap properties = jcrContent.adaptTo(ModifiableValueMap.class);

        if (!StringUtils.equals(assetFolderDefinition.getTitle(), properties.get(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, String.class))) {
            // Ensure if the asset folder definition already exists that the title is set properly
            properties.put(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, assetFolderDefinition.getTitle());
        }
    }

    /** Reporting **/

    transient private final GenericReport report = new GenericReport();

    private final ArrayList<EnumMap<ReportColumns, Object>> reportRows = new ArrayList<>();

    private enum ReportColumns {
        STATUS,
        ASSET_FOLDER_PATH,
        ASSET_FOLDER_TITLE
    }

    public enum ReportRowSatus {
        CREATED,
        UPDATED_FOLDER_TITLES,
        FAILED_TO_PARSE,
        FAILED_TO_CREATE,
    };

    private void record(ReportRowSatus status, String path, String title) {
        final EnumMap<ReportColumns, Object> row = new EnumMap<>(ReportColumns.class);

        row.put(ReportColumns.STATUS, StringUtil.getFriendlyName(status.name()));
        row.put(ReportColumns.ASSET_FOLDER_PATH, path);
        row.put(ReportColumns.ASSET_FOLDER_TITLE, title );

        reportRows.add(row);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        report.setRows(reportRows, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    /** Asset Folder Definition Class **/

    protected static final class AssetFolderDefinition extends BasicResourceDefinition {
        private static final String ASSET_ROOT_PATH = "/content/dam";
        private String parentPath = null;
        private FolderType folderType;

        public AssetFolderDefinition(ResourceDefinition resourceDefinition, String parentPath, FolderType folderType) {
            super(resourceDefinition.getName());
            super.setTitle(resourceDefinition.getTitle());
            this.folderType = folderType;
            this.parentPath = StringUtils.defaultIfBlank(parentPath, ASSET_ROOT_PATH);
            super.setPath(this.parentPath + "/" + resourceDefinition.getName());
        }

        public String getId() {
            // The Id of the Asset Folder Definition IS the path of the asset folder to create
            return getPath();
        }

        public String getParentPath() {
            return this.parentPath;
        }

        public String getNodeType() {
            if (FolderType.ORDERED_FOLDER.equals(folderType)) {
                return JcrResourceConstants.NT_SLING_ORDERED_FOLDER;
            } else {
                return JcrResourceConstants.NT_SLING_FOLDER;
            }
        }
    }
}
