package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;

@ProviderType
public interface DynamicDeckService {


    /**
     * This method will create the InDesign deck based on list of assets and other supporting parameter supplied.
     * <p>
     * TODO Planning to create service account and remove the dependency of resourceResolver from this service
     *
     * @param deckName
     * @param masterAssetResource
     * @param assetResourceList
     * @param templateFolderResource
     * @param resourceResolver
     * @param destinationFolderResource
     * @return
     * @throws IOException
     * @throws RepositoryException
     */
    String createDeck(String deckName, Resource masterAssetResource, List<Resource> assetResourceList,
                      Resource templateFolderResource, Resource destinationFolderResource, ResourceResolver resourceResolver)
            throws DynamicDeckDynamoException;

    /**
     * This method fetch asset list from collection/smart-collection.
     * <p>
     * TODO Planning to create service account and remove the dependency of resourceResolver from this service
     *
     * @param collectionResource
     * @param resourceResolver
     * @return
     */
    List<Resource> fetchAssetListFromCollection(Resource collectionResource, ResourceResolver resourceResolver)
            throws DynamicDeckDynamoException;

    List<Resource> fetchAssetListFromQuery(String queryString, ResourceResolver resourceResolver);

    List<Resource> fetchAssetListFromTags(String tagsString, ResourceResolver resourceResolver);
}
