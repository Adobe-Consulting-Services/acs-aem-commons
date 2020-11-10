/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
