package com.adobe.acs.commons.mcp.impl.processes.fragmentImport;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

/**
 * Import a series of content fragments from a spreadsheet
 */
public class ContentFragmentImport extends ProcessDefinition {

    public static final String PATH = "path";
    public static final String FOLDER_TITLE = "folderTitle";
    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String TEMPLATE = "template";
    public static final String DEFAULT_FOLDER_TYPE = "sling:Folder";

    @FormField(
            name = "Fragment data",
            required = true,
            component = FileUploadComponent.class
    )
    transient RequestParameter importFile;
    transient Spreadsheet spreadsheet;

    @FormField(
            name = "Dry run",
            component = CheckboxComponent.class
    )
    transient boolean dryRunMode;

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
            spreadsheet = new Spreadsheet(importFile, PATH, TEMPLATE, NAME, TITLE);
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
    public static enum ReportColumns {
        item, action, description, count
    }
    List<EnumMap<ReportColumns, Object>> reportRows = new ArrayList();
    EnumMap<ReportColumns, Object> createdFolders
            = trackActivity("All folders", "Create", "Count of all folders created");
    EnumMap<ReportColumns, Object> importedFragments
            = trackActivity("Fragments", "Import", "Count of all imported fragments");
    EnumMap<ReportColumns, Object> skippedFragments
            = trackActivity("Fragments", "Skipped", "Count of skipped fragments");

    private synchronized EnumMap<ReportColumns, Object> trackActivity(String item, String action, String description) {
        if (reportRows == null) {
            reportRows = Collections.synchronizedList(new ArrayList<>());
        }
        EnumMap<ReportColumns, Object> reportRow = new EnumMap<>(ReportColumns.class);
        reportRow.put(ReportColumns.item, item);
        reportRow.put(ReportColumns.action, action);
        reportRow.put(ReportColumns.description, description);
        reportRow.put(ReportColumns.count, 0L);
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

    private void increment(EnumMap<ReportColumns, Object> row, ReportColumns col, long amt) {
        if (row != null) {
            synchronized (row) {
                row.put(col, (Long) row.getOrDefault(col, 0) + amt);
            }
        }
    }

    protected void incrementCount(EnumMap<ReportColumns, Object> row, long amt) {
        increment(row, ReportColumns.count, amt);
    }

    // Build folders
    protected void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r -> {
            spreadsheet.getDataRowsAsCompositeVariants().stream()
                    .collect(Collectors.toSet()).forEach(row -> {
                String path = getString(row, PATH);
                String folderTitle = getString(row, FOLDER_TITLE);
                manager.deferredWithResolver(Actions.retry(10, 100, rr -> {
                    manager.setCurrentItem(path);
                    createFolderNode(path, folderTitle, rr);
                }));
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
        } else {
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
        ContentFragment cf = getOrCreateFragment(
                rr.getResource(path),
                templateResource,
                name,
                title
        );
        boolean created = rr.hasChanges();
        for (Iterator<ContentElement> i = cf.getElements(); i.hasNext();) {
            ContentElement contentElement = i.next();
            String elementName = contentElement.getName();
            String value = getString(row, elementName);
            String currentValue = contentElement.getContent();

            if (!String.valueOf(value).equals(String.valueOf(currentValue))) {
                contentElement.setContent(value, contentElement.getContentType());
            }
        }

        for (String attr : row.keySet()) {
            if (attr.contains(":") && row.get(attr) != null) {
                CompositeVariant var = row.get(attr);
                if (var.isArray()) {
                    cf.setMetaData(attr, var.getValues().toArray());
                } else {
                    cf.setMetaData(attr, var.getValueAs(String.class));
                }
            }
        }

        if (detailedReport) {
            if (created) {
                trackDetailedActivity("Created Fragment", path, "Created fragment " + name, 0L);
            } else if (rr.hasChanges()) {
                trackDetailedActivity("Updated Fragment", path, "Updated existing fragment " + name, 0L);
            }
        }
        if (rr.hasChanges()) {
            incrementCount(importedFragments, 1L);
            if (dryRunMode) {
                rr.revert();
            } else {
                rr.commit();
            }
        } else {
            incrementCount(skippedFragments, 1L);
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

    private ContentFragment getOrCreateFragment(Resource parent, Resource template, String name, String title) throws ContentFragmentException {
        Resource fragmentResource = parent.getChild(name);
        if (fragmentResource == null) {
            try {
                FragmentTemplate fragmentTemplate = template.adaptTo(FragmentTemplate.class);
// TODO: Replace this reflection hack with the proper method once ACS Commons doesn't support 6.2 anymore
// return fragmentTemplate.createFragment(parent, name, title);
                return (ContentFragment) MethodUtils.invokeMethod(fragmentTemplate, "createFragment", parent, name, title);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                Logger.getLogger(ContentFragmentImport.class.getName()).log(Level.SEVERE, "Unable to call createFragment method -- Is this 6.3 or newer?", ex);
                return null;
            }
        } else {
            return fragmentResource.adaptTo(ContentFragment.class);
        }
    }

    private Resource getFragmentTemplateResource(ResourceResolver rr, String templatePath) {
        Resource template = rr.resolve(templatePath);
        if (template.adaptTo(FragmentTemplate.class) != null) {
            return template;
        } else {
            return template.getChild("jcr:content");
        }
    }
}
