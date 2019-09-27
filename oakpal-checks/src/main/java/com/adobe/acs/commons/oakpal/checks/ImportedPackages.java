/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import com.google.common.collect.ImmutableMap;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.ProgressCheckFactory;
import net.adamcin.oakpal.core.SimpleProgressCheck;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ImportedPackages implements ProgressCheckFactory {

    private static final String CONFIG_VERSION = "aemVersion";
    private static final List<String> DEFAULT_VERSIONS = Arrays.asList("6.3", "6.4");


    @Override
    public ProgressCheck newInstance(JsonObject config) throws Exception {
        final List<String> versions;
        JsonArray versionsFromConfig = config.getJsonArray(CONFIG_VERSION);
        if (versionsFromConfig != null) {
            versions = versionsFromConfig.stream().map(v -> (JsonString)v).map(JsonString::getString).collect(Collectors.toList());
        } else {
            versions = DEFAULT_VERSIONS;
        }
        Map<String, Map<String, Set<Version>>> exportedPackagesByVersion = versions.stream()
            .map(version -> {
                InputStream inputStream = getClass().getResourceAsStream(String.format("/bundleinfo/%s.json", version));
                if (inputStream == null) {
                    throw new IllegalArgumentException(String.format("Unknown version %s", version));
                }
                try (JsonReader reader = Json.createReader(inputStream)) {
                    JsonObject packageDefinitions = reader.readObject();
                    ImmutableMap.Builder<String, Set<Version>> builder = ImmutableMap.builder();
                    packageDefinitions.keySet().forEach(key -> {
                        builder.put(key, packageDefinitions.getJsonArray(key).stream().map(v -> {
                            String str = ((JsonString) v).getString();
                            return new Version(str);
                        }).collect(Collectors.toSet()));
                    });
                    return new AbstractMap.SimpleEntry<>(version, builder.build());
                }
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new Check(exportedPackagesByVersion);

    }

    static final class Check extends SimpleProgressCheck {

        private final Map<String, Map<String, Set<Version>>> exportedPackagesByVersion;

        private Map<PackageId, List<Set<ImportedPackage>>> importedPackages;

        Check(Map<String, Map<String, Set<Version>>> exportedPackagesByVersion) {
            ImmutableMap.Builder<String, Map<String, Set<Version>>> builder = ImmutableMap.builder();
            exportedPackagesByVersion.forEach((version, exportedPackages) -> {
                Map<String, Set<Version>> mutableExports = new HashMap<>();
                exportedPackages.forEach((packageName, versions) -> {
                    mutableExports.put(packageName, new HashSet<>(versions));
                });
                builder.put(version, mutableExports);
            });

            this.exportedPackagesByVersion = builder.build();
        }

        @Override
        public void startedScan() {
            this.importedPackages = new HashMap<>();
        }

        @Override
        public void beforeExtract(PackageId packageId, Session inspectSession, PackageProperties packageProperties, MetaInf metaInf, List<PackageId> subpackages) throws RepositoryException {
            this.importedPackages.put(packageId, new ArrayList<>());
        }

        @Override
        public void importedPath(PackageId packageId, String path, Node node) throws RepositoryException {
            if (node.isNodeType(JcrConstants.NT_FILE) && path.endsWith(".jar")) {
                try (InputStream stream = JcrUtils.readFile(node)) {
                    ZipInputStream zipInputStream = new ZipInputStream(stream);
                    ZipEntry entry = zipInputStream.getNextEntry();
                    while (entry != null) {
                        if (entry.getName().equals("META-INF/MANIFEST.MF")) {
                            Manifest manifest = new Manifest(zipInputStream);
                            String exportPackageHeader = manifest.getMainAttributes().getValue("Export-Package");
                            parseExportPackage(exportPackageHeader);

                            String importedPackageHeader = manifest.getMainAttributes().getValue("Import-Package");
                            Set<ImportedPackage> importedPackagesForBundle = parseImportPackageHeader(importedPackageHeader);

                            this.importedPackages.get(packageId).add(importedPackagesForBundle);
                            break;
                        }

                        entry = zipInputStream.getNextEntry();
                    }
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
            }
        }

        @Override
        public void finishedScan() {
            exportedPackagesByVersion.forEach((version, exportedPackages) -> {
                importedPackages.forEach((packageId, importedPackagesForPackage) -> {
                    importedPackagesForPackage.forEach(importedPackagesForBundle -> {
                        importedPackagesForBundle.forEach(importedPackage -> {
                            Result result = importedPackage.satisfied(exportedPackages);
                            if (!result.satisfied) {
                                if (result.availableVersions.isEmpty()) {
                                    severeViolation(String.format("Package import %s cannot be satisified by AEM Version %s. Package is not exported.", importedPackage, version), packageId);
                                } else {
                                    severeViolation(String.format("Package import %s cannot be satisified by AEM Version %s. Available package versions are (%s).",
                                        importedPackage, version, result.availableVersions.stream().map(Version::toString).collect(Collectors.joining(", "))),
                                        packageId);
                                }
                            }
                        });
                    });
                });
            });
        }

        private void parseExportPackage(String header) {
            if (header == null) {
                return;
            }
            header = header.replaceAll(";uses:=\"[^\"]+\"", "");
            String[] parts = header.split(",");
            Arrays.stream(parts).forEach(this::parseExportPackageClause);
        }

        private void parseExportPackageClause(String clause) {
            String[] parts = clause.split(";");
            String packageName = null;
            String version = "0.0.0";
            for (String part : parts) {
                if (part.startsWith("version")) {
                    version = part.substring(9, part.length() - 1);
                } else if (!part.contains(":=")) {
                    packageName = part;
                }
            }
            if (packageName != null) {
                for (Map<String, Set<Version>> exportedPackages : exportedPackagesByVersion.values()) {
                    Set<Version> versions = exportedPackages.computeIfAbsent(packageName, s -> new HashSet<>());
                    versions.add(new Version(version));
                }
            }
        }
    }

    static Set<ImportedPackage> parseImportPackageHeader(String header) {
        if (header == null) {
            return Collections.emptySet();
        }
        Set<ImportedPackage> result = new LinkedHashSet<>();
        String[] parts = header.split(";");
        int currentPartIndex = 0;
        ImportedPackage currentPackage = new ImportedPackage();
        while (currentPartIndex < parts.length) {
            String currentPart = parts[currentPartIndex];
            if (currentPart.startsWith("resolution:=optional")) {
                currentPackage.optional = true;
                // might be resolution:=optional or resolution:=optional,org.apache.sling.xss
                int commaIndex = currentPart.indexOf(',');
                if (commaIndex > -1) {
                    result.add(currentPackage);
                    currentPackage = new ImportedPackage();
                    currentPackage = parsePartAsPackageName(currentPackage, currentPart.substring(commaIndex + 1), result);
                }
            } else if (currentPart.startsWith("version")) {
                // might be version="[1.0,2)",com.amazonaws.auth or might just be version="[1.0,2)"
                int firstQuoteIndex = currentPart.indexOf('"') + 1;
                int secondQuoteIndex = currentPart.indexOf('"', firstQuoteIndex);

                String versionPart = currentPart.substring(firstQuoteIndex, secondQuoteIndex);

                currentPackage.versionRange = new VersionRange(versionPart);

                int commaAfterSecondQuote = currentPart.indexOf(',', secondQuoteIndex);
                if (commaAfterSecondQuote > -1) {
                    result.add(currentPackage);
                    currentPackage = new ImportedPackage();
                    currentPackage = parsePartAsPackageName(currentPackage, currentPart.substring(commaAfterSecondQuote + 1), result);
                }
            } else {
                currentPackage = parsePartAsPackageName(currentPackage, currentPart, result);
            }
            currentPartIndex++;
        }
        if (currentPackage.packageName != null) {
            result.add(currentPackage);
        }
        return result;
    }

    @NotNull
    private static ImportedPackages.ImportedPackage parsePartAsPackageName(ImportedPackage currentPackage, String part, Set<ImportedPackage> result) {
        // could be just a bare package name or even a comma-delimited list of package names
        String[] subParts = part.split(",");
        for (int i = 0; i < subParts.length - 1; i++) {
            currentPackage.packageName = subParts[i];
            result.add(currentPackage);
            currentPackage = new ImportedPackage();
        }
        currentPackage.packageName = subParts[subParts.length - 1];
        return currentPackage;
    }

    static class ImportedPackage {
        private String packageName;
        private boolean optional;
        private VersionRange versionRange;

        private static final Result OK = new Result();
        private static final Result NO_EXPORTS = new Result(Collections.emptySet());

        Result satisfied(Map<String, Set<Version>> availablePackages) {
            if (optional || versionRange == null) {
                return OK;
            }
            Set<Version> availableVersions = availablePackages.get(packageName);
            if (availableVersions == null) {
                return NO_EXPORTS;
            }
            for (Version availableVersion : availableVersions) {
                if (versionRange.includes(availableVersion)) {
                    return OK;
                }
            }
            return new Result(availableVersions);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (packageName != null) {
                builder.append(packageName);
            }
            if (optional) {
                builder.append(";resolution:=optional");
            }
            if (versionRange != null) {
                builder.append(";version=\"").append(versionRange.toString()).append('"');
            }
            return builder.toString();
        }
    }

    static class Result {
        final boolean satisfied;
        final Set<Version> availableVersions;

        Result() {
            this.satisfied = true;
            this.availableVersions = Collections.emptySet();
        }

        Result(Set<Version> availableVersions) {
            this.satisfied = false;
            this.availableVersions = availableVersions;
        }
    }

}
