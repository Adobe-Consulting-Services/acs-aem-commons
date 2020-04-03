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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.data.Variant;
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
import com.day.cq.dam.api.DamConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates Asset Folder definitions (node and Title) based on a well defined Excel document.
 */
public class AssetFolderCreator extends ProcessDefinition implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(AssetFolderCreator.class);
    private static final long serialVersionUID = 4393712954263547160L;

    public static final String NAME = "Asset Folder Creator";

    protected transient Map<String, ResourceDefinitionBuilder> resourceDefinitionBuilders;

    public enum AssetFolderBuilder {
        TITLE_TO_NODE_NAME,
        TITLE_AND_NODE_NAME,
        LOWERCASE_WITH_DASHES,
        NONE
    }

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
    public transient InputStream excelFile = null;

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
        // Initialization not required for this process
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
            // Close the InputStream to prevent resource leaks.
            excelFile.close();

            final XSSFSheet sheet = workbook.getSheetAt(0);
            final Iterator<Row> rows = sheet.rowIterator();

            while(rows.hasNext()) {
                parseAssetFolderRow(rows.next());
            }
            log.info("Finished Parsing and collected [ {} ] asset folders for creation.", assetFolderDefinitions.size());
        });
    }

    /**
     * Parse a row in the Excel that represents an asset folder and ancestors.
     *
     * @param row the row to process from the Excel sheet.
     */
    private void parseAssetFolderRow(final Row row) {
        final Iterator<Cell> cells = row.cellIterator();

        // The previousAssetFolderPath is reset on each new row.
        String previousAssetFolderPath = null;

        while (cells.hasNext()) {
            try {
                previousAssetFolderPath = parseAssetFolderCell(cells.next(), previousAssetFolderPath);
            } catch (IllegalArgumentException e) {
                // Error logged in throwing method parseAssetFolderCell.
                // Skip rest of row to avoid creating undesired structures with bad data.
                break;
            }
        }
    }

    /**
     * Parse a single cell from an Excel row.
     *
     * @param cell the cell to process from the Excel row.
     * @param previousAssetFolderPath the node path of the previous
     * @return the asset folder path to the asset folder represented by {@param cell}
     * @throws IllegalArgumentException
     */
    private String parseAssetFolderCell(final Cell cell, final String previousAssetFolderPath) throws IllegalArgumentException {
        // #1791 - Cannot read from non-String type fields.
        // Note: switch from using cell.setCellType because it breaks in newer versions of POI
        // Use variant to proxy the value as it can deal with POI 3.x -> 4.x changes
        Variant var = new Variant(cell, Locale.getDefault());
        final String cellValue = StringUtils.trimToNull(var.toString());

        if (StringUtils.isNotBlank(cellValue)) {

            // Generate a asset folder definition that will in turn be used to drive the asset folder definition creation
            AssetFolderDefinition assetFolderDefinition = getAssetFolderDefinition(primary, cellValue, previousAssetFolderPath);

            // Try using the fallback converter if the primary convert could not resolve to a valid definition.
            if (assetFolderDefinition == null) {
                assetFolderDefinition = getAssetFolderDefinition(fallback, cellValue, previousAssetFolderPath);
            }

            if (assetFolderDefinition == null) {
                log.warn("Could not find a Asset Folder Converter that accepts value [ {} ]; skipping...", cellValue);
                // Record parse failure
                record(ReportRowStatus.FAILED_TO_PARSE, "", cellValue);
                throw new IllegalArgumentException(String.format("Unable to parse value [ %s ]. Skipping rest of row to prevent undesired structured from being created.", cellValue));
            } else {
                /* Prepare for next Cell */
                if (assetFolderDefinitions.get(assetFolderDefinition.getId()) == null) {
                    assetFolderDefinitions.put(assetFolderDefinition.getId(), assetFolderDefinition);
                }

                return assetFolderDefinition.getPath();
            }
        } else {
            // If cell is blank then treat as it it is empty.
            return previousAssetFolderPath;
        }
    }

    public void createAssetFolders(ActionManager manager) {
        assetFolderDefinitions.values().stream().forEach(assetFolderDefinition -> {
            try {
                manager.withResolver(rr -> {
                    createAssetFolder(assetFolderDefinition, rr);
                });
            } catch (Exception e) {
                log.error("Unable to import asset folders via ACS Commons MCP - Asset Folder Creator", e);
            }
        });
    }

    /**
     * Creates an Asset Folder.
     *
     * @param assetFolderDefinition the asset folder definition to create.
     * @param resourceResolver the resource resolver object used to create the asset folder.
     * @throws PersistenceException
     * @throws RepositoryException
     */
    protected void createAssetFolder(final AssetFolderDefinition assetFolderDefinition, final ResourceResolver resourceResolver) {
        ReportRowStatus status;

        Resource folder = resourceResolver.getResource(assetFolderDefinition.getPath());

        try {
            if (folder == null) {
                final Map<String, Object> folderProperties = new HashMap<>();
                folderProperties.put(JcrConstants.JCR_PRIMARYTYPE, assetFolderDefinition.getNodeType());
                folder = resourceResolver.create(resourceResolver.getResource(assetFolderDefinition.getParentPath()),
                        assetFolderDefinition.getName(),
                        folderProperties);

                status = ReportRowStatus.CREATED;
            } else {
                status = ReportRowStatus.UPDATED_FOLDER_TITLES;
            }

            final Resource jcrContent = folder.getChild(JcrConstants.JCR_CONTENT);

            if (jcrContent == null) {
                final Map<String, Object> jcrContentProperties = new HashMap<>();
                jcrContentProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                resourceResolver.create(folder, JcrConstants.JCR_CONTENT, jcrContentProperties);
            }

            setTitles(folder, assetFolderDefinition);
            record(status, assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());

            log.debug("Created Asset Folder [ {} -> {} ]", assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());
        } catch (Exception e) {
            record(ReportRowStatus.FAILED_TO_CREATE, assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());
            log.error("Unable to create Asset Folder [ {} -> {} ]", new String[]{assetFolderDefinition.getPath(), assetFolderDefinition.getTitle()}, e);
        }
    }

    /**
     * Generates the Asset Folder Definition.
     *
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

    private final transient GenericReport report = new GenericReport();

    private final ArrayList<EnumMap<ReportColumns, Object>> reportRows = new ArrayList<>();

    private enum ReportColumns {
        STATUS,
        ASSET_FOLDER_PATH,
        ASSET_FOLDER_TITLE
    }

    public enum ReportRowStatus {
        CREATED,
        UPDATED_FOLDER_TITLES,
        FAILED_TO_PARSE,
        FAILED_TO_CREATE,
    }

    private void record(ReportRowStatus status, String path, String title) {
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
        private static final String ASSET_ROOT_PATH = DamConstants.MOUNTPOINT_ASSETS;
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
