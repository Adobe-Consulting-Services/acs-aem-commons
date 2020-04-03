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
package com.adobe.acs.commons.mcp.impl.processes.cfi;

import com.adobe.acs.commons.data.CompositeVariant;
import com.adobe.acs.commons.data.Spreadsheet;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.day.cq.commons.jcr.JcrConstants;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import a series of content fragments from a spreadsheet
 */
public class ContentFragmentImport extends ProcessDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(ContentFragmentImport.class);

    public enum ReportColumns {
        ITEM, ACTION, DESCRIPTION, COUNT
    }

    List<EnumMap<ReportColumns, Object>> reportRows = new ArrayList();
    EnumMap<ReportColumns, Object> createdFolders
            = trackActivity("All folders", "Create", "Count of all folders created");
    EnumMap<ReportColumns, Object> importedFragments
            = trackActivity("Fragments", "Import", "Count of all imported fragments");
    EnumMap<ReportColumns, Object> skippedFragments
            = trackActivity("Fragments", "Skipped", "Count of skipped fragments");

    public static final String PATH = "path";
    public static final String FOLDER_TITLE = "folderTitle";
    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String TEMPLATE = "template";
    public static final String DEFAULT_FOLDER_TYPE = "sling:Folder";

    @FormField(
            name = "Fragment data",
            component = FileUploadComponent.class
    )
    transient RequestParameter importFile;
    transient Spreadsheet spreadsheet;

    @FormField(
            name = "Dry run",
            component = CheckboxComponent.class,
            options = "checked"
    )
    transient boolean dryRunMode = true;

    @FormField(
            name = "Detailed Report",
            component = CheckboxComponent.class,
            options = "checked"
    )
    transient boolean detailedReport = true;

    @Override
    public void init() throws RepositoryException {
        try {
            // Read spreadsheet
            spreadsheet = new Spreadsheet(importFile, PATH, TEMPLATE, NAME, TITLE).buildSpreadsheet();
        } catch (IOException ex) {
            throw new RepositoryException("Unable to process spreadsheet", ex);
        }
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        instance.defineCriticalAction("Create folders", rr, this::createFolders);
        instance.defineCriticalAction("Import fragments", rr, this::importFragments);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        GenericReport report = new GenericReport();
        report.setRows(reportRows, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    // Tracker
    private synchronized EnumMap<ReportColumns, Object> trackActivity(String item, String action, String description) {
        if (reportRows == null) {
            reportRows = Collections.synchronizedList(new ArrayList<>());
        }
        EnumMap<ReportColumns, Object> reportRow = new EnumMap<>(ReportColumns.class);
        reportRow.put(ReportColumns.ITEM, item);
        reportRow.put(ReportColumns.ACTION, action);
        reportRow.put(ReportColumns.DESCRIPTION, description);
        reportRow.put(ReportColumns.COUNT, 0L);
        reportRows.add(reportRow);
        return reportRow;
    }

    protected synchronized EnumMap<ReportColumns, Object> trackDetailedActivity(String item, String action, String description, Long bytes) {
        if (detailedReport) {
            return trackActivity(item, action, description);
        } else {
            return null;
        }
    }

    @SuppressWarnings("squid:S2445")
    private void increment(EnumMap<ReportColumns, Object> row, ReportColumns col, long amt) {
        if (row != null) {
            synchronized (row) {
                row.put(col, (Long) row.getOrDefault(col, 0) + amt);
            }
        }
    }

    protected void incrementCount(EnumMap<ReportColumns, Object> row, long amt) {
        increment(row, ReportColumns.COUNT, amt);
    }

    // Build folders
    protected void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r -> {
            Map<String, String> folders = new TreeMap<>();
            spreadsheet.getDataRowsAsCompositeVariants().forEach(row -> {
                String path = getString(row, PATH);
                String folderTitle = getString(row, FOLDER_TITLE);
                if (!folders.containsKey(path)) {
                    folders.put(path, folderTitle);
                    manager.deferredWithResolver(Actions.retry(10, 100, rr -> {
                        manager.setCurrentItem(path);
                        createFolderNode(path, folderTitle, rr);
                    }));
                }
            });
        });
    }

    protected boolean createFolderNode(String path, String folderTitle, ResourceResolver r) throws RepositoryException, PersistenceException {
        if (path == null) {
            return false;
        }
        if (dryRunMode) {
            return true;
        }
        String parentPath = StringUtils.substringBeforeLast(path, "/");
        boolean titleProvided;
        String folderName = StringUtils.substringAfterLast(path, "/");
        if (folderTitle == null) {
            folderTitle = folderName;
            titleProvided = false;
        } else {
            titleProvided = true;
        }
        Session s = r.adaptTo(Session.class);
        if (s.nodeExists(path) && titleProvided) {
            return updateFolderTitle(s, path, folderTitle, r);
        } else if (!s.nodeExists(path)) {
            if (!s.nodeExists(parentPath)) {
                createFolderNode(parentPath, null, r);
            }
            Node child = s.getNode(parentPath).addNode(folderName, DEFAULT_FOLDER_TYPE);
            trackDetailedActivity(path, "Create Folder", "Create folder", 0L);
            setFolderTitle(child, folderTitle);
            incrementCount(createdFolders, 1L);
            r.commit();
            r.refresh();
            return true;
        }
        return false;
    }

    private boolean updateFolderTitle(Session s, String path, String folderTitle, ResourceResolver r) throws PersistenceException, RepositoryException {
        Node folderNode = s.getNode(path);
        Node folderContentNode = folderNode.hasNode(JcrConstants.JCR_CONTENT) ? folderNode.getNode(JcrConstants.JCR_CONTENT) : null;
        if (null != folderContentNode
                && folderContentNode.hasProperty(JcrConstants.JCR_TITLE)
                && folderContentNode.getProperty(JcrConstants.JCR_TITLE).getString().equals(folderTitle)) {
            return false;
        } else {
            if (folderContentNode == null) {
                folderContentNode = folderNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_UNSTRUCTURED);
            }
            folderContentNode.setProperty(JcrConstants.JCR_TITLE, folderTitle);
            r.commit();
            r.refresh();
            return true;
        }
    }

    private void setFolderTitle(Node child, String title) throws RepositoryException {
        if (child.hasNode(JcrConstants.JCR_CONTENT)) {
            child.getNode(JcrConstants.JCR_CONTENT).setProperty(JcrConstants.JCR_TITLE, title);
        } else {
            child.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_UNSTRUCTURED).setProperty(JcrConstants.JCR_TITLE, title);
        }
    }

    // Create/Update fragment models
    //    -- https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/content-fragment-templates.html
    // Create/Update fragments
    // -- https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/reference-materials/javadoc/com/adobe/cq/dam/cfm/ContentFragmentManager.html
    protected void importFragments(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r -> {
            spreadsheet.getDataRowsAsCompositeVariants().forEach(row -> {
                manager.deferredWithResolver(rr -> {
                    importFragment(rr, row);
                });
            });
        });
    }

    private void importFragment(ResourceResolver rr, Map<String, CompositeVariant> row) throws ContentFragmentException, PersistenceException {
        String name = getString(row, NAME);
        String title = getString(row, TITLE);
        String path = getString(row, PATH);
        String template = getString(row, TEMPLATE);
        Resource templateResource = getFragmentTemplateResource(rr, template);
        if (templateResource == null) {
            throw new ContentFragmentException("Unable to locate template " + template);
        }
        
        boolean created;
        
        if (dryRunMode) {
            created = rr.getResource(path + "/" + name) == null;
        } else {
            ContentFragment cf = getOrCreateFragment(
                    rr.getResource(path),
                    templateResource,
                    name,
                    title
            );
            created = rr.hasChanges();
            setContentElements(cf, row);
            setAssetMetadata(row, cf);
            if (rr.hasChanges()) {
                incrementCount(importedFragments, 1L);
                rr.commit();
            } else {
                incrementCount(skippedFragments, 1L);
            }
        }

        if (detailedReport) {
            if (created) {
                trackDetailedActivity("Created Fragment", path, "Created fragment " + name, 0L);
            } else if (rr.hasChanges()) {
                trackDetailedActivity("Updated Fragment", path, "Updated existing fragment " + name, 0L);
            }
        }
    }

    private void setAssetMetadata(Map<String, CompositeVariant> row, ContentFragment cf) throws ContentFragmentException {
        for (Entry<String, CompositeVariant> col : row.entrySet()) {
            if (col.getKey().contains(":") && col.getValue() != null) {
                if (col.getValue().isArray()) {
                    cf.setMetaData(col.getKey(), col.getValue().getValues().toArray());
                } else {
                    cf.setMetaData(col.getKey(), col.getValue().getValueAs(String.class));
                }
            }
        }
    }

    private void setContentElements(ContentFragment cf, Map<String, CompositeVariant> row) throws ContentFragmentException {
        for (Iterator<ContentElement> i = cf.getElements(); i.hasNext();) {
            ContentElement contentElement = i.next();
            String elementName = contentElement.getName();
            String value = getString(row, elementName);
            String currentValue = contentElement.getContent();

            if (!String.valueOf(value).equals(String.valueOf(currentValue))) {
                contentElement.setContent(value, contentElement.getContentType());
            }
        }
    }

    private String getString(Map<String, CompositeVariant> row, String attr) {
        CompositeVariant v = row.get(attr.toLowerCase());  // Workaround issue #1428
        if (v != null) {
            return (String) v.getValueAs(String.class);
        } else {
            return null;
        }
    }

    protected ContentFragment getOrCreateFragment(Resource parent, Resource template, String name, String title) throws ContentFragmentException {
        Resource fragmentResource = parent.getChild(name);
        if (fragmentResource == null) {
            try {
                FragmentTemplate fragmentTemplate = template.adaptTo(FragmentTemplate.class);
// TODO: Replace this reflection hack with the proper method once ACS Commons doesn't support 6.2 anymore
                return (ContentFragment) MethodUtils.invokeMethod(fragmentTemplate, "createFragment", parent, name, title);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                LOG.error("Unable to call createFragment method -- Is this 6.3 or newer?", ex);
                return null;
            }
        } else {
            return fragmentResource.adaptTo(ContentFragment.class);
        }
    }

    protected Resource getFragmentTemplateResource(ResourceResolver rr, String templatePath) {
        Resource template = rr.resolve(templatePath);
        if (template.adaptTo(FragmentTemplate.class) != null) {
            return template;
        } else {
            return template.getChild("jcr:content");
        }
    }
}
