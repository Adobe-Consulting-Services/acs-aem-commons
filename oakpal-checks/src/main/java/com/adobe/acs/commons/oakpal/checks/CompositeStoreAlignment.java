/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2019 Adobe
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
package com.adobe.acs.commons.oakpal.checks;

import net.adamcin.oakpal.core.JavaxJson;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import net.adamcin.oakpal.core.Violation;
import net.adamcin.oakpal.core.checks.Rule;
import org.apache.jackrabbit.oak.spi.mount.Mount;
import org.apache.jackrabbit.oak.spi.mount.MountInfoProvider;
import org.apache.jackrabbit.oak.spi.mount.Mounts;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.registry.impl.JcrPackageRegistry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.core.JavaxJson.arrayOrEmpty;

/**
 * Report packages which affect paths in more than one composite node store.
 * {@code config} items:
 * <dl>
 * <dt>{@code severity}</dt>
 * <dd>(default: {@link Violation.Severity#MAJOR}) specify the severity of violations reported
 * by this check.</dd>
 * <dt>{@code scopePackageIds} ({@link Rule[]})</dt>
 * <dd>(default: include all) List of scope rules matching PackageId values (group:name:version) for inclusion in the scope for
 * composite store alignment check.</dd>
 * <dt>{@code mounts} ({@code {"name": "path1" [, "path2", ...] }} )</dt>
 * <dd>(default: {@code {"apps": ["/apps", "/libs"]}}) In a JSON Object, define a named {@link Mount} for each key,
 * with the list of paths to assign to the mount. The default mount (named {@code <default>}) is implicitly defined and
 * cannot be overridden. By default, this value is configured to simulate future standard AEM deployments where the /apps
 * and /libs root paths are mounted from a separate read-only node store at runtime.
 * .</dd>
 * </dl>
 */
public final class CompositeStoreAlignment implements ProgressCheckFactory {
    public static final String CONFIG_SEVERITY = "severity";
    public static final String CONFIG_SCOPE_PACKAGE_IDS = "scopePackageIds";
    public static final String CONFIG_MOUNTS = "mounts";

    @Override
    public ProgressCheck newInstance(final JsonObject config) throws Exception {
        final Violation.Severity severity = Violation.Severity.valueOf(config.getString(CONFIG_SEVERITY,
                Violation.Severity.MAJOR.name()).toUpperCase());
        final List<Rule> ignorePackageIds = Rule.fromJsonArray(arrayOrEmpty(config, CONFIG_SCOPE_PACKAGE_IDS));
        final MountInfoProvider defaultProvider = Mounts.defaultMountInfoProvider();
        final MountInfoProvider configProvider;
        if (config.containsKey(CONFIG_MOUNTS)) {
            Mounts.Builder builder = Mounts.newBuilder();
            final JsonObject mountsObj = config.getJsonObject(CONFIG_MOUNTS);
            for (Map.Entry<String, JsonValue> mountEntry : mountsObj.entrySet()) {
                if (defaultProvider.getDefaultMount().getName().equals(mountEntry.getKey())) {
                    continue;
                }
                if (mountEntry.getValue().getValueType() == JsonValue.ValueType.ARRAY) {
                    List<String> paths = JavaxJson.mapArrayOfStrings(mountEntry.getValue().asJsonArray());
                    builder.mount(mountEntry.getKey(), paths.toArray(new String[0]));
                } else if (mountEntry.getValue().getValueType() == JsonValue.ValueType.STRING) {
                    builder.mount(mountEntry.getKey(), ((JsonString) mountEntry.getValue()).getString());
                }
            }
            configProvider = builder.build();
        } else {
            configProvider = Mounts.newBuilder().mount("apps", "/apps", "/libs").build();
        }

        return new Check(severity, ignorePackageIds, configProvider);
    }

    static final class Check extends SimpleProgressCheck {
        private final Violation.Severity severity;
        private final List<Rule> scopePackageIds;
        private final MountInfoProvider mounts;

        private final Map<PackageId, List<PackageId>> subPackages = new HashMap<>();
        private final Map<PackageId, Set<Mount>> affectedMounts = new HashMap<>();
        private transient Set<Mount> currentPackageMounts = new LinkedHashSet<>();

        Check(final Violation.Severity severity, final List<Rule> scopePackageIds, final MountInfoProvider mounts) {
            this.severity = severity;
            this.scopePackageIds = scopePackageIds;
            this.mounts = mounts;
        }

        @Override
        public String getCheckName() {
            return CompositeStoreAlignment.class.getName();
        }

        @Override
        public void identifyPackage(final PackageId packageId, final File file) {
            subPackages.put(packageId, new ArrayList<>());
        }

        @Override
        public void identifySubpackage(final PackageId packageId, final PackageId parentId) {
            subPackages.put(packageId, new ArrayList<>());
            subPackages.getOrDefault(parentId, Collections.emptyList()).add(packageId);
        }

        @Override
        public void afterExtract(PackageId packageId, Session inspectSession) throws RepositoryException {
            affectedMounts.put(packageId, new LinkedHashSet<>(currentPackageMounts));
            currentPackageMounts.clear();
        }

        /**
         * Always ignore:
         * 1. the root path because every package potentially marks it as imported
         * 2. /etc, /etc/packages, or paths that start with /etc/packages/, because packages with subpackages will import
         * these paths, even if they are installed to a different JcrPackageRegistry.
         *
         * @param path the imported or deleted path to handle
         */
        private void handlePath(final String path) {
            if (!mounts.hasNonDefaultMounts()
                    || path.equals("/")
                    || path.equals("/etc")
                    || path.equals(JcrPackageRegistry.DEFAULT_PACKAGE_ROOT_PATH)
                    || path.startsWith(JcrPackageRegistry.DEFAULT_PACKAGE_ROOT_PATH_PREFIX)) {
                return;
            }
            currentPackageMounts.add(mounts.getMountByPath(path));
        }

        @Override
        public void importedPath(final PackageId packageId,
                                 final String path,
                                 final Node node) throws RepositoryException {
            handlePath(path);
        }

        @Override
        public void deletedPath(final PackageId packageId,
                                final String path,
                                final Session inspectSession) throws RepositoryException {
            handlePath(path);
        }

        private Set<Mount> getMountsAffectedByPackage(final PackageId packageId) {
            return new HashSet<>(affectedMounts.getOrDefault(packageId, Collections.emptySet()));
        }

        private Set<Mount> getMountsAffectedByPackageGraph(final PackageId root) {
            Set<Mount> allAffectedMounts = new HashSet<>(getMountsAffectedByPackage(root));
            for (PackageId subPackageId : subPackages.getOrDefault(root, Collections.emptyList())) {
                allAffectedMounts.addAll(getMountsAffectedByPackageGraph(subPackageId));
            }
            return allAffectedMounts;
        }

        @Override
        public void finishedScan() {
            for (PackageId affectingPackageId : affectedMounts.keySet()) {
                if (Rule.lastMatch(scopePackageIds, affectingPackageId.toString()).isExclude()) {
                    continue;
                }
                final Set<Mount> affectedByPackage = getMountsAffectedByPackage(affectingPackageId);
                if (affectedByPackage.size() > 1) {
                    reportViolation(severity,
                            "package content not aligned to a single composite store mount ("
                                    + affectedByPackage.stream()
                                    .map(Mount::getName).collect(Collectors.joining(" and ")) + ")",
                            affectingPackageId);
                } else if (affectedByPackage.size() > 0) { // filter out container packages only contain subpackages
                    final Set<Mount> affectedByPackageGraph = getMountsAffectedByPackageGraph(affectingPackageId);
                    if (affectedByPackageGraph.size() > 1) {
                        reportViolation(severity,
                                "recursive package installation not aligned to a single composite store mount ("
                                        + affectedByPackageGraph.stream()
                                        .map(Mount::getName).collect(Collectors.joining(" and ")) + ")",
                                affectingPackageId);
                    }
                }
            }
        }
    }
}
