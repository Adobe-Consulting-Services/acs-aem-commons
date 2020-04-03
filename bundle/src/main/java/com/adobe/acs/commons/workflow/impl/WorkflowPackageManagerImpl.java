/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACS AEM Commons - Workflow Package Manager.
 * Manager for creating and working with Workflow Packages.
 *
 */
@Component
@Service
public class WorkflowPackageManagerImpl implements WorkflowPackageManager {
    private static final Logger log = LoggerFactory.getLogger(WorkflowPackageManagerImpl.class);

    private static final String WORKFLOW_PACKAGES_PATH = "/var/workflow/packages";

    private static final String LEGACY_WORKFLOW_PACKAGES_PATH = "/etc/workflow/packages";

    private static final String WORKFLOW_PACKAGE_TEMPLATE = "/libs/cq/workflow/templates/collectionpage";

    private static final String NT_VLT_DEFINITION = "vlt:PackageDefinition";

    private static final String NN_VLT_DEFINITION = "vlt:definition";

    private static final String FILTER_RESOURCE_TYPE = "cq/workflow/components/collection/definition/resourcelist";

    private static final String FILTER_RESOURCE_RESOURCE_TYPE = "cq/workflow/components/collection/definition/resource";

    private static final String WORKFLOW_PAGE_RESOURCE_TYPE = "cq/workflow/components/collection/page";

    private static final String NT_SLING_FOLDER = "sling:Folder";

    private static final String SLING_RESOURCE_TYPE = SlingConstants.PROPERTY_RESOURCE_TYPE;

    private static final String[] DEFAULT_WF_PACKAGE_TYPES = {"cq:Page", "cq:PageContent", "dam:Asset"};

    private String[] workflowPackageTypes = DEFAULT_WF_PACKAGE_TYPES;
    
    private static final String SERVICE_NAME = "workflowpackagemanager-service";
    private static final Map<String, Object> AUTH_INFO;
    
    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Property(label = "Workflow Package Types",
            description = "Node Types allowed by the WF Package. Default: cq:Page, cq:PageContent, dam:Asset",
            value = { "cq:Page", "cq:PageContent", "dam:Asset" })
    public static final String PROP_WF_PACKAGE_TYPES = "wf-package.types";


    @Reference
    private ResourceCollectionManager resourceCollectionManager;
    
    @Reference
    ResourceResolverFactory resourceResolverFactory;
    
    private String bucketPath;

    /**
     * {@inheritDoc}
     */
    public final Page create(final ResourceResolver resourceResolver,
                             final String name, final String... paths) throws WCMException,
            RepositoryException {
        return this.create(resourceResolver, null, name, paths);
    }

    /**
     * {@inheritDoc}
     */
    public final Page create(final ResourceResolver resourceResolver, String bucketSegment,
                             final String name, final String... paths) throws WCMException,
            RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        

        if (StringUtils.isNotBlank(bucketSegment)) {
            bucketPath += "/" + bucketSegment;
        }

        final Node shardNode = JcrUtils.getOrCreateByPath(bucketPath,
                NT_SLING_FOLDER, NT_SLING_FOLDER, session, false);
        final Page page = pageManager.create(shardNode.getPath(), JcrUtil.createValidName(name),
                WORKFLOW_PACKAGE_TEMPLATE, name, false);
        final Resource contentResource = page.getContentResource();

        Node node = JcrUtil.createPath(contentResource.getPath() + "/" + NN_VLT_DEFINITION, NT_VLT_DEFINITION, session);
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
        return getPaths(resourceResolver,path, workflowPackageTypes);
    }

    /**
     * {@inheritDoc}
     */
    public final List<String> getPaths(final ResourceResolver resourceResolver,
            final String path, final String[] nodeTypes) throws RepositoryException {
        final List<String> collectionPaths = new ArrayList<String>();

        final Resource resource = resourceResolver.getResource(path);

        if (resource == null) {
            log.warn("Requesting paths for a non-existent Resource [ {} ]; returning empty results.", path);
            return Collections.emptyList();

        } else if (!isWorkflowPackage(resourceResolver, path)) {
            log.debug("Requesting paths for a non-Resource Collection  [ {} ]; returning provided path.", path);
            return Arrays.asList(new String[]{ path });

        } else {
            final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            final Page page = pageManager.getContainingPage(path);

            if (page != null && page.getContentResource() != null) {
                final Node node = page.getContentResource().adaptTo(Node.class);

                final ResourceCollection resourceCollection = getResourceCollection(node);

                if (resourceCollection != null) {
                    final List<Node> members = resourceCollection.list(nodeTypes);
                    for (final Node member : members) {
                        collectionPaths.add(member.getPath());
                    }
                    return collectionPaths;
                }
            }

            return Arrays.asList(new String[]{ path });
        }
    }

    /* This is broken out into its own method to allow for easier unit testing */
    protected ResourceCollection getResourceCollection(final Node node) throws RepositoryException {
        return ResourceCollectionUtil.getResourceCollection(node, resourceCollectionManager);
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

        if (!contentResource.isResourceType(WORKFLOW_PAGE_RESOURCE_TYPE)) {
            return false;
        }

        if (contentResource.getChild(NN_VLT_DEFINITION) == null) {
            return false;
        }

        return true;
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

    private String getBucketPath(final ResourceResolver resourceResolver) {
        if (resourceResolver.getResource(WORKFLOW_PACKAGES_PATH) != null) {
            return WORKFLOW_PACKAGES_PATH;
        } else {
            return LEGACY_WORKFLOW_PACKAGES_PATH;
        }

    }

    @Activate
    protected final void activate(final Map<String, String> config) {
        workflowPackageTypes =
                PropertiesUtil.toStringArray(config.get(PROP_WF_PACKAGE_TYPES), DEFAULT_WF_PACKAGE_TYPES);
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            bucketPath = getBucketPath(resolver);
            log.debug("using {} as bucket path for wf packages",bucketPath);
        } catch (LoginException e) {
            log.error("Cannot determine bucket path for WorkflowPackageManager, do not activate this service",e);
            // this service must not get activated
            throw new IllegalStateException(e);
        }
        
    }
}
