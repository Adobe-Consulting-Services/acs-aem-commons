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
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
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
    
    private void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r->{
            manager.setCurrentItem(fileBasePath);
            Files.walk(baseFolder.toPath()).map(Path::toFile).filter(File::isDirectory).filter(this::canImportFolder).forEach(f->{
                manager.deferredWithResolver(Actions.retry(10, 100, rr-> {
                    manager.setCurrentItem(f.getPath());
                    createFolderNode(folderToNodePath(f), f, rr);
                }));
            });
        });
    }

    private String folderToNodePath(File current) {
        if (baseFolder.equals(current)) {
            return jcrBasePath;
        } else {
            return folderToNodePath(current.getParentFile()) + "/" + JcrUtil.createValidName(current.getName());
        }
    }
    
    private boolean createFolderNode(String node, File folder, ResourceResolver r) throws RepositoryException, PersistenceException {
        Session s = r.adaptTo(Session.class);
        if (s.nodeExists(node)) {
            Node folderNode = s.getNode(node);
            if (folderNode.hasProperty(JcrConstants.JCR_TITLE) && folderNode.getProperty(JcrConstants.JCR_TITLE).getString().equals(folder.getName())) {
                return false;
            } else {
                folderNode.setProperty(JcrConstants.JCR_TITLE, folder.getName());
                r.commit();
                r.refresh();
                return true;
            }
        }
        String parentNode = node.substring(0, node.lastIndexOf("/"));
        String childNode = node.substring(node.lastIndexOf("/") + 1);
        if (!folder.getParentFile().equals(baseFolder)) {
            createFolderNode(parentNode, folder.getParentFile(), r);
        }
        Node child = s.getNode(parentNode).addNode(childNode, DEFAULT_FOLDER_TYPE);
        folderCount++;
        child.setProperty(JcrConstants.JCR_TITLE, folder.getName());
        r.commit();
        r.refresh();
        return true;
    }

    private void importAssets(ActionManager manager) throws IOException {
        manager.deferredWithResolver(rr->{
            Actions.setCurrentItem(fileBasePath);
            Files.walk(baseFolder.toPath()).map(Path::toFile).filter(File::isFile).filter(f->canImportFolder(f.getParentFile())).map(FileSource::new).forEach(fs->{
                if (canImportFile(fs)) {
                    manager.deferredWithResolver(Actions.retry(5, 25, importFile(fs)));
                } else {
                    filesSkipped++;
                }
            });        
        });
    }

    private CheckedConsumer<ResourceResolver> importFile(final FileSource source) {
        return (ResourceResolver r) -> {
            String basePath = folderToNodePath(source.file.getParentFile());
            createFolderNode(basePath, source.file.getParentFile(), r);
            String destPath = basePath + "/" + source.file.getName();
            Actions.setCurrentItem(destPath);
            handleExistingAsset(source, destPath, r);
        };
    }

    private boolean canImportFolder(File f) {
        if (f.equals(baseFolder)) {
            return true;
        }
        if (ignoreFolderList.contains(f.getName().toLowerCase())) {
            return false;
        }
        return canImportFolder(f.getParentFile());
    }

    private class FileSource implements Source {
        private final File file;

        private FileSource(File f) {
            this.file = f;
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
    }

}