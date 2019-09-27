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

import com.adobe.acs.commons.data.CompositeVariant;
import com.adobe.acs.commons.data.Spreadsheet;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PasswordComponent;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Import assets and metadata provided by a spreadsheet
 */
public class UrlAssetImport extends AssetIngestor {

    private static final String ACTION_SKIPPED = "Skipped";
    private static final String ACTION_UNMATCHED = "Unmatched";
    private static final String ACTION_IMPORT = "Import";
    public static final String SOURCE = "source";
    public static final String TARGET_FOLDER = "target";
    public static final String ORIGINAL_FILE_NAME = "original";
    public static final String RENDITION_NAME = "rendition";
    public static final String CONTENT_BASE = "/content";
    public static final String UNKNOWN_TARGET_FOLDER = "/content/dam/unsorted";

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
            component = FileUploadComponent.class
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

    @FormField(
            name = "Username",
            description = "Username for connections that require login",
            required = false
    )
    private String username = null;

        @FormField(
            name = "Password",
            description = "Password for connections that require login",
            required = false,
            component = PasswordComponent.class
    )
    private String password = null;

    transient Set<FileOrRendition> files;
    transient Map<String, Folder> folders = new TreeMap<>((a, b) -> b.compareTo(a));
    
    private ClientProvider clientProvider = new ClientProvider();

    Spreadsheet fileData;

    EnumMap<ReportColumns, Object> importedRenditions
            = trackDetailedActivity("All Renditions", ACTION_IMPORT, "Count of all rendition imports", 0L);

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
            clientProvider.setHttpClientSupplier(this::getHttpClient);
            clientProvider.setUsername(username);
            clientProvider.setPassword(password);
        }
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        try {
            fileData = new Spreadsheet(importFile, Arrays.asList(SOURCE, RENDITION_NAME, TARGET_FOLDER, ORIGINAL_FILE_NAME))
                    .buildSpreadsheet();
            files = extractFilesAndFolders(fileData.getDataRowsAsCompositeVariants());
            instance.getInfo().setDescription(String.format("Import %s (%s rows)",  fileData.getFileName(), fileData.getRowCount()));
        } catch (IOException ex) {
            LOG.error("Unable to process import", ex);
            instance.getInfo().setDescription(String.format("Import %s (failed)", fileData.getFileName()));
            throw new RepositoryException("Unable to parse input file", ex);
        }
        trackUnmatchedRenditions();
        trackIgnoredFiles();
        instance.defineCriticalAction("Create Folders", rr, this::createFolders);
        instance.defineAction(String.format("Import %s Assets", files.size()), rr, this::importAssets);
        int countOfRenditions = files.stream().map(FileOrRendition::getRenditions).mapToInt(Map::size).sum();
        if (countOfRenditions > 0) {
            instance.defineAction(String.format("Import %s Renditions", countOfRenditions), rr, this::importRenditions);
        }
        instance.defineAction("Update Metadata", rr, this::updateMetadata);
    }

    private void trackIgnoredFiles() {
        files.stream().filter(f -> !canImportContainingFolder(f)).forEach(file -> {
            trackDetailedActivity(file.getNodePath(preserveFileName), ACTION_SKIPPED, "Skipped file because its folder is also skipped", 0L);
            incrementCount(skippedFiles, 1 + file.getRenditions().size());
            file.getRenditions().forEach((renditionName, rendition)
                    -> trackDetailedActivity(rendition.getNodePath(preserveFileName), ACTION_SKIPPED, "Skipped rendition " + renditionName + " because its parent file is skipped", 0L));
            file.getRenditions().clear();
        });
    }

    private void trackUnmatchedRenditions() {
        unmatchedRenditions.forEach(row -> {
            long rowNumber = this.fileData.getRowNum(row);
            trackDetailedActivity(row.get(SOURCE).toString(), ACTION_UNMATCHED, "Unable to track original asset for rendition, row " + rowNumber, 0L);
            incrementCount(skippedFiles, 1);
        });
    }

    Set<Map<String, CompositeVariant>> unmatchedRenditions = new HashSet<>();

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
            Optional<FileOrRendition> asset = findOriginalRendition(allFiles, r);
            if (asset.isPresent()) {
                asset.get().addRendition(r);
            } else {
                unmatchedRenditions.add(r.getProperties());
            }
        });
        return allFiles;
    }

    protected void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r -> {
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, r.adaptTo(Session.class), true);
            folders.values().forEach(f
                    -> manager.deferredWithResolver(Actions.retry(retries, retryPause, rr -> {
                        manager.setCurrentItem(f.getSourcePath());
                        createFolderNode(f, rr);
                    }))
            );
        });
    }

    protected void importAssets(ActionManager manager) throws IOException {
        manager.setCurrentItem(jcrBasePath);
        files.stream().filter(this::canImportContainingFolder).forEach(file -> {
            // Check the file using the deferral method so that any failures at retrieving file size can be retried.
            manager.deferredWithResolver(rr -> {
                long lineNumber = fileData.getRowNum(file.getProperties());
                manager.setCurrentItem(String.format("Asset %s (line %s)", file.getItemName(), lineNumber));
                try {
                    if (canImportFile(file.getSource())) {
                        manager.deferredWithResolver(Actions.retry(retries, retryPause, importAsset(file.getSource(), manager)));
                    } else if (file.getSource().getLength() < 0) {
                        incrementCount(skippedFiles, 1);
                        throw new IOException("Unable to download " + file.getSourcePath());
                    } else {
                        incrementBytes(
                                trackDetailedActivity(file.getNodePath(preserveFileName), ACTION_SKIPPED, "Skipped file of either file size or extension", 0L),
                                file.getSource().getLength()
                        );
                        incrementCount(skippedFiles, 1);
                    }
                } finally {
                    file.getSource().close();
                }
            });
        });
    }

    protected void importRenditions(ActionManager manager) throws IOException {
        manager.setCurrentItem(jcrBasePath);
        files.stream().filter(this::canImportContainingFolder).forEach(file -> importRenditions(file, manager));
    }

    private void importRenditions(FileOrRendition file, ActionManager manager) {
        file.getRenditions().forEach((rendition, renditionFile) -> {
            manager.deferredWithResolver(Actions.retry(retries, retryPause, rr -> {
                try {
                    long lineNumber = fileData.getRowNum(renditionFile.getProperties());
                    manager.setCurrentItem(String.format("Rendition %s (line %s)", renditionFile.getItemName(), lineNumber));

                    String renditionName = rendition;
                    String type = mimetypeService.getMimeType(renditionFile.getName());
                    String extension = renditionFile.getName().substring(renditionFile.getName().lastIndexOf('.') + 1).toLowerCase();
                    if (renditionName.lastIndexOf('.') <= 0) {
                        renditionName += "." + extension;
                    }
                    if (!dryRunMode) {
                        commitAndRefresh(rr);
                        Resource assetResource = rr.getResource(file.getNodePath(preserveFileName));
                        if (assetResource == null) {
                            throw new ResourceNotFoundException("Unable to find asset resource " + file.getNodePath(preserveFileName));
                        }
                        Asset asset = assetResource.adaptTo(Asset.class);
                        asset.addRendition(renditionName, renditionFile.getSource().getStream(), type);
                    }
                    incrementCount(importedRenditions, 1L);
                    incrementBytes(importedData, renditionFile.getSource().getLength());
                    trackDetailedActivity(file.getNodePath(preserveFileName), "Import Rendition", "Add rendition " + renditionName, renditionFile.getSource().getLength());
                } finally {
                    renditionFile.getSource().close();
                }
            }));
        });
    }

    protected void updateMetadata(ActionManager manager) throws IOException {
        manager.setCurrentItem(jcrBasePath);
        files.stream().filter(this::canImportContainingFolder).forEach(file
                -> manager.deferredWithResolver(Actions.retry(retries, retryPause, updateMetadata(file)))
        );
    }

    private CheckedConsumer<ResourceResolver> updateMetadata(FileOrRendition file) {
        return (rr) -> {
            if (dryRunMode) {
                return;
            }
            long lineNumber = fileData.getRowNum(file.getProperties());
            Actions.setCurrentItem(String.format("Metadata %s (line %s)", file.getItemName(), lineNumber));
            commitAndRefresh(rr);
            Resource metaResource = rr.getResource(file.getNodePath(preserveFileName) + "/jcr:content/metadata");
            if (metaResource == null) {
                throw new ResourceNotFoundException("Unable to find asset resource " + file.getNodePath(preserveFileName));
            }
            updateMetadataFromRow(file, metaResource.adaptTo(ModifiableValueMap.class));
        };
    }

    public void commitAndRefresh(ResourceResolver rr) throws PersistenceException, RepositoryException {
        if (rr.hasChanges()) {
            rr.commit();
        }
        rr.refresh();
        disableWorkflowProcessing(rr);
    }

    public void updateMetadataFromRow(FileOrRendition file, ModifiableValueMap meta) {
        for (String prop : fileData.getHeaderRow()) {
            if (prop.contains(":")) {
                CompositeVariant value = file.getProperty(prop);
                meta.remove(prop);
                if (value != null && !value.isEmpty()) {
                    meta.put(prop, value.toPropertyValue());
                }
            }
        }
    }

    private Folder extractFolder(Map<String, CompositeVariant> assetData) {
        String folderPath = getTargetFolder(assetData);
        if (!folders.containsKey(folderPath)) {
            String rootFolder = folderPath.replaceFirst(jcrBasePath, "");
            String[] parts = rootFolder.split(Pattern.quote("/"));
            Folder parent = null;
            String currentPath = jcrBasePath;
            for (int i = 1; i < parts.length; i++) {
                String treePath = currentPath + "/" + parts[i];
                if (!folders.containsKey(treePath)) {
                    Folder folder = parent == null
                            ? new Folder(parts[i], jcrBasePath, assetData.get(SOURCE).toString())
                            : new Folder(parts[i], parent, assetData.get(SOURCE).toString());
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
        FileOrRendition file = new FileOrRendition(clientProvider, name, source, folder, assetData);

        file.setAsRenditionOfImage(
                assetData.get(RENDITION_NAME) == null ? null : assetData.get(RENDITION_NAME).toString(),
                assetData.get(ORIGINAL_FILE_NAME) == null ? null : assetData.get(ORIGINAL_FILE_NAME).toString()
        );

        return file;
    }

    private String getTargetFolder(Map<String, CompositeVariant> assetData) {
        String target = assetData.get(TARGET_FOLDER) == null ? null : assetData.get(TARGET_FOLDER).toString();
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
                .filter(f -> f.getParent().getNodePath(preserveFileName).equals(rendition.getParent().getNodePath(preserveFileName)))
                .collect(Collectors.toList());

        if (filesInFolder.isEmpty()) {
            LOG.error("Unable to find any other files in directory " + rendition.getParent().getNodePath(preserveFileName));
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
