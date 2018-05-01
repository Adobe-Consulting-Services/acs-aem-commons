/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.Failure;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.util.Spreadsheet;
import com.adobe.acs.commons.mcp.util.CompositeVariant;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import assets and metadata provided by a spreadsheet
 */
public class UrlAssetImport extends AssetIngestor {

    public static String SOURCE = "source";
    public static String TARGET_FOLDER = "target";
    public static String ORIGINAL_FILE_NAME = "original";
    public static String RENDITION_NAME = "rendition";
    public static String CONTENT_BASE = "/content";
    public static String UNKNOWN_TARGET_FOLDER = "/content/dam/unsorted";

    private static final Logger LOG = LoggerFactory.getLogger(UrlAssetImport.class);
    private HttpClientBuilderFactory httpFactory;
    private HttpClient httpClient = null;

    public UrlAssetImport(MimeTypeService mimeTypeService, HttpClientBuilderFactory httpFactory) {
        super(mimeTypeService);
        this.httpFactory = httpFactory;
    }

    @FormField(
            name = "Import data file",
            description = "Data file containing asset import data",
            component = FileUploadComponent.class,
            required = true
    )
    transient RequestParameter importFile;

    @FormField(
            name = "Default prefix",
            description = "Added to source if it starts with / e.g. file:/ | file:/C: | http://www.somewebsite",
            required = true,
            options = ("default=file:/")
    )
    private String defaultPrefix = "file:/";

    @FormField(
            name = "Connection timeout",
            description = "HTTP Connection timeout (in milliseconds)",
            required = true,
            options = ("default=30000")
    )
    private int timeout = 30000;

    transient Set<FileOrRendition> files;
    transient Map<String, Folder> folders = new TreeMap<>((a, b) -> b.compareTo(a));

    Spreadsheet fileData;

    @Override
    public void init() throws RepositoryException {
        super.init();
        if (httpFactory != null) {
            HttpClientBuilder clientBuilder = httpFactory.newBuilder();
            clientBuilder.setDefaultSocketConfig(
                    SocketConfig.custom()
                            .setSoTimeout(timeout)
                            .build());
            clientBuilder.setDefaultRequestConfig(
                    RequestConfig.custom()
                            .setConnectTimeout(timeout)
                            .build()
            );
            httpClient = clientBuilder.build();
        }
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        try {
            fileData = new Spreadsheet(importFile);
            files = extractFilesAndFolders(fileData.getDataRows());
            instance.getInfo().setDescription("Import " + fileData.getFileName() + " (" + fileData.getRowCount() + " rows)");
        } catch (IOException ex) {
            LOG.error("Unable to process import", ex);
            instance.getInfo().setDescription("Import " + fileData.getFileName() + " (failed)");
            throw new RepositoryException("Unable to parse input file", ex);
        }
        instance.defineCriticalAction("Create Folders", rr, this::createFolders);
        instance.defineCriticalAction("Import Assets", rr, this::importAssets);
    }

    protected Set<FileOrRendition> extractFilesAndFolders(List<Map<String, CompositeVariant>> fileData) {
        Set<FileOrRendition> allFiles = fileData.stream()
                .peek(this::extractFolder)
                .map(this::extractFile)
                .filter(t -> t != null)
                .collect(Collectors.toSet());

        // Remove renditions from the data set and file them with their original renditions
        Set<FileOrRendition> renditions = allFiles.stream().filter(FileOrRendition::isRendition).collect(Collectors.toSet());
        allFiles.removeAll(renditions);
        renditions.forEach(r -> {
            findOriginalRendition(allFiles, r).ifPresent(asset -> asset.addRendition(r));
        });
        return allFiles;
    }

    protected void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r -> {
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, r.adaptTo(Session.class), true);
            folders.values().forEach(f
                    -> manager.deferredWithResolver(Actions.retry(10, 100, rr -> {
                        manager.setCurrentItem(f.getItemName());
                        createFolderNode(f, rr);
                    }))
            );
        });
    }

    protected void importAssets(ActionManager manager) throws IOException {
        manager.setCurrentItem(jcrBasePath);
        files.stream().filter(this::canImportContainingFolder).forEach(file -> {
            // Check the file using the deferral method so that any failures at retrieving file size can be retried.
            manager.deferredWithResolver(Actions.retry(5, 50, rr1 -> {
                manager.setCurrentItem("Evaluate " + file.getItemName());
                try {
                    if (canImportFile(file.getSource())) {
                        // Files are downloaded in a separate action to get higher overall throughput.
                        manager.deferredWithResolver(Actions.retry(5, 50,
                                importAsset(file.getSource(), manager)
                                        .andThen(updateMetadata(file)
                                                .andThen(importRenditions(file, manager)))
                        ));
                    }
                } catch (IOException ex) {
                    Failure failure = new Failure();
                    failure.setException(ex);
                    failure.setNodePath(file.getNodePath());
                    manager.getFailureList().add(failure);
                } finally {
                    file.getSource().close();
                }
            }));
        });
    }

    private CheckedConsumer<ResourceResolver> updateMetadata(FileOrRendition file) throws PersistenceException {
        return (rr) -> {
            if (dryRunMode) {
                return;
            }
            commitAndRefresh(rr);
            disableWorkflowProcessing(rr);
            ModifiableValueMap meta = rr.getResource(file.getNodePath() + "/jcr:content/metadata").adaptTo(ModifiableValueMap.class);
            updateMetadataFromRow(file, meta);
            commitAndRefresh(rr);
        };
    }

    public void commitAndRefresh(ResourceResolver rr) throws PersistenceException {
        if (rr.hasChanges()) {
            rr.commit();
        }
        rr.refresh();
    }

    public void updateMetadataFromRow(FileOrRendition file, ModifiableValueMap meta) {
        for (String prop : fileData.getHeaderRow()) {
            if (prop.contains(":")) {
                CompositeVariant value = file.getProperty(prop);
                if (value == null || value.isEmpty()) {
                    meta.remove(prop);
                } else {
                    meta.put(prop, value.toPropertyValue());
                }
            }
        }
    }

    private CheckedConsumer<ResourceResolver> importRenditions(FileOrRendition file, ActionManager manager) throws PersistenceException {
        return r -> {
            file.getRenditions().forEach((rendition, renditionFile) -> {
                manager.deferredWithResolver(rr -> {
                    try {
                        String renditionName = rendition;
                        String type = mimetypeService.getMimeType(renditionFile.getName());
                        String extension = renditionFile.getName().substring(renditionFile.getName().lastIndexOf('.') + 1).toLowerCase();
                        if (renditionName.lastIndexOf('.') <= 0) {
                            renditionName += "." + extension;
                        }
                        if (!dryRunMode) {
                            disableWorkflowProcessing(rr);
                            Asset asset = rr.getResource(file.getNodePath()).adaptTo(Asset.class);
                            asset.addRendition(renditionName, renditionFile.getSource().getStream(), type);
                        }
                        incrementCount(importedAssets, 1L);
                        incrementCount(importedData, renditionFile.getSource().getLength());
                        trackDetailedActivity(file.getNodePath(), "Import Rendition", "Add rendition " + renditionName, renditionFile.getSource().getLength());
                    } catch (IOException | IllegalArgumentException ex) {
                        Failure failure = new Failure();
                        failure.setException(ex);
                        failure.setNodePath(renditionFile.getNodePath());
                        manager.getFailureList().add(failure);
                        throw ex;
                    } finally {
                        renditionFile.getSource().close();
                    }
                });
            });
        };
    }

    private Folder extractFolder(Map<String, CompositeVariant> assetData) {
        String folderPath = getTargetFolder(assetData);
        if (!folders.containsKey(folderPath)) {
            String rootFolder = folderPath.replace(jcrBasePath, "");
            String[] parts = rootFolder.split(Pattern.quote("/"));
            Folder parent = null;
            String currentPath = jcrBasePath;
            for (int i = 1; i < parts.length; i++) {
                String treePath = currentPath + "/" + parts[i];
                if (!folders.containsKey(treePath)) {
                    Folder folder = parent == null
                            ? new Folder(parts[i], jcrBasePath)
                            : new Folder(parts[i], parent);
                    folders.put(treePath, folder);
                    parent = folder;
                } else {
                    parent = folders.get(treePath);
                }
                currentPath = treePath;
            }
        }
        return folders.get(folderPath);
    }

    private FileOrRendition extractFile(Map<String, CompositeVariant> assetData) {
        String source = assetData.get(SOURCE).toString();
        if (source == null) {
            return null;
        }
        if (source.startsWith("/")) {
            source = defaultPrefix + source;
        }
        String name = source.substring(source.lastIndexOf('/') + 1);
        Folder folder = extractFolder(assetData);
        FileOrRendition file = new FileOrRendition(this::getHttpClient, name, source, folder, assetData);

        file.setAsRenditionOfImage(
                assetData.get(RENDITION_NAME).toString(),
                assetData.get(ORIGINAL_FILE_NAME).toString()
        );

        return file;
    }

    private String getTargetFolder(Map<String, CompositeVariant> assetData) {
        String target = assetData.get(TARGET_FOLDER).toString();
        if (target == null || target.isEmpty()) {
            return UNKNOWN_TARGET_FOLDER;
        } else if (!target.startsWith(CONTENT_BASE)) {
            return jcrBasePath + (target.startsWith("/") ? target : ("/" + target));
        } else {
            return target;
        }
    }

    private Optional<FileOrRendition> findOriginalRendition(Collection<FileOrRendition> allFiles, FileOrRendition rendition) {
        // Build list of files in the target folder
        List<FileOrRendition> filesInFolder = allFiles.stream()
                .filter(f -> f.getParent().getNodePath().equals(rendition.getParent().getNodePath()))
                .collect(Collectors.toList());

        if (filesInFolder.isEmpty()) {
            LOG.error("Unable to find any other files in directory " + rendition.getParent().getNodePath());
            return Optional.empty();
        } else {
            // Organize files by closest match (better match = smaller levensthein distance)
            String fileName = rendition.getOriginalAssetName() == null || rendition.getOriginalAssetName().isEmpty()
                    ? rendition.getName().toLowerCase()
                    : rendition.getOriginalAssetName().toLowerCase();
            filesInFolder.sort((a, b) -> compareName(a, b, fileName));

            // Return best match
            return Optional.of(filesInFolder.get(0));
        }
    }

    private int compareName(FileOrRendition a, FileOrRendition b, String fileName) {
        int aDist = StringUtils.getLevenshteinDistance(a.getName().toLowerCase(), fileName);
        int bDist = StringUtils.getLevenshteinDistance(b.getName().toLowerCase(), fileName);
        return Integer.compare(aDist, bDist);
    }

    private HttpClient getHttpClient() {
        return httpClient;
    }
}
