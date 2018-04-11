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

import com.adobe.acs.commons.mcp.impl.processes.asset.AssetIngestor.HierarchialElement;
import com.adobe.acs.commons.mcp.impl.processes.asset.AssetIngestor.Source;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

/**
 * Abstraction of a file which might be an asset or a rendition of another asset
 */
public class FileOrRendition implements HierarchialElement {

    private final String url;
    private final String name;
    private final Folder folder;
    private Source fileSource;
    private final Map<String, FileOrRendition> additionalRenditions;
    private String renditionName = null;
    private String originalAssetName = null;
    private Map<String, String> properties;
    private Supplier<HttpClient> clientProvider;

    public FileOrRendition(Supplier<HttpClient> clientProvider, String name, String url, Folder folder, Map<String, String> data) {
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
    public HierarchialElement getParent() {
        return folder;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getItemName() {
        return getName();
    }

    @Override
    public Source getSource() {
        if (fileSource == null) {
            FileOrRendition thizz = this;
            if (url.toLowerCase().startsWith("http")) {
                fileSource = new HttpConnectionSource(thizz);
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

    public String getProperty(String prop) {
        return properties.get(prop);
    }

    private class UrlConnectionSource implements Source {

        private final FileOrRendition thizz;

        public UrlConnectionSource(FileOrRendition thizz) {
            this.thizz = thizz;
        }
        Long size = null;
        URLConnection connection = null;
        InputStream lastOpenStream = null;

        @Override
        public String getName() {
            return name;
        }

        private URLConnection getConnection() throws IOException {
            if (connection == null) {
                URL theUrl = new URL(url);
                connection = theUrl.openConnection();
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
        public long getLength() {
            if (size == null) {
                try {
                    size = getConnection().getContentLengthLong();
                } catch (IOException ex) {
                    Logger.getLogger(FileOrRendition.class.getName()).log(Level.SEVERE, null, ex);
                    size = -1L;
                }
            }
            return size;
        }

        @Override
        public HierarchialElement getElement() {
            return thizz;
        }

        public void close() throws IOException {
            if (lastOpenStream != null) {
                lastOpenStream.close();
            }
            lastOpenStream = null;
        }
    }

    private class HttpConnectionSource implements Source {

        private final FileOrRendition thizz;

        public HttpConnectionSource(FileOrRendition thizz) {
            this.thizz = thizz;
        }
        Long size = null;
        HttpResponse connection = null;

        @Override
        public String getName() {
            return name;
        }

        private HttpResponse downloadResource() throws IOException {
            if (connection == null) {
                connection = clientProvider.get().execute(new HttpGet(url));
            }
            return connection;
        }

        @Override
        public InputStream getStream() throws IOException {
            HttpResponse c = downloadResource();
            connection = null;
            return c.getEntity().getContent();
        }

        @Override
        public long getLength() {
            if (size == null) {
                try {
                    size = downloadResource().getEntity().getContentLength();
                } catch (IOException ex) {
                    Logger.getLogger(FileOrRendition.class.getName()).log(Level.SEVERE, null, ex);
                    size = -1L;
                }
            }
            return size;
        }

        @Override
        public HierarchialElement getElement() {
            return thizz;
        }

        public void close() throws IOException {
            if (connection != null && connection.getEntity() != null && connection.getEntity().getContent() != null) {
                connection.getEntity().getContent().close();
            }
        }
    }

}
