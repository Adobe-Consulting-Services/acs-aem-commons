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

import java.util.stream.Stream;

/**
 * Represents a folder to be imported
 */
public class Folder implements HierarchicalElement {
    HierarchicalElement parent;
    // Note: in this implementation this is the url of the requested asset that generated this folder
    String sourcePath;
    String name;
    String basePath;

    public Folder(String name, Folder parent, String requestedPath) {
        this.name = name;
        this.parent = parent;
        this.sourcePath = requestedPath;
    }
    
    public Folder(String name, String basePath, String requestedPath) {
        this.name = name;
        this.basePath = basePath;
        this.sourcePath = requestedPath;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public HierarchicalElement getParent() {
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
    public Source getSource() {
        throw new UnsupportedOperationException("This implementation of folder does not provide a source.");
    }

    @Override
    public String getJcrBasePath() {
        return basePath;
    }

    @Override
    public Stream<HierarchicalElement> getChildren() {
        throw new UnsupportedOperationException("Folder does not support child navigation at the moment");
    }

    @Override
    public String getSourcePath() {
        return sourcePath;
    }
}
