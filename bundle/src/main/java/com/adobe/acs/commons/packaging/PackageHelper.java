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

package com.adobe.acs.commons.packaging;

import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageManager;
import com.day.jcr.vault.packaging.Version;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface PackageHelper {
    String DEFAULT_PACKAGE_THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/definition/package-thumbnail.png";

    enum ConflictResolution {
        None,
        Replace,
        IncrementVersion
    }

    /**
     *
     * @param jcrPackage
     * @param thumbnailResource
     */
    void addThumbnail(JcrPackage jcrPackage, Resource thumbnailResource);

    /**
     *
     * @param jcrPackageManager
     * @param groupName
     * @param name
     * @param version
     * @return
     * @throws RepositoryException
     */
    Version getNextVersion(JcrPackageManager jcrPackageManager, String groupName, String name,
                          String version) throws RepositoryException;

    /**
     *
     * @param jcrPackageManager
     * @param groupName
     * @param name
     * @param version
     * @throws RepositoryException
     */
    void removePackage(JcrPackageManager jcrPackageManager,
                       String groupName, String name, String version) throws RepositoryException;

    /**
     *
     * @param resources
     * @param session
     * @param groupName
     * @param name
     * @param version
     * @param conflictResolution
     * @param packageDefinitionProperties
     * @return
     * @throws IOException
     * @throws RepositoryException
     */
    JcrPackage createPackage(final Set<Resource> resources, final Session session,
                                     final String groupName, final String name, String version,
                                     final ConflictResolution conflictResolution,
                                     final Map<String, String> packageDefinitionProperties)
            throws IOException, RepositoryException;

    /**
     * Returns the JSON to return in the event of a successful packaging
     *
     * @param jcrPackage the successfully created jcr package
     * @return a string representation of JSON to write to response
     */
    String getSuccessJSON(JcrPackage jcrPackage) throws JSONException, RepositoryException;


    /**
     * Returns the JSON to return reporting what the packager definition will include for filterSet roots
     *
     * @param resources the resources that represent the filterSet roots
     * @return a string representation of JSON to write to response
     * @throws JSONException
     */
    String getPreviewJSON(final Set<Resource> resources) throws JSONException;


    /**
     * Returns the JSON to return in the event of an unsuccessful packaging
     *
     * @param msg the error message to display
     * @return a string representation of JSON to write to response
     */
    String getErrorJSON(String msg);
}
