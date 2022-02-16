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
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.mcp.impl.processes.asset.HierarchicalElement.UriHelper.decodeUriParts;
import static com.adobe.acs.commons.mcp.impl.processes.asset.HierarchicalElement.UriHelper.encodeUriParts;

/**
 * Abstraction of a file which might be an asset or a rendition of another asset
 */
public class FileOrRendition implements HierarchicalElement {

    private static final Logger LOG = LoggerFactory.getLogger(FileOrRendition.class);

    final String url;
    final String name;
    final Folder folder;
    private Source fileSource;
    private final Map<String, FileOrRendition> additionalRenditions;
    String renditionName = null;
    String originalAssetName = null;
    Map<String, CompositeVariant> properties;
    ClientProvider clientProvider;

    public FileOrRendition(ClientProvider clientProvider, String name, String url, Folder folder, Map<String, CompositeVariant> data) {
        if (folder == null) {
            throw new NullPointerException("Folder cannot be null");
        }
        this.name = name;
        this.url = url;
        this.folder = folder;
        this.properties = new HashMap<>(data);
        this.clientProvider = clientProvider;
        additionalRenditions = new TreeMap<>();
    }

    public void setAsRenditionOfImage(String renditonName, String originalAssetName) {
        this.renditionName = renditonName;
        this.originalAssetName = originalAssetName;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    public boolean isRendition() {
        return renditionName != null && !renditionName.isEmpty();
    }

    public void addRendition(FileOrRendition rendition) {
        additionalRenditions.put(rendition.renditionName, rendition);
    }

    public Map<String, FileOrRendition> getRenditions() {
        return additionalRenditions;
    }

    public String getOriginalAssetName() {
        return originalAssetName;
    }

    @Override
    public HierarchicalElement getParent() {
        return folder;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getItemName() {
        try {
            URI uri = new URI(encodeUriParts(url));
            String filePath = decodeUriParts(uri.getRawPath());
            return decodeUriParts(filePath);
        } catch (URISyntaxException | UnsupportedEncodingException ex) {
            LOG.error("Cannot decode url '{}'", url, ex);
        }
        return url;
    }

    @Override
    public Source getSource() {
        if (fileSource == null) {
            FileOrRendition thizz = this;
            if (url.toLowerCase().startsWith("http")) {
                fileSource = new HttpConnectionSource(thizz);
            } else if (url.toLowerCase().startsWith("sftp")) {
                fileSource = new SftpConnectionSource(thizz);
            } else {
                fileSource = new UrlConnectionSource(thizz);
            }
        }
        return fileSource;
    }

    @Override
    public String getJcrBasePath() {
        return folder.getJcrBasePath();
    }

    @Override
    public String getSourcePath() {
        return url;
    }

    public Map<String, CompositeVariant> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public CompositeVariant getProperty(String prop) {
        return properties.get(prop);
    }

    @Override
    public Stream<HierarchicalElement> getChildren() {
        throw new UnsupportedOperationException("FileOrRendition doesn't support child navigation");
    }

    public class UrlConnectionSource implements Source {

        final FileOrRendition thizz;
        private Long size = null;
        private URLConnection connection = null;
        private InputStream lastOpenStream = null;

        public UrlConnectionSource(FileOrRendition thizz) {
            this.thizz = thizz;
        }

        @Override
        public String getName() {
            return name;
        }

        private URLConnection getConnection() throws IOException {
            if (connection == null) {
                URL theUrl = new URL(url);
                connection = theUrl.openConnection();
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);
            }
            return connection;
        }

        @Override
        public InputStream getStream() throws IOException {
            close();
            URLConnection c = getConnection();
            connection = null;
            return c.getInputStream();
        }

        @Override
        public long getLength() throws IOException {
            if (size == null) {
                try {
                    size = getConnection().getContentLengthLong();
                } catch (IOException ex) {
                    size = -1L;
                    throw ex;
                }
            }
            return size;
        }

        @Override
        public HierarchicalElement getElement() {
            return thizz;
        }

        @Override
        public void close() throws IOException {
            if (lastOpenStream != null) {
                lastOpenStream.close();
            }
            lastOpenStream = null;
        }
    }

    public class HttpConnectionSource implements Source {

        final FileOrRendition thizz;
        private HttpGet lastRequest;
        private Long size = null;
        private HttpResponse connection = null;

        public HttpConnectionSource(FileOrRendition thizz) {
            this.thizz = thizz;
        }

        @Override
        public String getName() {
            return name;
        }

        private HttpResponse initiateDownload() throws IOException {
            if (connection == null) {
                try {
                    lastRequest = new HttpGet(url);
                    connection = clientProvider.getHttpClientSupplier().get().execute(lastRequest);
                    size = connection.getEntity().getContentLength();
                } catch (IOException | IllegalArgumentException ex) {
                    size = -1L;
                    throw new IOException("Error with URL " + url + ": " + ex.getMessage(), ex);
                }
            }
            return connection;
        }

        @Override
        public InputStream getStream() throws IOException {
            HttpResponse c = initiateDownload();
            return c.getEntity().getContent();
        }

        @Override
        public long getLength() throws IOException {
            if (size == null) {
                initiateDownload();
            }
            return size;
        }

        @Override
        public HierarchicalElement getElement() {
            return thizz;
        }

        @Override
        public void close() throws IOException {
            if (lastRequest != null) {
                lastRequest.releaseConnection();
                lastRequest = null;
            }
            if (connection != null && connection.getEntity() != null && connection.getEntity().getContent() != null) {
                connection.getEntity().getContent().close();
                connection = null;
            }
        }
    }

    public class SftpConnectionSource implements Source {

        final FileOrRendition thizz;
        private final JSch jsch = new JSch();
        private Session session;
        private InputStream currentStream;
        private Channel channel;

        public SftpConnectionSource(FileOrRendition thizz) {
            this.thizz = thizz;
        }

        public Session getSessionForHost(URI uri) throws IOException {
            if (session != null && !session.getHost().equals(uri.getHost())) {
                close();
            }
            if (session == null || !session.isConnected()) {
                try {
                    int port = uri.getPort() <= 0 ? 22 : uri.getPort();
                    session = jsch.getSession(clientProvider.getUsername(), uri.getHost(), port);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.setPassword(clientProvider.getPassword());
                    session.connect();
                } catch (JSchException ex) {
                    throw new IOException("Unable to connect to server", ex);
                }
            }
            return session;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream getStream() throws IOException {
            try {
                URI uri = new URI(encodeUriParts(getSourcePath()));

                if (channel == null || channel.isClosed()) {
                    channel = getSessionForHost(uri).openChannel("sftp");
                    channel.connect();
                }

                ChannelSftp sftpChannel = (ChannelSftp) channel;
                currentStream = sftpChannel.get(decodeUriParts(uri.getRawPath()));

                return currentStream;

            } catch (URISyntaxException ex) {
                throw new IOException("Bad URI format", ex);
            } catch (JSchException ex) {
                throw new IOException("Error with connection", ex);
            } catch (SftpException ex) {
                throw new IOException("Error retrieving file", ex);
            }
        }

        @Override
        public long getLength() throws IOException {
            try {
                URI uri = new URI(encodeUriParts(getSourcePath()));

                if (channel == null || channel.isClosed()) {
                    channel = getSessionForHost(uri).openChannel("sftp");
                    channel.connect();
                }

                ChannelSftp sftpChannel = (ChannelSftp) channel;
                SftpATTRS stats = sftpChannel.lstat(decodeUriParts(uri.getRawPath()));
                return stats.getSize();
            } catch (URISyntaxException ex) {
                throw new IOException("Error parsing URL", ex);
            } catch (SftpException ex) {
                throw new IOException("Error getting file stats", ex);
            } catch (JSchException ex) {
                throw new IOException("Error opening SFTP channel", ex);
            }
        }

        @Override
        public HierarchicalElement getElement() {
            return thizz;
        }

        @Override
        public void close() throws IOException {
            if (currentStream != null) {
                currentStream.close();
            }
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
                channel = null;
            }
            if (session != null) {
                session.disconnect();
                session = null;
            }
        }

    }
}
