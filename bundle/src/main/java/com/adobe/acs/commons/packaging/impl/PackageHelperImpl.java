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

package com.adobe.acs.commons.packaging.impl;

import com.adobe.acs.commons.packaging.JcrPackageCoverageProgressListener;
import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.jcr.vault.fs.api.PathFilterSet;
import com.day.jcr.vault.fs.config.DefaultWorkspaceFilter;
import com.day.jcr.vault.fs.io.ImportOptions;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageDefinition;
import com.day.jcr.vault.packaging.JcrPackageManager;
import com.day.jcr.vault.packaging.PackageException;
import com.day.jcr.vault.packaging.PackageId;
import com.day.jcr.vault.packaging.Packaging;
import com.day.jcr.vault.packaging.Version;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(
        label = "ACS AEM Commons - Package Helper",
        description = "Helper utility for creating CRX Packages and using the ACS AEM Commons packager. "
)
@Service
public class PackageHelperImpl implements PackageHelper {
    private static final Logger log = LoggerFactory.getLogger(ACLPackagerServletImpl.class);

    private static final String NN_THUMBNAIL = "thumbnail.png";

    private static final String JSON_EXCEPTION_MSG =
            "{\"status\": \"error\", \"msg\": \"Error creating JSON response.\"}";

    @Reference
    private Packaging packaging;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * {@inheritDoc}
     */
    public final void addThumbnail(final JcrPackage jcrPackage, Resource thumbnailResource) {
        ResourceResolver resourceResolver = null;

        if (jcrPackage == null) {
            log.error("JCR Package is null; no package thumbnail needed for null packages!");
            return;
        }

        boolean useDefault = thumbnailResource == null || !thumbnailResource.isResourceType(JcrConstants.NT_FILE);

        try {
            if (useDefault) {
                log.debug("Using default ACS AEM Commons packager package icon.");
                resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                thumbnailResource = resourceResolver.getResource(DEFAULT_PACKAGE_THUMBNAIL_RESOURCE_PATH);
            }

            if (thumbnailResource == null || !thumbnailResource.isResourceType(JcrConstants.NT_FILE)) {
                log.warn("Cannot find a specific OR a default package icon; no package icon will be used.");
            } else {
                final Node srcNode = thumbnailResource.adaptTo(Node.class);
                final Node dstParentNode = jcrPackage.getDefinition().getNode();

                JcrUtil.copy(srcNode, dstParentNode, NN_THUMBNAIL);
                dstParentNode.getSession().save();
            }
        } catch (RepositoryException e) {
            log.error("Could not add package thumbnail: {}", e.getMessage());
        } catch (LoginException e) {
            log.error("Could not add a default package thumbnail: {}", e.getMessage());
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public final Version getNextVersion(final JcrPackageManager jcrPackageManager,
                                        final String groupName, final String name,
                                        final String version) throws RepositoryException {
        final Node packageRoot = jcrPackageManager.getPackageRoot(false);
        final Version configVersion = Version.create(version);

        if (!packageRoot.hasNode(groupName)) {
            return configVersion;
        }

        final Node packageGroupNode = packageRoot.getNode(groupName);

        if (packageGroupNode == null) {
            return configVersion;
        } else {
            final NodeIterator children = packageGroupNode.getNodes();
            Version latestVersion = configVersion;
            boolean configVersionEligible = true;

            while (children.hasNext()) {
                final Node child = children.nextNode();

                final JcrPackage jcrPackage = jcrPackageManager.open(child, true);
                if (jcrPackage == null
                        || jcrPackage.getDefinition() == null
                        || jcrPackage.getDefinition().getId() == null) {

                    log.warn("Could not covert node [ {} ] into a proper JCR Package, moving to next node", child.getPath());
                    continue;

                } else if (!StringUtils.equals(name, jcrPackage.getDefinition().getId().getName())) {
                    // Name mismatch - so just skip
                    continue;
                }

                final Version packageVersion = jcrPackage.getDefinition().getId().getVersion();

                log.debug(packageVersion.toString() + " compareTo " + latestVersion.toString()
                        + " = " + packageVersion.compareTo(latestVersion));

                if (packageVersion.compareTo(latestVersion) >= 1) {
                    latestVersion = packageVersion;
                    log.debug("Found a new latest version: {}", latestVersion.toString());
                } else if (packageVersion.compareTo(configVersion) == 0) {
                    configVersionEligible = false;
                    log.debug("Found a package with the same version as the config version");
                }
            }

            log.debug("Current latest version: {}", latestVersion.toString());
            if (configVersionEligible && latestVersion.equals(configVersion)) {
                // If the config-specified version is newer than any existing package, jump to the config version
                return configVersion;
            } else {
                // Else increment the latest known version's minor
                latestVersion = this.normalizeVersion(latestVersion);
                final String[] segments = latestVersion.getNormalizedSegments();

                // Increment minor
                segments[1] = String.valueOf(Integer.parseInt(segments[1]) + 1);

                final Version nextVersion = Version.create(segments);
                log.debug("Next version: {}", nextVersion);
                return nextVersion;
            }
        }
    }

    private Version normalizeVersion(final Version version) {
        final int numVersionSegments = 3;
        final String[] normalizedSegments = version.getNormalizedSegments();
        final String[] segments = new String[numVersionSegments];


        if (normalizedSegments.length <= 0) {
            segments[0] = "1";
        } else {
            segments[0] = normalizedSegments[0];
        }

        if (normalizedSegments.length <= 1) {
            segments[1] = "0";
        } else {
            segments[1] = normalizedSegments[1];
        }

        if (normalizedSegments.length <= 2) {
            segments[2] = "0";
        } else {
            segments[2] = normalizedSegments[2];
        }

        return Version.create(segments);
    }

    /**
     * {@inheritDoc}
     */
    public final void removePackage(final JcrPackageManager jcrPackageManager,
                                    final String groupName, final String name,
                                    final String version) throws RepositoryException {
        final PackageId packageId = new PackageId(groupName, name, version);
        final JcrPackage jcrPackage = jcrPackageManager.open(packageId);

        if (jcrPackage != null && jcrPackage.getNode() != null) {
            jcrPackage.getNode().remove();
            jcrPackage.getNode().getSession().save();
        } else {
            log.debug("Nothing to remove at: ", packageId.getInstallationPath());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final JcrPackage createPackage(final Set<Resource> resources, final Session session,
                                          final String groupName, final String name, String version,
                                          final ConflictResolution conflictResolution,
                                          final Map<String, String> packageDefinitionProperties)
            throws IOException, RepositoryException {

        final JcrPackageManager jcrPackageManager = packaging.getPackageManager(session);

        if (ConflictResolution.Replace.equals(conflictResolution)) {
            this.removePackage(jcrPackageManager, groupName, name, version);
        } else if (ConflictResolution.IncrementVersion.equals(conflictResolution)) {
            version = this.getNextVersion(jcrPackageManager, groupName, name, version).toString();
        }

        final JcrPackage jcrPackage = jcrPackageManager.create(groupName, name, version);
        final JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
        final DefaultWorkspaceFilter workspaceFilter = new DefaultWorkspaceFilter();

        for (final Resource resource : resources) {
            workspaceFilter.add(new PathFilterSet(resource.getPath()));
        }

        jcrPackageDefinition.setFilter(workspaceFilter, true);

        for (final Map.Entry<String, String> entry : packageDefinitionProperties.entrySet()) {
            jcrPackageDefinition.set(entry.getKey(), entry.getValue(), false);
        }

        session.save();

        return jcrPackage;
    }

    /**
     * {@inheritDoc}
     */
    public final List<String> getContents(final JcrPackage jcrPackage) throws IOException, RepositoryException, PackageException {

        ImportOptions importOptions = new ImportOptions();
        importOptions.setDryRun(true);
        importOptions.setListener(new JcrPackageCoverageProgressListener());

        jcrPackage.extract(importOptions);

        JcrPackageCoverageProgressListener jcrPackageCoverageProgressListener =
                (JcrPackageCoverageProgressListener) importOptions.getListener();

        return jcrPackageCoverageProgressListener.getCoverage();
    }

    /**
     * {@inheritDoc}
     */
    public final String getSuccessJSON(final JcrPackage jcrPackage) throws JSONException, RepositoryException {
        final JSONObject json = new JSONObject();

        json.put("status", "success");
        json.put("path", jcrPackage.getNode().getPath());
        json.put("filterSets", new JSONArray());

        final List<PathFilterSet> filterSets = jcrPackage.getDefinition().getMetaInf().getFilter().getFilterSets();
        for (final PathFilterSet filterSet : filterSets) {
            final JSONObject jsonFilterSet = new JSONObject();
            jsonFilterSet.put("importMode", filterSet.getImportMode().name());
            jsonFilterSet.put("rootPath", filterSet.getRoot());

            json.accumulate("filterSets", jsonFilterSet);
        }

        return json.toString();
    }

    /**
     * {@inheritDoc}
     */
    public final String getPreviewJSON(final Set<Resource> resources) throws JSONException {
        final JSONObject json = new JSONObject();

        json.put("status", "preview");
        json.put("path", "Not applicable (Preview)");
        json.put("filterSets", new JSONArray());

        for (final Resource resource : resources) {
            final JSONObject tmp = new JSONObject();
            tmp.put("importMode", "Not applicable (Preview)");
            tmp.put("rootPath", resource.getPath());

            json.accumulate("filterSets", tmp);
        }

        return json.toString();
    }


    /**
     * {@inheritDoc}
     */
    public final String getErrorJSON(final String msg) {
        final JSONObject json = new JSONObject();
        try {
            json.put("status", "error");
            json.put("msg", msg);
            return json.toString();
        } catch (JSONException e) {
            log.error("Error creating JSON Error response message: {}", e.getMessage());
            return JSON_EXCEPTION_MSG;
        }
    }
}
