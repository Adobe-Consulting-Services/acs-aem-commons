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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;

public class S3AssetIngestor extends AssetIngestor {

    public S3AssetIngestor(MimeTypeService mimeTypeService) {
        super(mimeTypeService);
    }

    @FormField(
            name = "Bucket",
            description = "S3 Bucket Name"
    )
    String bucket;

    @FormField(
            name = "Access Key",
            description = "S3 Access Key"
    )
    String accessKey;

    @FormField(
            name = "Secret Key",
            description = "S3 Secret Key"
    )
    String secretKey;

    @FormField(
            name = "S3 Base Path",
            description = "S3 Base Path (Prefix)"
    )
    String s3BasePath;

    transient AmazonS3 s3Client;

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        if (StringUtils.isNotBlank(s3BasePath) && !s3BasePath.endsWith("/")) {
            s3BasePath = s3BasePath + "/";
        }
        instance.getInfo().setDescription(generateItemName(s3BasePath) + "->" + jcrBasePath);
        instance.defineCriticalAction("Create Folders", rr, this::createFolders);
        instance.defineCriticalAction("Import Assets", rr, this::importAssets);
        s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
    }

    void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r->{
            manager.setCurrentItem(generateItemName(s3BasePath));

            ObjectListing listing = s3Client.listObjects(bucket, s3BasePath);
            createFolders(manager, listing);
        });
    }
    private void createFolders(ActionManager manager, ObjectListing listing) {
        listing.getObjectSummaries().stream().
            filter(this::isFolder).filter(this::canImportFolder).forEach(s-> {
                manager.deferredWithResolver(Actions.retry(10, 100, rr-> {
                    String key = s.getKey();
                    manager.setCurrentItem(generateItemName(key));
                    createFolderNode(keyToNodePath(key), key, rr);
                }));
        });
        if (listing.isTruncated()) {
            createFolders(manager, s3Client.listNextBatchOfObjects(listing));
        }
    }

    void importAssets(ActionManager manager) throws IOException {
        manager.deferredWithResolver(rr->{
            manager.setCurrentItem(generateItemName(s3BasePath));
            ObjectListing listing = s3Client.listObjects(bucket, s3BasePath);
            importAssets(manager, listing);
        });
    }

    private void importAssets(ActionManager manager, ObjectListing listing) {
        listing.getObjectSummaries().stream().
                filter(this::isFile).filter(this::canImportContainingFolder).map(S3Source::new).forEach(ss-> {
            if (canImportFile(ss)) {
                manager.deferredWithResolver(Actions.retry(5, 25, importFile(ss, manager)));
            } else {
                filesSkipped++;
            }
        });
        if (listing.isTruncated()) {
            createFolders(manager, s3Client.listNextBatchOfObjects(listing));
        }
    }

    boolean canImportContainingFolder(S3ObjectSummary s3ObjectSummary) {
        String key = s3ObjectSummary.getKey();
        if (key.indexOf("/") >= 0) {
            String parentPath = StringUtils.substringBeforeLast(key, "/");
            return canImportFolder(parentPath);
        } else {
            return true;
        }
    }

    private boolean isFile(S3ObjectSummary s3ObjectSummary) {
        return !isFolder(s3ObjectSummary);
    }


    private CheckedConsumer<ResourceResolver> importFile(final S3Source source, ActionManager actionManager) {
        return (ResourceResolver r) -> {
            String path = keyToNodePath(source.s3ObjectSummary.getKey());
            createFolderNode(StringUtils.substringBeforeLast(path, "/"), StringUtils.substringBeforeLast(source.s3ObjectSummary.getKey(), "/"), r);
            actionManager.setCurrentItem(bucket + ":" + source.s3ObjectSummary.getKey());
            handleExistingAsset(source, path, r);
        };
    }

    String keyToNodePath(String key) {
        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1); // remove trailing slash
        }
        if ((StringUtils.isBlank(key) && StringUtils.isBlank(s3BasePath)) ||
                (key.equals(s3BasePath) || (key + "/").equals(s3BasePath))) {
            return jcrBasePath;
        } else if (key.indexOf("/") > -1) {
            return keyToNodePath(StringUtils.substringBeforeLast(key, "/")) + "/" + JcrUtil.createValidName(getName(key));
        } else {
            return jcrBasePath + "/" + JcrUtil.createValidName(key);
        }
    }

    private boolean createFolderNode(String folderPath, String folderKey, ResourceResolver r) throws RepositoryException, PersistenceException {
        String name = getName(folderKey);
        Session s = r.adaptTo(Session.class);
        if (s.nodeExists(folderPath)) {
            Node folderNode = s.getNode(folderPath);
            if (folderNode.hasProperty(JcrConstants.JCR_TITLE) && folderNode.getProperty(JcrConstants.JCR_TITLE).getString().equals(name)) {
                return false;
            } else {
                folderNode.setProperty(JcrConstants.JCR_TITLE, name);
                r.commit();
                r.refresh();
                return true;
            }
        }
        String parentNode = StringUtils.substringBeforeLast(folderPath, "/");
        String childNode = StringUtils.substringAfterLast(folderPath, "/");
        if (!jcrBasePath.equals(parentNode)) {
            String parentKey = StringUtils.substringBeforeLast(folderKey, "/");
            createFolderNode(parentNode, parentKey, r);
        }
        Node child = s.getNode(parentNode).addNode(childNode, DEFAULT_FOLDER_TYPE);
        folderCount++;
        child.setProperty(JcrConstants.JCR_TITLE, name);
        r.commit();
        r.refresh();
        return true;
    }

    private String generateItemName(String key) {
        if (StringUtils.isBlank(key)) {
            return bucket;
        } else {
            return bucket + ":" + key;
        }
    }

    private boolean isFolder(S3ObjectSummary s3ObjectSummary) {
        return isFolder(s3ObjectSummary.getKey());
    }

    private boolean isFolder(String key) {
        return key.endsWith("/");
    }

    boolean canImportFolder(S3ObjectSummary s3ObjectSummary) {
        return canImportFolder(s3ObjectSummary.getKey().substring(0, s3ObjectSummary.getKey().length() - 1));
    }

    private boolean canImportFolder(String key) {
        String name = getName(key);
        if (ignoreFolderList.contains(name.toLowerCase())) {
            return false;
        } else if (key.indexOf("/") >= 0) {
            String parentPath = StringUtils.substringBeforeLast(key, "/");
            return canImportFolder(parentPath);
        } else {
            return true;
        }
    }

    String getName(S3ObjectSummary s3ObjectSummary) {
        final String key = s3ObjectSummary.getKey();
        return getName(key);
    }

    private String getName(String key) {
        if (isFolder(key)) {
            String folderPath = key.substring(0, key.length() - 1); // remove last slash
            String name = StringUtils.substringAfterLast(folderPath, "/");
            if (StringUtils.isEmpty(name)) {
                return folderPath;
            } else {
                return name;
            }
        } else {
            String name =  StringUtils.substringAfterLast(key, "/");
            if (StringUtils.isEmpty(name)) {
                return key;
            } else {
                return name;
            }
        }
    }

    private class S3Source implements Source {

        private final S3ObjectSummary s3ObjectSummary;

        private S3Source(S3ObjectSummary s3ObjectSummary) {
            this.s3ObjectSummary = s3ObjectSummary;
        }

        @Override
        public long getLength() {
            return s3ObjectSummary.getSize();
        }

        @Override
        public InputStream getStream() throws IOException {
            return s3Client.getObject(bucket, s3ObjectSummary.getKey()).getObjectContent();
        }

        @Override
        public String getName() {
            return S3AssetIngestor.this.getName(s3ObjectSummary);
        }
    }
}
