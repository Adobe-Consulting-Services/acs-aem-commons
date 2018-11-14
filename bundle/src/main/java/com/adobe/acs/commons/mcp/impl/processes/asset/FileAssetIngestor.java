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
import com.adobe.acs.commons.functions.CheckedSupplier;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.day.cq.commons.jcr.JcrUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Asset Ingestor reads a directory structure recursively and imports it as-is
 * into AEM.
 */
public class FileAssetIngestor extends AssetIngestor {

    public FileAssetIngestor(MimeTypeService mimeTypeService) {
        super(mimeTypeService);
    }

    @FormField(
            name = "Source",
            description = "Source folder for content ingestion which can be a local folder or SFTP url with user/password",
            hint = "/var/mycontent, /mnt/all_the_things, sftp://user:password@host[:port]/base/path...",
            required = true
    )
    String fileBasePath;
    HierarchicalElement baseFolder;

    @Override
    public void init() throws RepositoryException {
        if (fileBasePath.toLowerCase().startsWith("sftp://")) {
            try {
                baseFolder = new SftpHierarchicalElement(fileBasePath);
                baseFolder.isFolder(); // Forces a login and check status of base folder
            } catch (JSchException | URISyntaxException ex) {
                Logger.getLogger(FileAssetIngestor.class.getName()).log(Level.SEVERE, null, ex);
                throw new RepositoryException("Unable to process URL!");
            }
        } else {
            File base = new File(fileBasePath);
            if (!base.exists()) {
                throw new RepositoryException("Source folder does not exist!");
            }
            baseFolder = new FileHierarchicalElement(base);
        }
        super.init();

    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        instance.getInfo().setDescription(fileBasePath + "->" + jcrBasePath);
        instance.defineCriticalAction("Create Folders", rr, this::createFolders);
        instance.defineCriticalAction("Import Assets", rr, this::importAssets);
    }

    void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r -> {
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, r.adaptTo(Session.class), true);
            manager.setCurrentItem(fileBasePath);
            baseFolder.visitAllFolders(folder -> {
                if (canImportFolder(folder)) {
                    manager.deferredWithResolver(Actions.retry(10, 100, rr -> {
                        manager.setCurrentItem(folder.getSourcePath());
                        createFolderNode(folder, rr);
                    }));
                }
            });
        });
    }

    void importAssets(ActionManager manager) throws IOException {
        manager.deferredWithResolver(rr -> {
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, rr.adaptTo(Session.class), true);
            manager.setCurrentItem(fileBasePath);
            baseFolder.visitAllFiles(file -> {
                if (canImportContainingFolder(file)) {
                    Source fileSource = file.getSource();
                    if (canImportFile(fileSource)) {
                        try {
                            if (canImportFile(fileSource)) {
                                manager.deferredWithResolver(Actions.retry(5, 25, importAsset(fileSource, manager)));
                            } else {
                                incrementCount(skippedFiles, 1);
                                trackDetailedActivity(fileSource.getName(), "Skip", "Skipping file", 0L);
                            }
                        } catch (IOException ex) {
                            Failure failure = new Failure();
                            failure.setException(ex);
                            failure.setNodePath(fileSource.getElement().getNodePath());
                            manager.getFailureList().add(failure);
                        } finally {
                            try {
                                fileSource.close();
                            } catch (IOException ex) {
                                Failure failure = new Failure();
                                failure.setException(ex);
                                failure.setNodePath(fileSource.getElement().getNodePath());
                                manager.getFailureList().add(failure);
                            }
                        }
                    }
                }
            });
        });
    }

    private class FileSource implements Source {

        final File file;
        final HierarchicalElement element;
        private InputStream lastOpenStream;

        private FileSource(File f, FileHierarchicalElement el) {
            this.file = f;
            this.element = el;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public InputStream getStream() throws IOException {
            close();
            lastOpenStream = new FileInputStream(file);
            return lastOpenStream;
        }

        @Override
        public long getLength() {
            return file.length();
        }

        @Override
        public HierarchicalElement getElement() {
            return element;
        }

        @Override
        public void close() throws IOException {
            if (lastOpenStream != null) {
                lastOpenStream.close();
            }
            lastOpenStream = null;
        }
    }

    class FileHierarchicalElement implements HierarchicalElement {

        final File file;

        FileHierarchicalElement(File f) {
            this.file = f;
        }
        
        @Override 
        public String getSourcePath() {
            return file.getAbsolutePath();
        }

        @Override
        public boolean excludeBaseFolder() {
            return getParent() == null && isFolder();
        }

            @Override
        public Source getSource() {
            return new FileSource(file, this);
        }

        @Override
        public String getItemName() {
            return file.getName();
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public HierarchicalElement getParent() {
            File parent = file.getParentFile();
            if (parent == null || file.getAbsolutePath().equals(fileBasePath)) {
                return null;
            }
            return new FileHierarchicalElement(file.getParentFile());
        }

        @Override
        public boolean isFolder() {
            return file.isDirectory();
        }

        @Override
        public boolean isFile() {
            return file.isFile();
        }

        @Override
        public String getJcrBasePath() {
            return jcrBasePath;
        }

        @Override
        public Stream<HierarchicalElement> getChildren() {
            return Stream.of(file.listFiles()).map(FileHierarchicalElement::new);
        }
    }

    class SftpHierarchicalElement implements HierarchicalElement {

        boolean isFile;
        HierarchicalElement parent;
        String path;
        ChannelSftp channel;
        boolean retrieved = false;
        URI uri;
        String sourcePath;
        long size;
        Source source;
        boolean keepChannelOpen = false;

        SftpHierarchicalElement(String uri) throws URISyntaxException, JSchException {
            this.sourcePath = uri;
            this.uri = new URI(uri);
            this.path = this.uri.getPath();
        }

        private SftpHierarchicalElement(String uri, ChannelSftp channel, boolean holdOpen) throws URISyntaxException, JSchException {
            this(uri);
            this.channel = channel;
            this.keepChannelOpen = holdOpen;
        }

        @Override
        public String getSourcePath() {
            return sourcePath;
        }
        
        @Override
        public boolean excludeBaseFolder() {
            return getParent() == null && isFolder();
        }

        private ChannelSftp openChannel() throws URISyntaxException, JSchException {
            if (channel == null || !channel.isConnected()) {
                JSch jsch = new JSch();
                int port = uri.getPort() <= 0 ? 22 : uri.getPort();
                String userInfo = uri.getUserInfo();
                String username = StringUtils.substringBefore(userInfo, ":");
                String password = StringUtils.substringAfter(userInfo, ":");

                com.jcraft.jsch.Session session = jsch.getSession(username, uri.getHost(), port);
                Hashtable props = new Hashtable();
                props.put("StrictHostKeyChecking", "no");
                session.setConfig(props);
                session.setPassword(password);
                session.connect();
                channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();
                // If this object opened the channel it should probably be the one closing it too
                keepChannelOpen = false;
            }
            return channel;
        }

        private void closeChannel() {
            if (channel != null) {
                channel.disconnect();
                channel.getSession().disconnect();
            }
            channel = null;
        }

        private void retrieveDetails() throws URISyntaxException, JSchException, SftpException {
            if (!retrieved) {
                openChannel();
                SftpATTRS attributes = channel.lstat(path);
                isFile = !attributes.isDir();
                size = attributes.getSize();
                if (!keepChannelOpen) {
                    closeChannel();
                }
            }
            retrieved = true;
        }

        @Override
        public boolean isFile() {
            try {
                retrieveDetails();
            } catch (URISyntaxException | JSchException | SftpException ex) {
                Logger.getLogger(FileAssetIngestor.class.getName()).log(Level.SEVERE, null, ex);
            }
            return isFile;
        }

        @Override
        public HierarchicalElement getParent() {
            if (parent == null && !fileBasePath.equals(getSourcePath())) {
                try {
                    parent = new SftpHierarchicalElement(StringUtils.substringBeforeLast(getSourcePath(), "/"));
                } catch (URISyntaxException | JSchException ex) {
                    Logger.getLogger(FileAssetIngestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return parent;
        }

        @Override
        public Stream<HierarchicalElement> getChildren() {
            try {
                openChannel();
                Vector<ChannelSftp.LsEntry> children = channel.ls(path);
                return children.stream().map((ChannelSftp.LsEntry entry) -> {
                    try {
                        String childPath = getSourcePath() + "/" + entry.getFilename();
                        return (HierarchicalElement) new SftpHierarchicalElement(childPath, channel, true);
                    } catch (URISyntaxException | JSchException ex) {
                        Logger.getLogger(FileAssetIngestor.class.getName()).log(Level.SEVERE, null, ex);
                        return null;
                    }
                }).filter(Objects::nonNull);
            } catch (URISyntaxException | JSchException | SftpException ex) {
                Logger.getLogger(FileAssetIngestor.class.getName()).log(Level.SEVERE, null, ex);
                return Stream.empty();
            } finally {
                if (!keepChannelOpen) {
                    closeChannel();
                }
            }
        }

        @Override
        public String getName() {
            return StringUtils.substringAfterLast(path, "/");
        }

        @Override
        public String getItemName() {
            return path;
        }

        @Override
        public Source getSource() {
            if (source == null) {
                try {
                    retrieveDetails();
                    source = new SftpSource(size, this::openChannel, this);
                } catch (URISyntaxException | JSchException | SftpException ex) {
                    Logger.getLogger(FileAssetIngestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return source;
        }

        @Override
        public String getJcrBasePath() {
            return jcrBasePath;
        }
    }

    public static class SftpSource implements Source {

        Long length;
        CheckedSupplier<ChannelSftp> channel;
        InputStream lastStream;
        ChannelSftp lastChannel;
        HierarchicalElement element;

        public SftpSource(long length, CheckedSupplier<ChannelSftp> channel, HierarchicalElement elem) {
            this.channel = channel;
            this.length = length;
            this.element = elem;
        }

        @Override
        public String getName() {
            return element.getName();
        }

        @Override
        public InputStream getStream() throws IOException {
            try {
                lastChannel = channel.get();
                lastStream = lastChannel.get(element.getNodePath());
            } catch (Exception ex) {
                Logger.getLogger(FileAssetIngestor.class.getName()).log(Level.SEVERE, null, ex);
                close();
                throw new IOException("Error in retrieving file", ex);
            }
            return lastStream;
        }

        @Override
        public long getLength() throws IOException {
            return length;
        }

        @Override
        public HierarchicalElement getElement() {
            return element;
        }

        @Override
        public void close() throws IOException {
            if (lastStream != null) {
                lastStream.close();
                lastStream = null;
            }

            if (lastChannel != null) {
                lastChannel.disconnect();
                lastChannel.getSession().disconnect();
                lastChannel = null;
            }
        }
    }
}