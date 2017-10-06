/*
 * Copyright 2017 Adobe.
 *
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
 */
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.day.cq.commons.jcr.JcrUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;

/**
 * Asset Ingestor reads a directory structure recursively and imports it as-is into AEM.
 */
public class FileAssetIngestor extends AssetIngestor {

    public FileAssetIngestor(MimeTypeService mimeTypeService) {
        super(mimeTypeService);
    }
    @FormField(
            name = "Source",
            description = "Source folder for content ingestion",
            hint = "/var/mycontent, /tmp, /mnt/all_the_things, ...",
            required = true
    )    
    String fileBasePath;
    File baseFolder;
    
    @Override
    public void init() throws RepositoryException {
        baseFolder = new File(fileBasePath);
        if (!baseFolder.exists()) {
            throw new RepositoryException("Source folder does not exist!");
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
        manager.deferredWithResolver(r->{
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, r.adaptTo(Session.class), true);
            manager.setCurrentItem(fileBasePath);
            Files.walk(baseFolder.toPath()).map(Path::toFile).filter(f -> !f.equals(baseFolder)).
                    map(FileHierarchialElement::new).filter(FileHierarchialElement::isFolder).filter(this::canImportFolder).forEach(f->{
                manager.deferredWithResolver(Actions.retry(10, 100, rr-> {
                    manager.setCurrentItem(f.getItemName());
                    createFolderNode(f, rr);
                }));
            });
        });
    }

    void importAssets(ActionManager manager) throws IOException {
        manager.deferredWithResolver(rr->{
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, rr.adaptTo(Session.class), true);
            manager.setCurrentItem(fileBasePath);
            Files.walk(baseFolder.toPath()).map(FileHierarchialElement::new).filter(FileHierarchialElement::isFile).
                    filter(this::canImportContainingFolder).map(FileHierarchialElement::getSource).forEach(fs->{
                if (canImportFile(fs)) {
                    manager.deferredWithResolver(Actions.retry(5, 25, importAsset(fs, manager)));
                } else {
                    filesSkipped.incrementAndGet();
                }
            });        
        });
    }

    private class FileSource implements Source {
        private final File file;
        private final HierarchialElement element;

        private FileSource(File f, FileHierarchialElement el) {
            this.file = f;
            this.element = el;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public InputStream getStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public long getLength() {
            return file.length();
        }

        @Override
        public HierarchialElement getElement() {
            return element;
        }
    }

    class FileHierarchialElement implements HierarchialElement {

        private final File file;

        FileHierarchialElement(File f) {
            this.file = f;
        }

        private FileHierarchialElement(Path p) {
            this(p.toFile());
        }

        @Override
        public Source getSource() {
            return new FileSource(file, this);
        }

        @Override
        public String getItemName() {
            return file.getAbsolutePath();
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public HierarchialElement getParent() {
            File parent = file.getParentFile();
            if (parent.equals(baseFolder)) {
                return null;
            }
            return new FileHierarchialElement(file.getParentFile());
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
    }

}