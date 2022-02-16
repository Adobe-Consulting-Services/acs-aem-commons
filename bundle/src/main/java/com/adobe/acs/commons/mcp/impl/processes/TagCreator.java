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

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.impl.BasicResourceDefinition;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagConstants;
import com.day.cq.tagging.TagManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates cq:Tags based on a well defined Excel document.
 */
public class TagCreator extends ProcessDefinition implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(TagCreator.class);
    private static final long serialVersionUID = 4325471295421747160L;

    public static final String NAME = "Tag Creator";

    private final transient Map<String, ResourceDefinitionBuilder> resourceDefinitionBuilders;

    public enum TagBuilder {
        TITLE_TO_NODE_NAME,
        TITLE_AND_NODE_NAME,
        LOWERCASE_WITH_DASHES,
        LOCALIZED_TITLE,
        NONE
    }

    public TagCreator(Map<String, ResourceDefinitionBuilder> resourceDefinitionBuilders) {
        this.resourceDefinitionBuilders = resourceDefinitionBuilders;
    }

    @FormField(
            name = "Excel File",
            description = "Provide the .xlsx file that defines the tag taxonomy",
            component = FileUploadComponent.class,
            options = {"mimeTypes=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "required"}
    )
    public transient InputStream excelFile = null;

    @FormField(
            name = "Primary Converter",
            description = "This will be used first and the Fallback will only be used if this fails to generate a valid Tag definition.",
            component = SelectComponent.EnumerationSelector.class,
            options = {"default=TITLE_AND_NODE_NAME", "required"}
    )
    public TagBuilder primary = TagBuilder.TITLE_AND_NODE_NAME;

    @FormField(
            name = "Fallback Converter",
            description = "This is only invoked when the Primary cannot generate a valid Tag definition. If this can also not genreate a valid Tag definition then the row will be skipped.",
            component = SelectComponent.EnumerationSelector.class,
            options = {"default=LOWERCASE_WITH_DASHES", "required"}
    )
    public TagBuilder fallback = TagBuilder.LOWERCASE_WITH_DASHES;

    @Override
    public void init() throws RepositoryException {
        // nothing to do here
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        report.setName(instance.getName());
        instance.getInfo().setDescription(String.format("Create tags using [ %s -> %s ]", StringUtil.getFriendlyName(primary.name()), StringUtil.getFriendlyName(fallback.name())));

        instance.defineCriticalAction("Parse tags", rr, this::parseTags);
        instance.defineCriticalAction("Create tags", rr, this::importTags);
    }

    transient volatile HashMap<String, TagDefinition> tagDefinitions = new LinkedHashMap<>();

    /**
     * Parses the input Excel file and creates a list of TagDefinition objects to process.
     *
     * @param manager the action manager
     * @throws IOException
     */
    @SuppressWarnings({"squid:S3776", "squid:S1141"})
    public void parseTags(ActionManager manager) throws Exception {
        manager.withResolver(rr -> {
            final XSSFWorkbook workbook = new XSSFWorkbook(excelFile);
            final XSSFSheet sheet = workbook.getSheetAt(0);
            final Iterator<Row> rows = sheet.rowIterator();
            final String tagsRootPath = new TagRootResolver(rr).getTagsLocationPath();

            if (tagsRootPath == null) {
                record(ReportRowSatus.FAILED_TO_PARSE,
                        "Abandoning Tag parsing. Unable to determine AEM Tags root (/content/cq:tags vs /etc/tags). Please ensure the path exists and is accessible by the user running Tag Creator.", "N/A", "N/A");
                return;
            }

            while(rows.hasNext()) {
                final Row row = rows.next();
                final Iterator<Cell> cells = row.cellIterator();

                int cellIndex = 0;
                // The previousTagId is reset on each new row.
                String previousTagId = null;

                while (cells.hasNext()) {
                    final Cell cell = cells.next();

                    final String cellValue = StringUtils.trimToNull(cell.getStringCellValue());
                    if (StringUtils.isBlank(cellValue)) {
                        // Hitting a blank cell means its the end of this row; don't process anything past this
                        break;
                    }

                    // Generate a tag definition that will in turn be used to drive the tag creation
                    TagDefinition tagDefinition = getTagDefinition(primary, cellIndex, cellValue, previousTagId, tagsRootPath);

                    if (tagDefinition == null) {
                        tagDefinition = getTagDefinition(fallback, cellIndex, cellValue, previousTagId, tagsRootPath);
                    }

                    if (tagDefinition == null) {
                        log.warn("Could not find a Tag Data Converter that accepts value [ {} ]; skipping...", cellValue);
                        // Record parse failure
                        record(ReportRowSatus.FAILED_TO_PARSE, cellValue, "", "");
                        // Break to next Row
                        break;
                    } else {
                        /* Prepare for next Cell */
                        cellIndex++;
                        previousTagId = tagDefinition.getId();

                        if (tagDefinitions.get(tagDefinition.getId()) == null) {
                            tagDefinitions.put(tagDefinition.getId(), tagDefinition);
                        }
                    }
                }
            }
            log.info("Finished Parsing and collected [ {} ] tags for import.", tagDefinitions.size());
        });
    }

    /**
     * Perform the tag creation based on the successfully parsed values in parseTags(..).
     *
     * @param manager the action manager
     */
    public void importTags(ActionManager manager) {
        tagDefinitions.values().stream().forEach(tagDefinition -> {
            try {
                manager.withResolver(rr -> {
                    final TagManager tagManager = rr.adaptTo(TagManager.class);
                    ReportRowSatus status;

                    createTag(tagDefinition, tagManager);
                });
            } catch (Exception e) {
                log.error("Unable to import tags via ACS Commons MCP - Tag Creator", e);
            }
        });
    }

    private void createTag(TagDefinition tagDefinition, TagManager tagManager) {
        ReportRowSatus status;
        try {
            if (tagManager.resolve(tagDefinition.getId()) == null) {
                status = ReportRowSatus.CREATED;
            } else {
                status = ReportRowSatus.UPDATED_EXISTING;
            }

            final Tag tag = tagManager.createTag(
                    tagDefinition.getId(),
                    tagDefinition.getTitle(),
                    tagDefinition.getDescription(),
                    false);
            if (tag != null) {
                setTitles(tag, tagDefinition);
                record(status, tag.getTagID(), tag.getPath(), tag.getTitle());
                log.debug("Created tag [ {} -> {} ]", tagDefinition.getId(), tagDefinition.getTitle());
            } else {
                log.error("Tag [ {} ] is null", tagDefinition.getId());
            }
        } catch (Exception e) {
            record(ReportRowSatus.FAILED_TO_CREATE, tagDefinition.getId(), tagDefinition.getPath(), tagDefinition.getTitle());
            log.error("Unable to create tag [ {} -> {} ]", tagDefinition.getId(), tagDefinition.getTitle());
        }
    }

    /**
     * Generates the Tag Definition.
     * @param tagBuilder The tag builder to use to construct the Tag definition.
     * @param index The level of the tag hierarchy; 0 is the Tag namespace
     * @param value The value to convert into a Tag definition.
     * @param previousTagId The previous Tag Id to build up.
     * @return a valid TagDefinition, or null if a valid TagDefinition cannot be generated.
     */
    private TagDefinition getTagDefinition(final TagBuilder tagBuilder, final int index, final String value, final String previousTagId, final String tagsRootPath) {
        final ResourceDefinitionBuilder resourceDefinitionBuilder = resourceDefinitionBuilders.get(tagBuilder.name());

        if (resourceDefinitionBuilder != null && resourceDefinitionBuilder.accepts(value)) {
            final TagDefinition tagDefinition = new TagDefinition(resourceDefinitionBuilder.convert(value), tagsRootPath);

            switch (index) {
                case 0: tagDefinition.setId(tagDefinition.getName() + TagConstants.NAMESPACE_DELIMITER);
                        break;
                case 1: tagDefinition.setId(previousTagId + tagDefinition.getName());
                        break;
                default: tagDefinition.setId(previousTagId + "/" + tagDefinition.getName());
            }

            return tagDefinition;
        }

        return null;
    }

    private void setTitles(final Tag tag, final TagDefinition tagDefinition) throws RepositoryException {
        final Node node = tag.adaptTo(Node.class);

        if (node == null) {
            log.error("Tag [ {} ] could not be adapted to a Node", tagDefinition.getId());
            return;
        }

        if (!StringUtils.equals(tag.getTitle(), tagDefinition.getTitle())) {
            // Ensure if the tag already exists that the title is set properly
            node.setProperty("jcr:title", tagDefinition.getTitle());
        }

        if (!tagDefinition.getLocalizedTitles().isEmpty()){
            // If Localized titles are provides ensure they are set properly
            final Map<String,String> translationsMap = tagDefinition.getLocalizedTitles();
            for (final Map.Entry<String, String> entry : translationsMap.entrySet()) {
                node.setProperty("jcr:title." + entry.getKey(), entry.getValue());
            }
        }
    }

    /** Reporting **/

    private final transient GenericBlobReport report = new GenericBlobReport();

    private final transient ArrayList<EnumMap<ReportColumns, Object>> reportRows = new ArrayList<>();

    private enum ReportColumns {
        STATUS,
        TAG_ID,
        TAG_PATH,
        TAG_TITLE
    }

    public enum ReportRowSatus {
        CREATED,
        UPDATED_EXISTING,
        FAILED_TO_PARSE,
        FAILED_TO_CREATE
    }

    private void record(ReportRowSatus status, String tagId, String path, String title) {
        final EnumMap<ReportColumns, Object> row = new EnumMap<>(ReportColumns.class);

        row.put(ReportColumns.STATUS, StringUtil.getFriendlyName(status.name()));
        row.put(ReportColumns.TAG_ID, tagId);
        row.put(ReportColumns.TAG_PATH, path);
        row.put(ReportColumns.TAG_TITLE, title);

        reportRows.add(row);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        report.setRows(reportRows, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    /** Tag Definition Class **/

    private final class TagDefinition extends BasicResourceDefinition {
        private final String tagsRootPath;

        public TagDefinition(ResourceDefinition resourceDefinition, String tagsRootPath) {
            super(resourceDefinition.getName());
            super.setId(resourceDefinition.getId());
            super.setDescription(resourceDefinition.getDescription());
            super.setTitle(resourceDefinition.getTitle());
            super.setLocalizedTitles(resourceDefinition.getLocalizedTitles());

            this.tagsRootPath = tagsRootPath;
        }

        @Override
        public String getPath() {
            if (getId() != null) {
                return tagsRootPath + StringUtils.replace(getId(), ":", "/");
            } else {
                return null;
            }
        }
    }

    protected enum TagsLocation {
        ETC, CONTENT, UNKNOWN;
    }

    protected static final class TagRootResolver {
        private static final String CONTENT_LOCATION = "/content/cq:tags";
        private static final String ETC_LOCATION = "/etc/tags";

        private final String tagsLocationPath;

        public TagRootResolver(final ResourceResolver resourceResolver) {
            final TagsLocation tagsLocation = resolveTagsLocation(resourceResolver);

            if (tagsLocation == TagsLocation.CONTENT) {
                tagsLocationPath = CONTENT_LOCATION;
            } else if (tagsLocation == TagsLocation.ETC) {
                tagsLocationPath = ETC_LOCATION;
            } else if (contentLocationExists(resourceResolver)) {
                tagsLocationPath = CONTENT_LOCATION;
            } else if (etcLocationExists(resourceResolver)) {
                tagsLocationPath = ETC_LOCATION;
            } else {
                tagsLocationPath = null;
            }
        }

        public String getTagsLocationPath() {
            return tagsLocationPath;
        }

        private TagsLocation resolveTagsLocation(ResourceResolver resourceResolver) {
            final TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
            final Tag[] namespaces = tagManager.getNamespaces();

            if (namespaces.length > 0) {
                final Tag tag = namespaces[0];
                if (StringUtils.startsWith(tag.getPath(), CONTENT_LOCATION)) {
                    return TagsLocation.CONTENT;
                } else {
                    return TagsLocation.ETC;
                }
            }

            return TagsLocation.UNKNOWN;
        }

        private boolean contentLocationExists(ResourceResolver resourceResolver) {
            return resourceResolver.getResource(CONTENT_LOCATION) != null;
        }

        private boolean etcLocationExists(ResourceResolver resourceResolver) {
            return resourceResolver.getResource(ETC_LOCATION) != null;
        }
    }
}
