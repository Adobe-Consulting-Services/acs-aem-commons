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
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PasswordComponent;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

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
            description = "S3 Secret Key",
            component = PasswordComponent.class
    )
    String secretKey;

    @FormField(
            name = "S3 Base Path",
            description = "S3 Base Path (Prefix)",
            required = false
    )
    String s3BasePath;

    @FormField(
            name = "Endpoint URL",
            description = "Endpoint URL, leave blank for default. Used primarily for S3-compatible object-storage solutions.",
            required = false
    )
    String endpointUrl;

    transient AmazonS3 s3Client;

    transient String baseItemName;

    @Override
    public void init() throws RepositoryException {
        super.init();
        if (StringUtils.isNotBlank(s3BasePath)) {
            baseItemName = bucket + ":" + s3BasePath;
        } else {
            baseItemName = bucket;
        }
        if (StringUtils.isNotBlank(endpointUrl)) {
            baseItemName = endpointUrl + "/" + baseItemName;
        }
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        if (StringUtils.isNotBlank(s3BasePath) && !s3BasePath.endsWith("/")) {
            s3BasePath = s3BasePath + "/";
        }
        instance.getInfo().setDescription(baseItemName + "->" + jcrBasePath);
        instance.defineCriticalAction("Create Folders", rr, this::createFolders);
        instance.defineCriticalAction("Import Assets", rr, this::importAssets);
        s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
        if (StringUtils.isNotBlank(endpointUrl)) {
            s3Client.setEndpoint(endpointUrl);
        }
    }

    void createFolders(ActionManager manager) {
        manager.deferredWithResolver(r -> {
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, r.adaptTo(Session.class), true);
            manager.setCurrentItem(baseItemName);

            ObjectListing listing = s3Client.listObjects(bucket, s3BasePath);
            createFolders(manager, listing);
        });
    }

    private void createFolders(ActionManager manager, ObjectListing listing) {
        listing.getObjectSummaries().stream().filter(sum -> !sum.getKey().equals(s3BasePath)).map(S3HierarchicalElement::new)
                .filter(S3HierarchicalElement::isFolder).filter(this::canImportFolder).forEach(el -> {
            manager.deferredWithResolver(Actions.retry(10, 100, rr -> {
                manager.setCurrentItem(el.getItemName());
                createFolderNode(el, rr);
            }));
        });
        if (listing.isTruncated()) {
            createFolders(manager, s3Client.listNextBatchOfObjects(listing));
        }
    }

    void importAssets(ActionManager manager) {
        manager.deferredWithResolver(rr -> {
            JcrUtil.createPath(jcrBasePath, DEFAULT_FOLDER_TYPE, DEFAULT_FOLDER_TYPE, rr.adaptTo(Session.class), true);
            manager.setCurrentItem(baseItemName);
            ObjectListing listing = s3Client.listObjects(bucket, s3BasePath);
            importAssets(manager, listing);
        });
    }

    private void importAssets(ActionManager manager, ObjectListing listing) {
        listing.getObjectSummaries().stream().map(S3HierarchicalElement::new)
                .filter(S3HierarchicalElement::isFile).filter(this::canImportContainingFolder)
                .map(S3HierarchicalElement::getSource).forEach(ss -> {
            try {
                if (canImportFile(ss)) {
                    manager.deferredWithResolver(Actions.retry(5, 25, importAsset(ss, manager)));
                } else {
                    incrementCount(skippedFiles, 1);
                    trackDetailedActivity(ss.getName(), "Skip", "Skipping file", 0L);
                }
            } catch (IOException ex) {
                Failure failure = new Failure();
                failure.setException(ex);
                failure.setNodePath(ss.getElement().getNodePath());
                manager.getFailureList().add(failure);
            } finally {
                try {
                    ss.close();
                } catch (IOException ex) {
                    Failure failure = new Failure();
                    failure.setException(ex);
                    failure.setNodePath(ss.getElement().getNodePath());
                    manager.getFailureList().add(failure);
                }
            }
        });
        if (listing.isTruncated()) {
            importAssets(manager, s3Client.listNextBatchOfObjects(listing));
        }
    }

    private class S3Source implements Source {

        final S3ObjectSummary s3ObjectSummary;
        private S3ObjectInputStream lastOpenStream;
        final HierarchicalElement element;

        private S3Source(S3ObjectSummary s3ObjectSummary, S3HierarchicalElement element) {
            this.s3ObjectSummary = s3ObjectSummary;
            this.element = element;
        }

        @Override
        public long getLength() {
            return s3ObjectSummary.getSize();
        }

        @Override
        public InputStream getStream() throws IOException {
            close();
            lastOpenStream = s3Client.getObject(bucket, s3ObjectSummary.getKey()).getObjectContent();
            return lastOpenStream;
        }

        @Override
        public String getName() {
            return element.getName();
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

    class S3HierarchicalElement implements HierarchicalElement {

        final S3ObjectSummary original;
        final String negativePath;
        final String effectiveKey;

        S3HierarchicalElement(S3ObjectSummary original) {
            this(original, null);
        }

        private S3HierarchicalElement(S3ObjectSummary original, String negativePath) {
            this.original = original;
            this.negativePath = negativePath != null ? negativePath : "";
            this.effectiveKey = original.getKey().substring(0, original.getKey().length() - this.negativePath.length());
        }

        @Override
        public Stream<HierarchicalElement> getChildren() {
            throw new UnsupportedOperationException("S3 Elements do not support navigation children directly");
        }        
        
        @Override
        public boolean isFile() {
            return !isFolder();
        }

        @Override
        public boolean isFolder() {
            return effectiveKey.endsWith("/");
        }

        @Override
        public HierarchicalElement getParent() {
            if (isFolder()) {
                String newNegativePath = getName() + "/" + this.negativePath;
                String newEffectiveKey = original.getKey().substring(0, original.getKey().length() - newNegativePath.length());
                if (newNegativePath.equals(original.getKey()) || newEffectiveKey.equals(s3BasePath)) {
                    return null;
                }
                return new S3HierarchicalElement(original, newNegativePath);
            } else {
                String newNegativePath = getName();
                String newEffectiveKey = original.getKey().substring(0, original.getKey().length() - newNegativePath.length());
                if (newNegativePath.equals(original.getKey()) || newEffectiveKey.equals(s3BasePath)) {
                    return null;
                }
                return new S3HierarchicalElement(original, newNegativePath);
            }
        }

        @Override
        public String getName() {
            String keyWithoutTrailingSlash;
            if (isFolder()) {
                keyWithoutTrailingSlash = effectiveKey.substring(0, effectiveKey.length() - 1);
            } else {
                keyWithoutTrailingSlash = effectiveKey;
            }
            String name = StringUtils.substringAfterLast(keyWithoutTrailingSlash, "/");
            if (StringUtils.isEmpty(name)) {
                return keyWithoutTrailingSlash;
            } else {
                return name;
            }
        }

        @Override
        public String getItemName() {
            return bucket + ":" + effectiveKey;
        }

        @Override
        public Source getSource() {
            if (StringUtils.isNotBlank(negativePath)) {
                return null;
            } else {
                return new S3Source(original, this);
            }
        }

        @Override
        public String getJcrBasePath() {
            return jcrBasePath;
        }

        @Override
        public String getSourcePath() {
            return getItemName();
        }
    }
}
