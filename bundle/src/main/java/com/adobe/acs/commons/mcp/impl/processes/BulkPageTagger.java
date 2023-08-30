/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2023 Adobe
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
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.day.cq.tagging.Tag;
import com.day.cq.wcm.api.Page;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The type Bulk page tagger.
 */
public class BulkPageTagger extends ProcessDefinition implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(BulkPageTagger.class);
    private static final long serialVersionUID = 798823856839772874L;

    /**
     * The constant NAME.
     */
    public static final String NAME = "Bulk Page Tagger";

    /**
     * The Excel file.
     */
    @FormField(
            name = "Excel File",
            description = "Provide the .xlsx file that defines the content pages and the corresponding cq:tags to be added on the pages",
            component = FileUploadComponent.class,
            options = {"mimeTypes=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "required"}
    )
    public transient InputStream excelFile = null;


    @Override
    public void init() throws RepositoryException {
        // Nothing to be done here.

    }


    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException {
        report.setName(instance.getName());
        instance.getInfo().setDescription("Bulk Tag AEM content Pages");
        instance.defineCriticalAction("Parse Excel File", rr, this::parseExcel);
        instance.defineCriticalAction("Add Tags to Content Pages", rr, this::tagPages);

    }


    /**
     * The Page tag mapping dataStructure.
     */
    transient volatile HashMap<String, String> pageTagMapping = new LinkedHashMap<>();

    /**
     * Parse input excel.
     *
     * @param manager the manager
     * @throws Exception the exception
     */
    @SuppressWarnings("squid:S112")
    public void parseExcel(ActionManager manager) throws Exception {
        manager.withResolver(rr -> {
            final XSSFWorkbook workbook = new XSSFWorkbook(excelFile);
            final XSSFSheet sheet = workbook.getSheetAt(0);

            final Iterator<Row> rows = sheet.rowIterator();
            final String tagsRootPath = new TagCreator.TagRootResolver(rr).getTagsLocationPath();


            if (tagsRootPath == null) {
                recordAction(ReportRowSatus.FAILED_TO_PARSE,
                        "Abandoning Tag parsing. Unable to determine AEM Tags root (/content/cq:tags vs /etc/tags). Please ensure the path exists and is accessible by the user running Tag Creator.", "N/A");
                return;
            }

            while (rows.hasNext()) {
                final Row row = rows.next();
                if (row.getCell(0) == null) {
                    break;
                }
                if (row.getRowNum() != 0 && row.getCell(0) != null) {
                    pageTagMapping.put(row.getCell(0).getStringCellValue(), row.getCell(1).getStringCellValue());
                }
            }

        });

    }


    /**
     * Tag pages from the excel file with cq:tags.
     *
     * @param manager the manager
     * @throws Exception the exception
     */
    @SuppressWarnings("squid:S112")
    public void tagPages(ActionManager manager) throws Exception {

        manager.withResolver(rr -> {

            pageTagMapping.forEach((key, value) -> {
                BulkPageTagger.ReportRowSatus status;
                Resource resource = rr.getResource(key);
                if (resource != null) {
                    Page page = resource.adaptTo(Page.class);
                    if (page != null) {
                        Tag[] existingPageTags = page.getTags();
                        String[] tagIds = Stream.of(existingPageTags)
                                .map(Tag::getTagID)
                                .toArray(String[]::new);
                        Set<String> updatedTags = Arrays.stream(value.split("[;\n]"))
                                .map(String::trim)
                                .collect(Collectors.toSet());
                        updatedTags.addAll(Arrays.asList(tagIds));
                        String[] updatedTagsArray = updatedTags.stream().toArray(String[]::new);

                        ModifiableValueMap properties = page.getContentResource().adaptTo(ModifiableValueMap.class);
                        properties.put(com.day.cq.tagging.TagConstants.PN_TAGS, updatedTagsArray);
                        try {
                            rr.commit();
                            status = ReportRowSatus.UPDATED_EXISTING;
                            recordAction(status, page.getPath(), Arrays.toString(updatedTagsArray));
                        } catch (PersistenceException e) {
                            status = ReportRowSatus.FAILED_TO_UPDATE;
                            recordAction(status, page.getPath(), Arrays.toString(updatedTagsArray));
                            log.error(String.format("Unable to add tags to page with page path - %s ", page.getPath()));
                        }


                    }

                }

            });
        });
    }


    /**
     * Reporting
     **/


    private final transient GenericBlobReport report = new GenericBlobReport();

    private final transient ArrayList<EnumMap<BulkPageTagger.ReportColumns, Object>> reportRows = new ArrayList<>();

    private enum ReportColumns {
        /**
         * Status report columns.
         */
        STATUS,
        /**
         * Page path report columns.
         */
        PAGE_PATH,
        /**
         * Tags array report columns.
         */
        TAGS_ARRAY
    }

    /**
     * The enum Report row satus.
     */
    public enum ReportRowSatus {
        /**
         * Created report row satus.
         */
        CREATED,
        /**
         * Updated existing report row satus.
         */
        UPDATED_EXISTING,
        /**
         * Failed to parse report row satus.
         */
        FAILED_TO_PARSE,
        /**
         * Failed to update report row satus.
         */
        FAILED_TO_UPDATE
    }


    private void recordAction(BulkPageTagger.ReportRowSatus status, String pagePath, String tags) {
        final EnumMap<BulkPageTagger.ReportColumns, Object> row = new EnumMap<>(BulkPageTagger.ReportColumns.class);

        row.put(BulkPageTagger.ReportColumns.STATUS, StringUtil.getFriendlyName(status.name()));
        row.put(ReportColumns.PAGE_PATH, pagePath);
        row.put(ReportColumns.TAGS_ARRAY, tags);

        reportRows.add(row);
    }


    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        report.setRows(reportRows, BulkPageTagger.ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }


}
