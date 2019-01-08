/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.packaging;

import aQute.bnd.annotation.ProviderType;

import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Version;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helper interface for dynamic package creation.
 */
@ProviderType
@SuppressWarnings("squid:S1214")
public interface PackageHelper {
    /**
     * JCR Path to default ACS thumbnail resource.
     */
    String DEFAULT_PACKAGE_THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/definition/package-thumbnail.png";

    /**
     *  None: If conflicting package exists; fail to create a new package.
     *  Replace: If a conflicting package exists; remove that package and create a new w updated params.
     *  IncrementVersion: If a conflict package exists; increment the package minor version to the next free minor.
     */
    @SuppressWarnings("squid:S00115")
    enum ConflictResolution {
        None,
        Replace,
        IncrementVersion
    }

    /**
     * Adds the thumbnail resource to the package as the package thumbnail.
     *
     * If the thumbnailResource is null or not a valid thumbnail resource, a default ACS Package thumbnail will be
     * used.
     *
     * @param jcrPackage The package to add the thumbnail to.
     * @param thumbnailResource The JCR resource that is the thumbnail to be used as the package's thumbnail
     */
    void addThumbnail(JcrPackage jcrPackage, Resource thumbnailResource);

    /**
     * Derives the next package version to use based on the input params and the existing package versions matching
     * the input Package group and name. Next version increments "minor" version only.
     *
     * Ex. 1.0.0 ~> 1.1.0
     *     3.22.0 ~> 3.23.0
     *
     * If the param version's major is greater than the largest existing package version in jcr,
     * then the param version will be used.
     *
     * Ex. param ~> 2.0.0, largest in jcr ~>1.40.0; returned version will be 2.0.0
     *
     * @param jcrPackageManager JcrPackageManager object
     * @param groupName package group name
     * @param name package name
     * @param version package version
     * @return the next version based on existing versions in the jcr
     * @throws RepositoryException
     */
    Version getNextVersion(JcrPackageManager jcrPackageManager, String groupName, String name,
                           String version) throws RepositoryException;

    /**
     * Deletes the package node from the repository. This does NOT uninstall the package, rather deletes the
     * package node under /etc/packages/[package-group]
     *
     * @param jcrPackageManager JcrPackageManager object
     * @param groupName package group name
     * @param name package name
     * @param version package version
     * @throws RepositoryException
     */
    void removePackage(JcrPackageManager jcrPackageManager,
                       String groupName, String name, String version) throws RepositoryException;

    /**
     *
     * @param pathFilterSets the pathFilterSets that define package
     * @param session JCR Session obj; must have access to create packages under /etc/packages
     * @param groupName package group name
     * @param name package name
     * @param version package version
     * @param conflictResolution determines how package creation will be handled in the event of an existing package
     *                           of the same package group, package name, and version class
     * @param packageDefinitionProperties properties that will be added to the package definition
     * @return the jcr package that was created, or null
     * @throws IOException
     * @throws RepositoryException
     */
    JcrPackage createPackageFromPathFilterSets(final Collection<PathFilterSet> pathFilterSets, final Session session,
                                               final String groupName, final String name, String version,
                                               final ConflictResolution conflictResolution,
                                               final Map<String, String> packageDefinitionProperties)
            throws IOException, RepositoryException;

    /**
     *
     * @param resources the resources to include in the package
     * @param session JCR Session obj; must have access to create packages under /etc/packages
     * @param groupName package group name
     * @param name package name
     * @param version package version
     * @param conflictResolution determines how package creation will be handled in the event of an existing package
     *                           of the same package group, package name, and version class
     * @param packageDefinitionProperties properties that will be added to the package definition
     * @return the jcr package that was created, or null
     * @throws IOException
     * @throws RepositoryException
     */
    JcrPackage createPackage(final Collection<Resource> resources, final Session session,
                                     final String groupName, final String name, String version,
                                     final ConflictResolution conflictResolution,
                                     final Map<String, String> packageDefinitionProperties)
            throws IOException, RepositoryException;

    /**
     * returns a list of the JCR resource paths includes in the built JcrPackage
     * @param jcrPackage the jcr package
     * @return a list of absolute (starting with /) paths of the JCR resources included in the package
     * @throws IOException
     * @throws RepositoryException
     */
    List<String> getContents(JcrPackage jcrPackage) throws IOException, RepositoryException, PackageException;

    /**
     * Returns the JSON to return in the event of a successful packaging.
     *
     * @param jcrPackage the successfully created jcr package
     * @return a string representation of JSON to write to response
     */
    String getSuccessJSON(JcrPackage jcrPackage) throws RepositoryException;


    /**
     * Returns the JSON to return reporting what the packager definition will include for filterSet roots.
     *
     * @param resources the resources that represent the filterSet roots
     * @return a string representation of JSON to write to response
     */
    String getPreviewJSON(final Collection<Resource> resources);


    /**
     * Returns the JSON to return reporting what the packager definition will include for the filterSets.
     *
     * @param pathFilterSets the pathFilterSets of the package
     * @return a string representation of JSON to write to response
     */
    String getPathFilterSetPreviewJSON(final Collection<PathFilterSet> pathFilterSets);


    /**
     * Returns the JSON to return in the event of an unsuccessful packaging.
     *
     * @param msg the error message to display
     * @return a string representation of JSON to write to response
     */
    String getErrorJSON(String msg);

    /**
     * Returns the JSON to return reporting what the packager definition will include for filterSet roots.
     *
     * @param paths the paths that represent the filterSet roots
     * @return a string representation of JSON to write to response
     */
    String getPreviewJSONForPaths(Collection<String> paths);

    /**
    *
    * @param paths the resources to include in the package
    * @param session JCR Session obj; must have access to create packages under /etc/packages
    * @param groupName package group name
    * @param name package name
    * @param version package version
    * @param conflictResolution determines how package creation will be handled in the event of an existing package
    *                           of the same package group, package name, and version class
    * @param packageDefinitionProperties properties that will be added to the package definition
    * @return the jcr package that was created, or null
    * @throws IOException
    * @throws RepositoryException
    */
    JcrPackage createPackageForPaths(Collection<String> paths, Session session, String groupName, String name,
            String version, ConflictResolution conflictResolution, Map<String, String> packageDefinitionProperties) throws IOException, RepositoryException;
}
