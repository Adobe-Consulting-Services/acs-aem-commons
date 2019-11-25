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
import com.adobe.acs.commons.mcp.form.PasswordComponent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Stream;

import static com.adobe.acs.commons.mcp.impl.processes.asset.HierarchicalElement.UriHelper.decodeUriParts;
import static com.adobe.acs.commons.mcp.impl.processes.asset.HierarchicalElement.UriHelper.encodeUriParts;

/**
 * Asset Ingestor reads a directory structure recursively and imports it as-is
 * into AEM.
 */
public class FileAssetIngestor extends AssetIngestor {

    private static final Logger LOG = LoggerFactory.getLogger(FileAssetIngestor.class);

    public FileAssetIngestor(MimeTypeService mimeTypeService) {
        super(mimeTypeService);
    }

    @FormField(
            name = "Source",
            description = "Source folder for content ingestion which can be a local folder or SFTP url",
            hint = "/var/mycontent, /mnt/all_the_things, sftp://host[:port]/base/path..."
    )
    String fileBasePath;

    @FormField(
            name = "Connection timeout",
            description = "Connection timeout (in milliseconds) for SFTP connection",
            required = false,
            options = ("default=30000")
    )
    int timeout = 30000;

    @FormField(
            name = "Username",
            description = "Username for SFTP connection",
            required = false
    )
    String username = null;

    @FormField(
            name = "Password",
            description = "Password for SFTP connection",
            required = false,
            component = PasswordComponent.class
    )
    String password = null;

    HierarchicalElement baseFolder;

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        baseFolder = getBaseFolder(fileBasePath);
        instance.getInfo().setDescription(fileBasePath + "->" + jcrBasePath);
        instance.defineCriticalAction("Create Folders", rr, this::createFolders);
        instance.defineCriticalAction("Import Assets", rr, this::importAssets);
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN") // url comes from trusted source
    HierarchicalElement getBaseFolder(final String url) throws RepositoryException {
        HierarchicalElement baseHierarchicalElement;
        if (url.toLowerCase().startsWith("sftp://")) {
            try {
                baseHierarchicalElement = new SftpHierarchicalElement(url);
                // Forces a login
                ((SftpHierarchicalElement) baseHierarchicalElement).retrieveDetails();
            } catch (URISyntaxException | UnsupportedEncodingException ex) {
                String msg = String.format("Invalid syntax for url '%s'", url);
                LOG.error("{}: {}", msg, ex.getMessage());
                throw new RepositoryException(msg, ex);
            } catch (JSchException | SftpException ex) {
                String msg = String.format("Cannot access remote system '%s'", url);
                LOG.error("{}: {}", msg, ex.getMessage());
                throw new RepositoryException(msg, ex);
            }
        } else {
            File base = new File(url);
            if (!base.exists()) {
                String msg = String.format("Source folder '%s' does not exist!", url);
                throw new RepositoryException(msg)  ;
            }
            baseHierarchicalElement = new FileHierarchicalElement(base);
        }

        return baseHierarchicalElement;
    }

    void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r -> {
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, r.adaptTo(Session.class), true);
            manager.setCurrentItem(fileBasePath);
            baseFolder.visitAllFolders(folder -> {
                if (canImportFolder(folder)) {
                    manager.deferredWithResolver(Actions.retry(retries, retryPause, rr -> {
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
                        addFileImportTask(fileSource, manager);
                    }
                }
            });
        });
    }

    private void addFileImportTask(Source fileSource, ActionManager manager) {
        try {
            if (canImportFile(fileSource)) {
                manager.deferredWithResolver(Actions.retry(retries, retryPause, importAsset(fileSource, manager)));
            } else {
                incrementCount(skippedFiles, 1);
                trackDetailedActivity(fileSource.getName(), "Skip", "Skipping file", 0L);
            }
        } catch (IOException ex) {
            Failure failure = new Failure();
            failure.setException(ex);
            failure.setNodePath(fileSource.getElement().getNodePath(preserveFileName));
            manager.getFailureList().add(failure);
        } finally {
            try {
                fileSource.close();
            } catch (IOException ex) {
                Failure failure = new Failure();
                failure.setException(ex);
                failure.setNodePath(fileSource.getElement().getNodePath(preserveFileName));
                manager.getFailureList().add(failure);
            }
        }
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
            return file.getPath();
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

        SftpHierarchicalElement(String uri) throws URISyntaxException, UnsupportedEncodingException {
            this.sourcePath = uri;
            this.uri = new URI(encodeUriParts(uri));
            this.path = decodeUriParts(this.uri.getRawPath());
        }

        SftpHierarchicalElement(String uri, ChannelSftp channel, boolean holdOpen) throws URISyntaxException, UnsupportedEncodingException {
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

        private ChannelSftp openChannel() throws JSchException {
            if (channel == null || !channel.isConnected()) {
                JSch jsch = new JSch();
                int port = uri.getPort() <= 0 ? 22 : uri.getPort();

                com.jcraft.jsch.Session session = jsch.getSession(username, uri.getHost(), port);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(timeout);
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
                try {
                    channel.getSession().disconnect();
                } catch (JSchException ex) {
                    // Ignore possible exception thrown by getSession()
                }
            }
            channel = null;
        }

        private void retrieveDetails() throws JSchException, SftpException {
            if (!retrieved) {
                try {
                    openChannel();
                    SftpATTRS attributes = channel.lstat(path);
                    processAttrs(attributes);
                } finally {
                    if (!keepChannelOpen) {
                        closeChannel();
                    }
                }
            }
        }

        private void processAttrs(SftpATTRS attrs) {
            isFile = !attrs.isDir();
            size = attrs.getSize();
            retrieved = true;
        }

        @Override
        public boolean isFile() {
            try {
                retrieveDetails();
            } catch (JSchException | SftpException ex) {
                LOG.error("Cannot access remote system: {}", ex.getMessage());
            }
            return isFile;
        }

        @Override
        public HierarchicalElement getParent() {
            if (parent == null && !fileBasePath.equals(getSourcePath())) {
                try {
                    parent = new SftpHierarchicalElement(StringUtils.substringBeforeLast(getSourcePath(), "/"));
                } catch (URISyntaxException | UnsupportedEncodingException  ex) {
                    LOG.error("Cannot determine parent path for '{}': {}", getSourcePath(), ex.getMessage());
                }
            }
            return parent;
        }

        @SuppressWarnings("squid:S1149")
        @Override
        public Stream<HierarchicalElement> getChildren() {
            try {
                openChannel();
                Vector<ChannelSftp.LsEntry> children = channel.ls(path);
                return children.stream()
                        .filter(this::isNotDotFolder)
                        .map(this::getChildFromEntry)
                        .filter(Objects::nonNull);
            } catch (JSchException | SftpException ex) {
                LOG.error("Cannot retrieve child list for '{}': {}", path, ex.getMessage());
                return Stream.empty();
            } finally {
                if (!keepChannelOpen) {
                    closeChannel();
                }
            }
        }

        private boolean isNotDotFolder(ChannelSftp.LsEntry entry) {
            return !(".".equals(entry.getFilename()) || "..".equals(entry.getFilename()));
        }

        private HierarchicalElement getChildFromEntry(ChannelSftp.LsEntry entry) {
            String childPath = null;
            try {
                childPath = getSourcePath() + "/" + entry.getFilename();
                SftpHierarchicalElement child = new SftpHierarchicalElement(childPath, channel, true);
                child.processAttrs(entry.getAttrs());
                return child;
            } catch (URISyntaxException | UnsupportedEncodingException ex) {
                LOG.error("Cannot list children for '{}': {}", childPath, ex.getMessage());
                return null;
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
                } catch (JSchException | SftpException ex) {
                    LOG.error("Cannot determine source: {}", ex.getMessage());
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
                lastStream = lastChannel.get(element.getItemName());
            } catch (Exception ex) {
                LOG.error("Error while obtaining inpustream for file '{}': {}", element.getItemName(), ex.getMessage());
                close();
                throw new IOException("Error in retrieving file " + element.getItemName(), ex);
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
                try {
                    lastChannel.getSession().disconnect();
                } catch (JSchException ex) {
                    // Ignore possible exception thrown by getSession()
                }
                lastChannel = null;
            }
        }
    }
}
