package com.adobe.acs.commons.workflow.impl;

import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.workflow.collection.ResourceCollection;
import com.day.cq.workflow.collection.ResourceCollectionManager;
import com.day.cq.workflow.collection.ResourceCollectionUtil;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Workflow Package Manager",
        description = "Manager for creating and working w Workflow Packages"
)
@Service
public class WorkflowPackageManagerImpl implements WorkflowPackageManager {
    private static final Logger log = LoggerFactory.getLogger(WorkflowPackageManagerImpl.class);

    private static final String WORKFLOW_PACKAGE_TEMPLATE = "/libs/cq/workflow/templates/collectionpage";

    private static final String NT_VLT_DEFINITION = "vlt:PackageDefinition";

    private static final String FILTER_RESOURCE_TYPE = "cq/workflow/components/collection/definition/resourcelist";

    private static final String FILTER_RESOURCE_RESOURCE_TYPE = "cq/workflow/components/collection/definition/resource";

    private static final String WORKFLOW_PAGE_RESOURCE_TYPE = "cq/workflow/components/collection/page";

    private static final String NT_SLING_FOLDER = "sling:Folder";

    private static final String SLING_RESOURCE_TYPE = SlingConstants.PROPERTY_RESOURCE_TYPE;

    private static final SimpleDateFormat DATE_FOLDER_NAME = new SimpleDateFormat("yyyy/MM/dd");

    private static final String[] DEFAULT_WF_PACKAGE_TYPES = {"cq:Page", "cq:PageContent", "dam:Asset"};

    private String[] workflowPageTypes = DEFAULT_WF_PACKAGE_TYPES;

    @Property(label = "Workflow Package Types",
            description = "Node Types allowed by the WF Package. Default: cq:Page, cq:PageContent, dam:Asset",
            value = { "cq:Page", "cq:PageContent", "dam:Asset" })
    public static final String PROP_WF_PACKAGE_TYPES = "wf-package.types";


    @Reference
    private ResourceCollectionManager resourceCollectionManager;

    /**
     * {@inheritDoc}
     */
    public final Page create(final ResourceResolver resourceResolver,
                       final String name, final String... paths) throws WCMException,
            RepositoryException {

        final Session session = resourceResolver.adaptTo(Session.class);
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        final Node shardNode = JcrUtils.getOrCreateByPath("/etc/workflow/packages/"
                        + DATE_FOLDER_NAME.format(new Date()),
                NT_SLING_FOLDER, NT_SLING_FOLDER, session, false);
        final Page page = pageManager.create(shardNode.getPath(), JcrUtil.createValidName(name),
                WORKFLOW_PACKAGE_TEMPLATE, name, false);
        final Resource contentResource = page.getContentResource();

        Node node = JcrUtil.createPath(contentResource.getPath() + "/vlt:definition", NT_VLT_DEFINITION, session);
        node = JcrUtil.createPath(node.getPath() + "/filter", JcrConstants.NT_UNSTRUCTURED, session);
        JcrUtil.setProperty(node, SLING_RESOURCE_TYPE, FILTER_RESOURCE_TYPE);

        int i = 0;
        Node resourceNode = null;
        for (final String path : paths) {
            if (path != null) {
                resourceNode = JcrUtil.createPath(node.getPath() + "/resource_" + i++,
                        JcrConstants.NT_UNSTRUCTURED, session);
                JcrUtil.setProperty(resourceNode, "root", path);
                JcrUtil.setProperty(resourceNode, "rules", this.getIncludeRules(path));
                JcrUtil.setProperty(resourceNode, SLING_RESOURCE_TYPE, FILTER_RESOURCE_RESOURCE_TYPE);
            }
        }

        session.save();

        return page;
    }

    /**
     * {@inheritDoc}
     */
    public final List<String> getPaths(final ResourceResolver resourceResolver,
                                 final String path) throws RepositoryException {
        final List<String> paths = new ArrayList<String>();
        final Resource resource = resourceResolver.getResource(path);

        if (resource == null) {
            log.warn("Requesting paths for a non-existent Resource [ {} ]; returning empty results.", path);
            return paths;
        }

        final Node node = resource.adaptTo(Node.class);
        final ResourceCollection resourceCollection =
                ResourceCollectionUtil.getResourceCollection(node, resourceCollectionManager);

        if (resourceCollection != null) {
            final List<Node> members = resourceCollection.list(workflowPageTypes);
            for (final Node member : members) {
                paths.add(member.getPath());
            }
        } else {
            // Is not a workflow package; return a List of the provided payload path
            paths.add(path);
        }
        return paths;
    }

    /**
     * {@inheritDoc}
     */
    public final void delete(final ResourceResolver resourceResolver, final String path) throws RepositoryException {
        final Resource resource = resourceResolver.getResource(path);

        if (resource == null) {
            log.error("Requesting to delete a non-existent Workflow Package [ {} ]", path);
            return;
        }

        final Node node = resource.adaptTo(Node.class);
        if (node != null) {
            node.remove();
            node.getSession().save();
        } else {
            log.error("Trying to delete a wf resource [ {} ] that does not resolve to a node.", resource.getPath());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isWorkflowPackage(final ResourceResolver resourceResolver, final String path) {
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        final Page workflowPackagesPage = pageManager.getPage(path);
        if (workflowPackagesPage == null) {
            return false;
        }

        final Resource contentResource = workflowPackagesPage.getContentResource();
        if (contentResource == null) {
            return false;
        }

        return contentResource.isResourceType(WORKFLOW_PAGE_RESOURCE_TYPE);
    }

    /**
     * Creates the Workflow Page Resource's include rules.
     *
     * @param path the path for which the include rules are to be created
     * @return a String array of all the include rules
     */
    private String[] getIncludeRules(final String path) {
        String[] rules;

        final String rootInclude = "include:" + path;
        final String contentInclude = "include:" + path + "/jcr:content(/.*)?";

        rules = new String[]{rootInclude, contentInclude};

        return rules;
    }


    @Activate
    protected final void activate(final Map<String, String> config) {
        workflowPageTypes = PropertiesUtil.toStringArray(config.get(PROP_WF_PACKAGE_TYPES), DEFAULT_WF_PACKAGE_TYPES);
    }
}
