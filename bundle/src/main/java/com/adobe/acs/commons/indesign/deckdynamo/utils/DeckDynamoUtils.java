package com.adobe.acs.commons.indesign.deckdynamo.utils;

import com.adobe.acs.commons.indesign.deckdynamo.constants.DeckDynamoConstants;
import com.adobe.acs.commons.indesign.deckdynamo.constants.DeckDynamoIDSConstants;
import com.adobe.acs.commons.indesign.deckdynamo.exception.DeckDynamoException;
import com.adobe.dam.print.ids.PrintFormat;
import com.adobe.granite.workflow.PayloadMap;
import com.adobe.granite.workflow.exec.WorkItem;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.dam.api.DamConstants;
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

public final class DeckDynamoUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeckDynamoUtils.class);
    private static final String IDSP_SCRIPT_ARG = "<IDSP:scriptArgs>\n"
            + "                            <IDSP:name>%s</IDSP:name>\n"
            + "                            <IDSP:value><![CDATA[%s]]></IDSP:value>\n"
            + "                        </IDSP:scriptArgs>";

    /**
     * Private Constructor will prevent the instantiation of this class directly
     */
    private DeckDynamoUtils() {

    }

    public static String createIDSPScriptArg(String name, Object value) {
        return String.format(IDSP_SCRIPT_ARG, name, value);
    }


    public static StringBuilder getImagePaths(List<String> assetPathList,
                                              String placeholderImagePath) {
        StringBuilder imagePaths = new StringBuilder(StringUtils.EMPTY);
        getCollectionPaths(assetPathList, imagePaths);
        imagePaths.append(placeholderImagePath);
        return imagePaths;
    }

    private static void getCollectionPaths(List<String> assetPathList,
                                           StringBuilder imagePaths) {
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
        for (String format : exportFormats.toString().split(DeckDynamoConstants.COMMA)) {
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
                exportFormats.append(format.getFormat()).append(DeckDynamoConstants.COMMA);
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
                    contentProperties.put(DeckDynamoIDSConstants.IDS_EXPORTED, true);
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

            if (DeckDynamoConstants.PN_LIGHTBOX_COLLECTION.equalsIgnoreCase(resultCollectionResource.getName())) {
                continue;
            }

            collectionMap.put(resultCollectionResource.getValueMap().get(JcrConstants.JCR_TITLE, String.class), resultCollectionResource.getPath());

        }
        return collectionMap;
    }

    /**
     * This method will return asset Input stream for indd file
     *
     * @param resourceResolver
     * @param assetResource
     * @return
     * @throws DeckDynamoException
     */
    public static InputStream getInddXmlRenditionInputStream(ResourceResolver resourceResolver,
                                                             Resource assetResource) throws DeckDynamoException {
        Resource assetJcrContentResource = resourceResolver.getResource(assetResource.getPath() + DeckDynamoConstants.SLASH
                + JcrConstants.JCR_CONTENT + DeckDynamoConstants.SLASH + DamConstants.RENDITIONS_FOLDER + DeckDynamoConstants.SLASH
                + StringUtils.replace(assetResource.getName(), DeckDynamoConstants.DOT + DeckDynamoConstants.INDD_EXTENSION,
                DeckDynamoConstants.DOT + DeckDynamoConstants.XML_EXTENSION)
                + DeckDynamoConstants.SLASH + JcrConstants.JCR_CONTENT);
        return getInputStreamByResource(assetJcrContentResource);
    }

    public static InputStream getAssetInputStreamByPath(ResourceResolver resourceResolver, String annotatedXmlPath) throws DeckDynamoException {
        Resource assetJcrContentResource = resourceResolver
                .getResource(annotatedXmlPath + DeckDynamoConstants.SLASH + JcrConstants.JCR_CONTENT + DeckDynamoConstants.SLASH
                        + DamConstants.RENDITIONS_FOLDER + DeckDynamoConstants.SLASH + DamConstants.ORIGINAL_FILE + DeckDynamoConstants.SLASH + JcrConstants.JCR_CONTENT);
        return getInputStreamByResource(assetJcrContentResource);
    }

    private static InputStream getInputStreamByResource(Resource assetJcrContentResource) throws DeckDynamoException {
        if (null == assetJcrContentResource) {
            throw new DeckDynamoException("Annotated XML resource is null");
        }
        Node node = assetJcrContentResource.adaptTo(Node.class);
        if (null == node) {
            throw new DeckDynamoException("Annotated XML node is null, asset resource path: " + assetJcrContentResource.getPath());
        }
        try {
            return node.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
        } catch (RepositoryException e) {
            throw new DeckDynamoException("Repository exception occurred while fetching the xml input stream", e);
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

        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null || !session.isLive()) {
            LOGGER.error("Resource resolver is null or not live while committing resourceResolver");
            return;
        }
        try {
            resourceResolver.refresh();
            session.refresh(true);
            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
            }
            if (session.hasPendingChanges()) {
                session.save();
            }
        } catch (PersistenceException | RepositoryException e) {
            LOGGER.error("Error occurred while committing resource resolver", e);
        }

    }

    /**
     * This method close resourceResolver object once no longer required
     *
     * @param resourceResolver
     */
    public static void closeResourceResolver(ResourceResolver resourceResolver) {
        Session session = null;
        if (resourceResolver != null && resourceResolver.isLive()) {
            session = resourceResolver.adaptTo(Session.class);
            resourceResolver.close();
        }
        if (null != session && session.isLive()) {
            session.logout();
        }
    }

    /**
     * Fetches all Resources which are there in a smart collection
     *
     * @param smartCollectionResource
     * @return
     * @throws DeckDynamoException
     */
    public static List<Resource> fetchSmartCollectionResourceList(Resource smartCollectionResource) throws DeckDynamoException {

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
            throw new DeckDynamoException("Error while getting the collection query from smart collection resource", e);
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
            return graniteAssetMgr.createAsset(parent.getPath() + DeckDynamoConstants.SLASH + name, null, DeckDynamoConstants.INDESIGN_MIME_TYPE, true);
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
     * @param templateFolderPath
     * @param extension
     * @param resourceResolver
     * @return
     */
    public static String findFileUnderFolderByExtension(String templateFolderPath, String extension, ResourceResolver resourceResolver) {
        Resource templateFolderResource = resourceResolver.getResource(templateFolderPath);
        if (null == templateFolderResource) {
            LOGGER.debug("Supplied template folder path is null/empty, hence exiting the deck generation process");
            return null;
        }

        Stream<Resource> resourceStream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(templateFolderResource.getChildren().iterator(), Spliterator.ORDERED),
                false);

        Optional<Resource> targetResource = resourceStream.filter(item -> StringUtils.endsWithIgnoreCase(item.getPath(), extension)).findFirst();

        return targetResource.map(Resource::getPath).orElse(null);

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

    /**
     * Get the modifiable value map for the given property path in the resource
     *
     * @param resource
     * @param propertyPath
     * @return
     */
    public static ModifiableValueMap getResourceModifiableValueMap(Resource resource, String propertyPath) {
        if (resource == null || StringUtils.isBlank(propertyPath)) {
            LOGGER.debug("Supplied resource is null/empty. Returning an empty map.");
            return null;
        }

        Resource propertiesResource = resource.getChild(propertyPath);
        if (propertiesResource == null) {
            LOGGER.debug("Couldn't get the child resource '{}' for resource at path '{}'. Returning an empty map.",
                    propertyPath, resource.getPath());
            return null;
        }
        return propertiesResource.adaptTo(ModifiableValueMap.class);
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
