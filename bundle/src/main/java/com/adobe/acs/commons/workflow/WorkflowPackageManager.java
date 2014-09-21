package com.adobe.acs.commons.workflow;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMException;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.text.SimpleDateFormat;
import java.util.List;

public interface WorkflowPackageManager {
    SimpleDateFormat DATED_BUCKET_SEGMENT_FORMAT = new SimpleDateFormat("yyyy/MM/dd");

    /**
     * Creates a Workflow Package for the provided paths. The Workflow Page will include the provided paths and
     * any jcr:content sub-structure.
     *
     * Stores all workflow packages under /etc/workflow/packages/{bucketSegment}
     *
     * @param resourceResolver the resource resolver used to create the package
     * @param bucketSegment a path segment used to organize workflow packages
     * @param name the name of the package
     * @param paths the paths to include
     * @return the Page representing the Workflow Page
     * @throws WCMException
     * @throws RepositoryException
     */
    Page create(ResourceResolver resourceResolver, String bucketSegment, String name,
                String... paths) throws WCMException, RepositoryException;

    /**
     * Creates a Workflow Package for the provided paths. The Workflow Page will include the provided paths and
     * any jcr:content sub-structure.
     *
     * Stores all workflow packages under /etc/workflow/packages
     *
     * @param resourceResolver the resource resolver used to create the package
     * @param name the name of the package
     * @param paths the paths to include
     * @return the Page representing the Workflow Page
     * @throws WCMException
     * @throws RepositoryException
     */
    Page create(ResourceResolver resourceResolver, String name, String... paths) throws WCMException,
            RepositoryException;

    /**
     * Gets the payload paths in the Workflow Package.
     *
     * This method will always return a List.
     * - If the path does not resolve to a resource > an empty list
     * - If the path does not resolve to a Workflow Package > a List of one item; the param path
     * - If the path does resolve to a Workflow Package > a List of all resources in the Workflow Package but not the
     * WF Package itself.
     *
     * @param resourceResolver The resource resolver to access the Workflow Package
     * @param workflowPackagePath the absolute path to the Workflow Package
     * @return a list of paths contained in the Workflow Package
     * @throws RepositoryException
     */
    List<String> getPaths(ResourceResolver resourceResolver, String workflowPackagePath) throws RepositoryException;

    /**
     * Deletes the specified Workflow Package.
     *
     * @param resourceResolver The resource resolver to access the Workflow Package
     * @param workflowPackagePath the absolute path to the Workflow Package to delete
     * @throws RepositoryException
     */
    void delete(ResourceResolver resourceResolver, String workflowPackagePath) throws RepositoryException;

    /**
     * Checks if the resource at the specified path is a Workflow Package Page.
     *
     * @param resourceResolver The resource resolver to access the Workflow Package candidate
     * @param path The path to the candidate Workflow Package
     * @return true if the path points to a Workflow Package Page
     */
    boolean isWorkflowPackage(ResourceResolver resourceResolver, String path);
}
