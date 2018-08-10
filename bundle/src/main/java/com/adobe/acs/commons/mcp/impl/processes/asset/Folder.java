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

/**
 * Represents a folder to be imported
 */
public class Folder implements HierarchialElement {
    HierarchialElement parent;
    String name;
    String basePath;

    public Folder(String name, Folder parent) {
        this.name = name;
        this.parent = parent;
    }
    
    public Folder(String name, String basePath) {
        this.name = name;
        this.basePath = basePath;
    }
    
    
    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public HierarchialElement getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getItemName() {
        return name;
    }

    @Override
    public AssetIngestor.Source getSource() {
        throw new UnsupportedOperationException("This implementation of folder does not provide a source.");
    }

    @Override
    public String getJcrBasePath() {
        return basePath;
    }
    
}
