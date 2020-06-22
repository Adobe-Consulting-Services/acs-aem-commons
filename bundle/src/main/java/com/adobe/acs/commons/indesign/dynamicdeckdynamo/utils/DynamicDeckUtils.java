/*
 * ACS AEM Commons
 *
 * Copyright (C) 2020 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants.DynamicDeckDynamoConstants;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants.DynamicDeckDynamoIDSConstants;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.adobe.dam.print.ids.PrintFormat;
import com.adobe.granite.workflow.PayloadMap;
import com.adobe.granite.workflow.exec.WorkItem;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.collection.SmartCollection;
import com.day.cq.search.Query;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class DynamicDeckUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDeckUtils.class);
    private static final String IDSP_SCRIPT_ARG = "<IDSP:scriptArgs>\n"
            + "                            <IDSP:name>%s</IDSP:name>\n"
            + "                            <IDSP:value><![CDATA[%s]]></IDSP:value>\n"
            + "                        </IDSP:scriptArgs>";

    /**
     * Private Constructor will prevent the instantiation of this class directly
     */
    private DynamicDeckUtils() {

    }

    public static String createIDSPScriptArg(String name, Object value) {
        return String.format(IDSP_SCRIPT_ARG, name, value);
    }


    public static StringBuilder getImagePaths(List<String> assetPathList, String placeholderImagePath) {
        StringBuilder imagePaths = new StringBuilder(StringUtils.EMPTY);
        getCollectionPaths(assetPathList, imagePaths);
        imagePaths.append(placeholderImagePath);
        return imagePaths;
    }

    private static void getCollectionPaths(List<String> assetPathList, StringBuilder imagePaths) {
        if (assetPathList.isEmpty()) {
            LOGGER.debug("Asset resource list is empty");
            return;
        }
        for (String path : assetPathList) {
            imagePaths.append(path).append(",");
        }
    }

    public static StringBuilder addExportFormat(Asset master, List<PrintFormat> formats) {
        StringBuilder exportFormats = getExportFormats(formats);
        for (String format : exportFormats.toString().split(",")) {
            if (PrintFormat.INDD.getFormat().equals(format)) {
                addExportJobProperty(master);
            }
        }
        return exportFormats;
    }

    private static StringBuilder getExportFormats(List<PrintFormat> formats) {
        StringBuilder exportFormats = new StringBuilder(StringUtils.EMPTY);
        if (formats != null && !formats.isEmpty()) {
            for (PrintFormat format : formats) {
                exportFormats.append(format.getFormat()).append(",");
            }
        }
        return exportFormats;
    }

    private static void addExportJobProperty(Asset master) {
        if (master == null) {
            LOGGER.debug("Master asset is null");
            return;
        }
        Resource assetResource = master.adaptTo(Resource.class);
        if (assetResource != null && !ResourceUtil.isNonExistingResource(assetResource)) {
            Resource content = assetResource.getChild(JcrConstants.JCR_CONTENT);
            if (content != null && !ResourceUtil.isNonExistingResource(content)
                    && content.adaptTo(ModifiableValueMap.class) != null) {
                ModifiableValueMap contentProperties = content.adaptTo(ModifiableValueMap.class);
                if (contentProperties != null) {
                    contentProperties.put(DynamicDeckDynamoIDSConstants.IDS_EXPORTED, true);
                    contentProperties.put(DamConstants.DAM_ASSET_STATE, DamConstants.DAM_ASSET_STATE_PROCESSING);
                }
            }
        }
    }

    public static Map<String, String> getCollectionsListForLoggedInUser(String collectionQuery, ResourceResolver resourceResolver) {

        if (StringUtils.isEmpty(collectionQuery)) {
            LOGGER.error("Collection query string to fetch all collections for logged-in user is null/empty");
            return Collections.emptyMap();
        }
        Map<String, String> collectionMap = new HashMap<>();
        Iterator<Resource> resultItr = resourceResolver.findResources(collectionQuery, javax.jcr.query.Query.JCR_SQL2);
        while (resultItr.hasNext()) {

            Resource resultCollectionResource = resultItr.next();
            LOGGER.debug("CollectionList Query Result Resource:{}", resultCollectionResource);

            if (DynamicDeckDynamoConstants.PN_LIGHTBOX_COLLECTION.equalsIgnoreCase(resultCollectionResource.getName())) {
                continue;
            }

            collectionMap.put(resultCollectionResource.getValueMap().get(JcrConstants.JCR_TITLE, String.class), resultCollectionResource.getPath());

        }
        return collectionMap;
    }

    /**
     * This method will return asset Input stream for indd file
     *
     * @param assetResource
     * @return
     * @throws DynamicDeckDynamoException
     */
    public static InputStream getInddXmlRenditionInputStream(Resource assetResource) throws DynamicDeckDynamoException {

        String renditionName = StringUtils.replace(assetResource.getName(), ".indd", ".xml");
        Rendition xmlRenditionAsset = assetResource.adaptTo(Asset.class).getRendition(renditionName);

        if (null == xmlRenditionAsset) {
            throw new DynamicDeckDynamoException("Asset xml rendition doesn't exists");
        }
        return getInputStreamByResource(xmlRenditionAsset.adaptTo(Resource.class));
    }

    public static InputStream getAssetOriginalInputStream(Resource annotatedXmlResource) throws DynamicDeckDynamoException {
        Rendition annotatedXmlRendition = annotatedXmlResource.adaptTo(Asset.class).getRendition("original");
        return getInputStreamByResource(annotatedXmlRendition.adaptTo(Resource.class));
    }

    private static InputStream getInputStreamByResource(Resource assetResource) throws DynamicDeckDynamoException {
        if (null == assetResource) {
            throw new DynamicDeckDynamoException("Annotated XML resource is null");
        }
        Node node = assetResource.getChild(JcrConstants.JCR_CONTENT).adaptTo(Node.class);

        if (null == node) {
            throw new DynamicDeckDynamoException("Asset resource's data node is null");
        }
        try {

            return node.getProperty("jcr:data").getBinary().getStream();
        } catch (RepositoryException e) {
            throw new DynamicDeckDynamoException("Repository exception occurred while fetching the xml input stream", e);
        }
    }

    /**
     * This method commit resourceResolver object after session refresh
     *
     * @param resourceResolver
     */
    public static void commit(ResourceResolver resourceResolver) {
        if (resourceResolver == null || !resourceResolver.isLive()) {
            LOGGER.error("Resource resolver is null or not live while committing resourceResolver");
            return;
        }

        try {
            resourceResolver.refresh();
            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
            }
        } catch (PersistenceException e) {
            LOGGER.error("Error occurred while committing resource resolver", e);
        }

    }


    /**
     * Fetches all Resources which are there in a smart collection
     *
     * @param smartCollectionResource
     * @return
     * @throws DynamicDeckDynamoException
     */
    public static List<Resource> fetchSmartCollectionResourceList(Resource smartCollectionResource) throws DynamicDeckDynamoException {

        List<Resource> arrayList = new ArrayList<>();

        SmartCollection smartResourceCollection = smartCollectionResource.adaptTo(SmartCollection.class);

        if (null == smartResourceCollection) {
            LOGGER.debug("Smart collection not found for {}", smartCollectionResource.getPath());
            return Collections.emptyList();
        }

        Query query = null;
        try {
            query = smartResourceCollection.getQuery();
        } catch (IOException | RepositoryException e) {
            throw new DynamicDeckDynamoException("Error while getting the collection query from smart collection resource", e);
        }
        query.setHitsPerPage(0);
        SearchResult result = query.getResult();
        Iterator<Resource> itr = result.getResources();
        while (itr.hasNext()) {
            arrayList.add(itr.next());
        }
        return arrayList;
    }

    public static Asset createUniqueAsset(Resource parent, String name, ResourceResolver resolver) {
        AssetManager graniteAssetMgr = resolver.adaptTo(AssetManager.class);
        if (graniteAssetMgr != null) {
            return graniteAssetMgr.createAsset(parent.getPath() + "/" + name, null, DynamicDeckDynamoConstants.INDESIGN_MIME_TYPE, true);
        }
        return null;

    }

    /**
     * @param resolver
     * @param folderName
     * @param folderParentResource
     * @return
     * @throws PersistenceException
     */
    public static String getOrCreateFolder(ResourceResolver resolver, String folderName,
                                           Resource folderParentResource) throws PersistenceException {
        Resource folderResource = folderParentResource.getChild(folderName);
        if (folderResource == null) {
            Resource createdFolderResource = createFolder(resolver, folderName, folderParentResource);
            commit(resolver);
            return createdFolderResource.getPath();

        } else {
            return folderResource.getPath();
        }
    }

    /**
     * @param resolver
     * @param folderName
     * @param resource
     * @return
     * @throws PersistenceException
     */
    public static Resource createFolder(ResourceResolver resolver, String folderName,
                                        Resource resource) throws PersistenceException {
        Map<String, Object> folderResourceProp = new HashMap<>();
        folderResourceProp.put(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
        return resolver.create(resource, folderName, folderResourceProp);
    }

    /**
     * This method is used to find files under specific folder by file extension.
     *
     * @param templateFolderResource
     * @param extension
     * @return
     */
    public static Resource findFileUnderFolderByExtension(Resource templateFolderResource, String extension) {

        Stream<Resource> resourceStream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(templateFolderResource.getChildren().iterator(), Spliterator.ORDERED),
                false);

        Optional<Resource> targetResource = resourceStream.filter(item -> StringUtils.endsWithIgnoreCase(item.getPath(), extension)).findFirst();

        return targetResource.orElse(null);

    }

    /**
     * Get asset resource path from the payload.
     *
     * @param workItem
     * @param resourceResolver
     * @return
     */
    public static Resource getAssetResourceFromPayload(WorkItem workItem,
                                                       ResourceResolver resourceResolver) {
        Resource resource = null;
        if (workItem.getWorkflowData().getPayloadType().equals(PayloadMap.TYPE_JCR_PATH)) {
            String path = workItem.getWorkflowData().getPayload().toString();
            if (path.contains(NameConstants.NN_CONTENT)) {
                path = StringUtils.substringBefore(path, FileSystem.SEPARATOR + NameConstants.NN_CONTENT);
            }
            resource = resourceResolver.getResource(path);
        }
        return resource;
    }


    public static void updateUserData(Session jcrSession) {

        if (jcrSession != null) {
            try {
                jcrSession.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
            } catch (RepositoryException e) {
                LOGGER.error("Error in repository operation::", e);
            }
        } else {
            LOGGER.error("JCR session object is null.");
        }

    }
}
