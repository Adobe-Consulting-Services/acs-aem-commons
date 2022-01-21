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

import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.cq.commons.jcr.JcrUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.Version;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ACS AEM Commons - Package Helper Helper utility for creating CRX Packages and
 * using the ACS AEM Commons packager.
 */
@Component
@Service
public final class PackageHelperImpl implements PackageHelper {

    private static final Logger log = LoggerFactory.getLogger(PackageHelperImpl.class);

    private static final String NN_THUMBNAIL = "thumbnail.png";

    private static final String KEY_STATUS = "status";
    private static final String KEY_MSG = "msg";
    private static final String KEY_PATH = "path";
    private static final String KEY_FILTER_SETS = "filterSets";
    private static final String KEY_IMPORT_MODE = "importMode";
    private static final String KEY_ROOT_PATH = "rootPath";

    @Reference
    private Packaging packaging;

    /**
     * {@inheritDoc}
     */
    public void addThumbnail(final JcrPackage jcrPackage, Resource thumbnailResource) {
        if (jcrPackage == null) {
            log.error("JCR Package is null; no package thumbnail needed for null packages!");
            return;
        }
        Node thumbnailNode = null;
        if (thumbnailResource != null) {
            thumbnailNode = thumbnailResource.adaptTo(Node.class);
        }

        try {
            boolean useDefault = thumbnailNode == null || !thumbnailNode.isNodeType(JcrConstants.NT_FILE);
            final Node dstParentNode = jcrPackage.getDefinition().getNode();
            final Session session = dstParentNode.getSession();
            if (useDefault) {
                log.debug("Using default ACS AEM Commons packager package icon.");
                if (session.nodeExists(DEFAULT_PACKAGE_THUMBNAIL_RESOURCE_PATH)) {
                    thumbnailNode = session.getNode(DEFAULT_PACKAGE_THUMBNAIL_RESOURCE_PATH);
                }
            }

            if (thumbnailNode == null || !thumbnailNode.isNodeType(JcrConstants.NT_FILE)) {
                log.warn("Cannot find a specific OR a default package icon; no package icon will be used.");
            } else {
                JcrUtil.copy(thumbnailNode, dstParentNode, NN_THUMBNAIL);
                dstParentNode.getSession().save();
            }
        } catch (RepositoryException e) {
            log.error("Could not add package thumbnail: {}", e);
        }
    }

    @SuppressWarnings("squid:S3776")
    public Version getNextVersion(final JcrPackageManager jcrPackageManager,
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

                try (final JcrPackage jcrPackage = jcrPackageManager.open(child, true)) {
                    if (jcrPackage == null
                            || jcrPackage.getDefinition() == null
                            || jcrPackage.getDefinition().getId() == null) {

                        log.warn("Could not covert node [ {} ] into a proper JCR Package, moving to next node",
                                child.getPath());
                        continue;

                    } else if (!StringUtils.equals(name, jcrPackage.getDefinition().getId().getName())) {
                        // Name mismatch - so just skip
                        continue;
                    }

                    final Version packageVersion = jcrPackage.getDefinition().getId().getVersion();

                    log.debug("{} compareTo {} = {}", packageVersion.toString(), latestVersion.toString(), packageVersion.compareTo(latestVersion));

                    if (packageVersion.compareTo(latestVersion) >= 1) {
                        latestVersion = packageVersion;
                        log.debug("Found a new latest version: {}", latestVersion);
                    } else if (packageVersion.compareTo(configVersion) == 0) {
                        configVersionEligible = false;
                        log.debug("Found a package with the same version as the config version");
                    }
                }
            }

            log.debug("Current latest version: {}", latestVersion);
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
    public void removePackage(final JcrPackageManager jcrPackageManager,
            final String groupName, final String name,
            final String version) throws RepositoryException {
        final PackageId packageId = new PackageId(groupName, name, version);
        try (final JcrPackage jcrPackage = jcrPackageManager.open(packageId)) {

            if (jcrPackage != null && jcrPackage.getNode() != null) {
                jcrPackage.getNode().remove();
                jcrPackage.getNode().getSession().save();
            } else {
                log.debug("Nothing to remove at: {} ", packageId.getInstallationPath());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public JcrPackage createPackage(final Collection<Resource> resources, final Session session,
            final String groupName, final String name, String version,
            final ConflictResolution conflictResolution,
            final Map<String, String> packageDefinitionProperties)
            throws IOException, RepositoryException {

        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();

        for (final Resource resource : resources) {
            pathFilterSets.add(new PathFilterSet(resource.getPath()));
        }

        return this.createPackageFromPathFilterSets(pathFilterSets, session, groupName, name, version,
                conflictResolution, packageDefinitionProperties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrPackage createPackageForPaths(final Collection<String> paths, final Session session,
            final String groupName, String name, final String version,
            final ConflictResolution conflictResolution,
            Map<String, String> packageDefinitionProperties)
            throws IOException, RepositoryException {

        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();

        for (final String path : paths) {
            pathFilterSets.add(new PathFilterSet(path));
        }

        return this.createPackageFromPathFilterSets(pathFilterSets, session, groupName, name, version,
                conflictResolution, packageDefinitionProperties);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("squid:S2095") // closing is responsibility of caller
    public JcrPackage createPackageFromPathFilterSets(final Collection<PathFilterSet> pathFilterSets,
            final Session session,
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

        for (final PathFilterSet pathFilterSet : pathFilterSets) {
            workspaceFilter.add(pathFilterSet);
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
    public List<String> getContents(final JcrPackage jcrPackage) throws IOException,
            RepositoryException, PackageException {

        JcrPackageCoverageProgressListener jcrPackageCoverageProgressListener
                = new JcrPackageCoverageProgressListener();

        ImportOptions importOptions = new ImportOptions();
        importOptions.setDryRun(true);
        importOptions.setListener(jcrPackageCoverageProgressListener);

        jcrPackage.extract(importOptions);

        return jcrPackageCoverageProgressListener.getCoverage();
    }

    /**
     * {@inheritDoc}
     */
    public String getSuccessJSON(final JcrPackage jcrPackage) throws RepositoryException {
        final JsonObject json = new JsonObject();

        json.addProperty(KEY_STATUS, "success");
        json.addProperty(KEY_PATH, jcrPackage.getNode().getPath());
        JsonArray filterSetsArray = new JsonArray();
        json.add(KEY_FILTER_SETS, filterSetsArray);

        final List<PathFilterSet> filterSets = jcrPackage.getDefinition().getMetaInf().getFilter().getFilterSets();
        for (final PathFilterSet filterSet : filterSets) {
            final JsonObject jsonFilterSet = new JsonObject();
            jsonFilterSet.addProperty(KEY_IMPORT_MODE, filterSet.getImportMode().name());
            jsonFilterSet.addProperty(KEY_ROOT_PATH, filterSet.getRoot());

            filterSetsArray.add(jsonFilterSet);
        }

        return json.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getPreviewJSON(final Collection<Resource> resources) {
        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();

        for (final Resource resource : resources) {
            pathFilterSets.add(new PathFilterSet(resource.getPath()));
        }

        return this.getPathFilterSetPreviewJSON(pathFilterSets);
    }

    /**
     * {@inheritDoc}
     */
    public String getPreviewJSONForPaths(Collection<String> paths) {
        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();

        for (final String path : paths) {
            pathFilterSets.add(new PathFilterSet(path));
        }

        return this.getPathFilterSetPreviewJSON(pathFilterSets);
    }

    /**
     * {@inheritDoc}
     */
    public String getPathFilterSetPreviewJSON(final Collection<PathFilterSet> pathFilterSets) {
        final JsonObject json = new JsonObject();

        json.addProperty(KEY_STATUS, "preview");
        json.addProperty(KEY_PATH, "Not applicable (Preview)");
        JsonArray filterSets = new JsonArray();
        json.add(KEY_FILTER_SETS, filterSets);

        for (final PathFilterSet pathFilterSet : pathFilterSets) {
            final JsonObject tmp = new JsonObject();
            tmp.addProperty(KEY_IMPORT_MODE, "Not applicable (Preview)");
            tmp.addProperty(KEY_ROOT_PATH, pathFilterSet.getRoot());

            filterSets.add(tmp);
        }

        return json.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getErrorJSON(final String msg) {
        Gson gson = new Gson();
        final JsonObject json = new JsonObject();
        json.addProperty(KEY_STATUS, "error");
        json.addProperty(KEY_MSG, msg);
        return gson.toJson(json);
    }

}
