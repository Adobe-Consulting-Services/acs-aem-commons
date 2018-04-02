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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstraction of a file which might be an asset or a rendition of another asset
 */
public class FileOrRendition extends HashMap<String, String> implements HierarchialElement {
    private final String url;
    private final String name;
    private final Folder folder;
    private Source fileSource;
    private final Map<String, FileOrRendition> additionalRenditions;
    private String renditionName = null;
    private String originalAssetName = null;
    
    public FileOrRendition(String name, String url, Folder folder, Map<String, String> data) {
        if (folder == null) {
            throw new NullPointerException("Folder cannot be null");
        }
        this.name = name;
        this.url = url;
        this.folder = folder;
        putAll(data);
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
            fileSource = new Source() {
                Long size = null;
                URLConnection connection = null;

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
            };
        }
        return fileSource;
    }

    @Override
    public String getJcrBasePath() {
        return folder.getJcrBasePath();
    }
    
}