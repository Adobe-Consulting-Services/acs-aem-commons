/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

public class ContentSync {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RemoteInstance remoteInstance;
    private ContentImporter importer;
    private ResourceResolver resourceResolver;

    public ContentSync(RemoteInstance remoteInstance, ResourceResolver resourceResolver, ContentImporter importer) {
        this.remoteInstance = remoteInstance;
        this.resourceResolver = resourceResolver;
        this.importer = importer;
    }

    /**
     * Ensure that the order of child nodes matches the order on the remote instance.
     * <p>
     * The method makes an HTTP call to the remote instance to fetch the ordered list of child nodes
     * and re-sorts the given node to match it.
     *
     * @param node the node to sort
     * @return children after sort
     */
    public List<String> sort(Node node) throws RepositoryException, IOException, URISyntaxException {
        List<String> children = remoteInstance.listChildren(node.getPath());
        sort(node, children);
        return children;
    }

    /**
     * Sort child nodes of a JCR node
     *
     * @param node     the node to sort
     * @param children the desired order of children
     */
    public void sort(Node node, List<String> children) throws RepositoryException {
        if (!node.getPrimaryNodeType().hasOrderableChildNodes()) {
            // node does not support orderable child nodes
            return;
        }

        Node prev = null;
        for (int i = 0; i < children.size(); i++) {
            String childName = children.get(children.size() - 1 - i);
            if (!node.hasNode(childName)) {
                continue;
            }
            Node n = node.getNode(childName);
            if (prev != null) {
                node.orderBefore(n.getName(), prev.getName());
            }
            prev = n;
        }
    }

    /**
     * Copy binary data from remote instance and update local resource.
     * Performs an HTT call for each property path
     *
     * @param propertyPaths list of binary properties to update, e.g.
     *                      <pre>
     *                      [
     *                         "/content/contentsync/jcr:content/image/file/jcr:content/jcr:data",
     *                         "/content/contentsync/jcr:content/image/file/jcr:content/dam:thumbnails/dam:thumbnail_48.png/jcr:content/jcr:data",
     *                      ]
     *                      </pre>
     */
    public void copyBinaries(List<String> propertyPaths) throws IOException, RepositoryException, URISyntaxException {
        Session session = resourceResolver.adaptTo(Session.class);
        for (String propertyPath : propertyPaths) {
            try (InputStream ntData = remoteInstance.getStream(propertyPath)) {
                Binary binary = session.getValueFactory().createBinary(ntData);
                Property p = session.getProperty(propertyPath);
                if (p.getType() == PropertyType.BINARY) {
                    p.setValue(binary);
                } else {
                    Node propertyNode = p.getParent();
                    String propertyName = p.getName();
                    p.remove();
                    propertyNode.setProperty(propertyName, binary);
                }
            }
        }
    }

    public ImportOptions getImportOptions() {
        return new ImportOptions() {

            @Override
            public boolean isCheckin() {
                return false;
            }

            @Override
            public boolean isAutoCheckout() {
                return true;
            }

            @Override
            public boolean isIgnoredImportProvider(String extension) {
                return false;
            }

            @Override
            public boolean isOverwrite() {
                // preserve the node, uuid and version history
                return false;
            }

            @Override
            public boolean isPropertyOverwrite() {
                return true;
            }
        };
    }

    /**
     * Clear jcr:content and remove all properties except protected ones
     *
     * @param node  the node to clear
     */
    public void clearContent(Node node) throws RepositoryException {
        if (!node.hasNode(JCR_CONTENT)) {
            return;
        }
        Node jcrContent = node.getNode(JCR_CONTENT);
        if (!jcrContent.isCheckedOut()) {
            VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
            versionManager.checkout(jcrContent.getPath());
        }
        // remove children of jcr:content
        for (NodeIterator iterator = jcrContent.getNodes(); iterator.hasNext(); ) {
            Node n = iterator.nextNode();
            n.remove();
        }
        // remove any non-protected properties
        for (PropertyIterator iterator = jcrContent.getProperties(); iterator.hasNext(); ) {
            Property p = iterator.nextProperty();
            if (!p.getDefinition().isProtected()) {
                p.remove();
            }
        }
    }

    /**
     * Ensure parent node exists before importing content.
     *
     * Parent can be null, for example, if a user is sync-ing /content/my-site/en/one
     * and the /content/my-site tree does not exist on the local instance.
     *
     * In such a case the method would fetch the primary type of the parent node (/content/my-site/en)
     * and use it as intermediate node type to ensure parent.
     *
     * @param path  the path to ensure if the parent exists
     * @return  the parent node
     */
    public Node ensureParent(String path) throws RepositoryException, IOException, URISyntaxException {
        String parentPath = ResourceUtil.getParent(path);
        Session session = resourceResolver.adaptTo(Session.class);
        Node parentNode;
        if (!session.nodeExists(parentPath)) {
            String parentNodeType = remoteInstance.getPrimaryType(parentPath);
            parentNode = JcrUtils.getOrCreateByPath(parentPath, parentNodeType, parentNodeType, session, false);
        } else {
            parentNode = session.getNode(parentPath);
        }
        return parentNode;
    }

    /**
     *
     * importContent("/content/contentsync/page", "jcr:content.json", .... )
     * where /content/contentsync/page is an existing cq:Page resource
     *
     * importContent("/content/dam/contentsync/asset", "jcr:content.json", .... )
     * where /content/contentsync/asset is an existing dam:Asset resource
     *
     * importContent("/content/dam/contentsync", "folderName.json", .... )
     * importContent("/content/misc", "nodeName.json", .... )
     *
     * @param catalogItem
     * @return
     */
    public Node ensureContentNode(CatalogItem catalogItem) throws RepositoryException, IOException, URISyntaxException {
        String path = catalogItem.getPath();

        Node parentNode = ensureParent(path);

        Node contentNode;
        if (catalogItem.hasContentResource()) {
            String nodeName = ResourceUtil.getName(path);

            String primaryType = catalogItem.getString(JCR_PRIMARYTYPE);
            if(parentNode.hasNode(nodeName)){
                contentNode = parentNode.getNode(nodeName);
            } else {
                contentNode = parentNode.addNode(nodeName, primaryType);
                String mixins[] = catalogItem.getMixins();
                for (String mx : mixins) {
                    contentNode.addMixin(mx);
                }
            }
        } else {
            contentNode = parentNode;
        }
        return contentNode;
    }

    public void importData(CatalogItem catalogItem, JsonObject jsonObject) throws RepositoryException, IOException, URISyntaxException {
        String path = catalogItem.getPath();
        log.debug("importing {}", path);

        Node contentNode = ensureContentNode(catalogItem);
        clearContent(contentNode);

        ImportOptions importOptions = getImportOptions();
        String nodeName;
        if (catalogItem.hasContentResource()) {
            nodeName = JCR_CONTENT;
        } else {
            nodeName = ResourceUtil.getName(path);
        }

        StringWriter sw = new StringWriter();
        try(JsonWriter writer = Json.createWriter(sw)){
            writer.write(jsonObject);
        }
        InputStream contentStream = new ByteArrayInputStream(sw.toString().getBytes(StandardCharsets.UTF_8));
        importer.importContent(contentNode, nodeName + ".json", contentStream, importOptions, null);
    }

    @SuppressWarnings("squid:S112")
    public String createVersion(Resource resource) throws Exception {
        String revisionId = null;
        if (resource.isResourceType("cq:Page")) {
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            Page pg = resource.adaptTo(Page.class);
            if (pg != null) {
                revisionId = pageManager.createRevision(pg, null, "created by contentsync").getId();
            }
        } else if (resource.isResourceType("dam:Asset")) {
            AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
            Asset asset = resource.adaptTo(Asset.class);
            revisionId = assetManager.createRevision(asset, null, "created by contentsync").getId();
        }
        return revisionId;
    }

    public void runWorkflows(String workflowModel, List<String> paths) throws WorkflowException {
        WorkflowSession workflowSession = resourceResolver.adaptTo(WorkflowSession.class);
        WorkflowModel model = workflowSession.getModel(workflowModel);
        for (String path : paths) {
            WorkflowData data = workflowSession.newWorkflowData("JCR_PATH", path);
            workflowSession.startWorkflow(model, data);
        }
    }
}
