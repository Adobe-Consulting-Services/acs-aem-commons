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

package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.impl;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants.DynamicDeckDynamoConstants;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants.DynamicDeckDynamoIDSConstants;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils.XMLResourceIterator;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.DynamicDeckConfigurationService;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.DynamicDeckService;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.XMLGeneratorService;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils.DynamicDeckUtils;
import com.adobe.dam.print.ids.PrintFormat;
import com.day.cq.commons.RangeIterator;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.tagging.TagManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component(service = DynamicDeckService.class)
public class DynamicDeckServiceImpl implements DynamicDeckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDeckServiceImpl.class);
    private static final PrintFormat[] formats = {PrintFormat.JPG, PrintFormat.INDD, PrintFormat.PDF};
    private static final List<PrintFormat> allFormats = Arrays.asList(formats);

    @Reference
    private DynamicDeckConfigurationService configurationService;
    @Reference
    private XMLGeneratorService xmlGeneratorService;
    @Reference
    private JobManager jobManager;

    @Override
    public String createDeck(String deckName, Resource masterAssetResource, List<Resource> assetResourceList, Resource templateFolderResource,
                             Resource destinationFolderResource, ResourceResolver resourceResolver)
            throws DynamicDeckDynamoException {

        Resource inddTemplateResource = DynamicDeckUtils.findFileUnderFolderByExtension(templateFolderResource, "indd");
        Resource annotatedXmlResource = DynamicDeckUtils.findFileUnderFolderByExtension(templateFolderResource, "xml");

        if (null == inddTemplateResource || null == annotatedXmlResource) {
            throw new DynamicDeckDynamoException("Supplied INDD template folder path doesn't contain InDesign Template file OR template XML file : " + templateFolderResource.getPath());
        }

        final StringBuilder idspScriptArgs = new StringBuilder();
        List<String> inddImageList = new ArrayList<>();

        Asset damAsset = DynamicDeckUtils.createUniqueAsset(destinationFolderResource, JcrUtil.createValidName(deckName) + ".indd", resourceResolver);
        if (damAsset == null) {
            throw new DynamicDeckDynamoException("Dynamic Deck document not created at the destination : " + destinationFolderResource.getPath());
        }

        List<XMLResourceIterator> assetItrList = new ArrayList<>();
        assetItrList.add(new XMLResourceIterator(DynamicDeckDynamoConstants.XML_SECTION_TYPE_GENERIC, assetResourceList.listIterator()));

        String processedXmlPath;

        try (InputStream templateXmlInputStream = DynamicDeckUtils.getAssetOriginalInputStream(annotatedXmlResource)) {
            processedXmlPath = xmlGeneratorService.generateInddXML(templateXmlInputStream, assetItrList,
                    masterAssetResource, damAsset.adaptTo(Resource.class), resourceResolver, inddImageList);
        } catch (IOException e) {
            throw new DynamicDeckDynamoException(" Error while processing InDesign Template XML, hence exiting the deck generation process");
        }

        if (StringUtils.isBlank(processedXmlPath)) {
            throw new DynamicDeckDynamoException("Processed XML is null/empty, hence exiting the deck generation process");
        }

        StringBuilder exportFormats = DynamicDeckUtils.addExportFormat(damAsset, allFormats);
        StringBuilder imagePaths = DynamicDeckUtils.getImagePaths(inddImageList, configurationService.getPlaceholderImagePath());

        addIdsScriptArgs(inddTemplateResource.getPath(), damAsset.adaptTo(Resource.class), idspScriptArgs, imagePaths, processedXmlPath, exportFormats);
        DynamicDeckUtils.commit(resourceResolver);


        return damAsset.getPath();
    }

    /**
     * This method fetch asset list from collection/smart-collection.
     *
     * @param collectionResource
     * @param resourceResolver
     * @return
     */
    @Override
    public List<Resource> fetchAssetListFromCollection(Resource collectionResource, ResourceResolver resourceResolver) throws DynamicDeckDynamoException {
        List<Resource> arrayList = new ArrayList<>();

        if (DamUtil.isSmartCollection(collectionResource)) {
            return DynamicDeckUtils.fetchSmartCollectionResourceList(collectionResource);
        }

        Iterator<Asset> assets = DamUtil.getAssets(collectionResource);

        assets.forEachRemaining(eachAsset -> {
            Resource eachAssetResource = eachAsset.adaptTo(Resource.class);
            if (null != eachAssetResource) {
                arrayList.add(eachAsset.adaptTo(Resource.class));
            }
        });
        return arrayList;
    }

    @Override
    public List<Resource> fetchAssetListFromQuery(String queryString, ResourceResolver resourceResolver) {
        Iterator<Resource> assetResourceIterator = resourceResolver.findResources(queryString, Query.JCR_SQL2);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(assetResourceIterator, Spliterator.ORDERED),
                false).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<Resource> fetchAssetListFromTags(String tagsString, ResourceResolver resourceResolver) {
        TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
        List<Resource> arrayList = new ArrayList<>();
        String[] tagsArray = StringUtils.split(tagsString, ",");
        Arrays.stream(tagsArray).forEach(tag -> {
            RangeIterator<Resource> tags = tagManager.find(tag);
            tags.forEachRemaining(taggedResource -> {
                if (taggedResource.getPath().startsWith("/content/dam/") && taggedResource.getPath().endsWith("/jcr:content/metadata")) {
                    Asset taggedAsset = taggedResource.getParent().getParent().adaptTo(Asset.class);
                    if (null != taggedAsset) {
                        arrayList.add(taggedAsset.adaptTo(Resource.class));
                    }
                }
            });
        });
        return arrayList;
    }

    private void addExportJobProperty(Asset assetObject, String exportJobId) {
        if (assetObject == null) {
            LOGGER.error("Asset is null, hence export properties cannot be added.");
            return;
        }
        if (StringUtils.isBlank(exportJobId)) {
            LOGGER.error("ExportJobId is null/empty, hence export properties cannot be added.");
            return;
        }
        Resource assetResource = assetObject.adaptTo(Resource.class);
        if (assetResource == null || ResourceUtil.isNonExistingResource(assetResource)) {
            LOGGER.error("Asset resource is null or doesn't exist, hence export properties cannot be added. {}", assetObject.getPath());
            return;
        }
        Resource content = assetResource.getChild(JcrConstants.JCR_CONTENT);
        if (content == null || ResourceUtil.isNonExistingResource(content)
                && content.adaptTo(ModifiableValueMap.class) == null) {
            LOGGER.error("Asset JCR content is null or doesn't exist, hence export properties cannot be added. {}", assetResource.getPath());
            return;
        }
        ModifiableValueMap contentProperties = content.adaptTo(ModifiableValueMap.class);
        if (contentProperties == null) {
            LOGGER.error("Property object is null, hence export properties cannot be added {}", assetResource.getPath());
            return;
        }
        contentProperties.put("exportJobId", exportJobId);
        contentProperties.put(DamConstants.DAM_ASSET_STATE, DamConstants.DAM_ASSET_STATE_PROCESSING);
        contentProperties.put(DynamicDeckDynamoConstants.PN_INDD_TEMPLATE_TYPE, DynamicDeckDynamoConstants.DECK_TYPE);

    }


    private Job addJob(Asset master, Map<String, Object> props) throws DynamicDeckDynamoException {
        Job offloadingJob = jobManager.addJob(DynamicDeckDynamoIDSConstants.IDS_EXTENDSCRIPT_JOB, props);
        if (offloadingJob == null) {
            throw new DynamicDeckDynamoException("Job manager is not able to create job");
        }
        addExportJobProperty(master, offloadingJob.getId());
        return offloadingJob;
    }

    private void addIdsScriptArgs(String templatePath, Resource masterAssetResource,
                                  StringBuilder idspScriptArgs, StringBuilder imagePaths, String formattedXMLPath,
                                  StringBuilder exportFormats) throws DynamicDeckDynamoException {
        final Map<String, Object> props = new HashMap<>();
        final String[] scriptPaths = new String[]{
                DynamicDeckDynamoIDSConstants.IDS_SCRIPT_ROOT_PATH + "json2.jsx/" + JcrConstants.JCR_CONTENT,
                DynamicDeckDynamoIDSConstants.IDS_SCRIPT_ROOT_PATH + "cq-lib.jsx/" + JcrConstants.JCR_CONTENT,
                DynamicDeckDynamoIDSConstants.IDS_SCRIPT_ROOT_PATH + "dynamic-deck.jsx/" + JcrConstants.JCR_CONTENT};


        idspScriptArgs.append(DynamicDeckUtils.createIDSPScriptArg(DynamicDeckDynamoIDSConstants.IDS_ASSET_NAME, masterAssetResource.getName()));
        idspScriptArgs.append(DynamicDeckUtils.createIDSPScriptArg(DynamicDeckDynamoIDSConstants.IDS_TEMPLATE_PATH, templatePath));
        idspScriptArgs.append(DynamicDeckUtils.createIDSPScriptArg(DynamicDeckDynamoIDSConstants.IDS_ARGS_TAG_XML, formattedXMLPath));
        idspScriptArgs.append(DynamicDeckUtils.createIDSPScriptArg(DynamicDeckDynamoIDSConstants.IDS_ARGS_IMAGE_LIST, imagePaths.toString()));
        idspScriptArgs.append(DynamicDeckUtils.createIDSPScriptArg(DynamicDeckDynamoIDSConstants.IDS_ARGS_FORMATS, exportFormats.toString()));
        idspScriptArgs.append(DynamicDeckUtils.createIDSPScriptArg(DynamicDeckDynamoIDSConstants.IDS_ARGS_TYPE, DynamicDeckDynamoConstants.DECK_TYPE));

        addIDSProperties(masterAssetResource.getPath(), idspScriptArgs, props, scriptPaths);

        addJob(masterAssetResource.adaptTo(Asset.class), props);
    }

    private void addIDSProperties(String assetPath, StringBuilder idspScriptArgs, Map<String, Object> props, String[] scriptPaths) {
        props.put(DynamicDeckDynamoIDSConstants.IDS_JOB_SCRIPT, scriptPaths);
        props.put(DynamicDeckDynamoIDSConstants.IDS_JOB_PAYLOAD, assetPath);
        props.put(DynamicDeckDynamoIDSConstants.INPUT_PAYLOAD, assetPath);
        props.put(DynamicDeckDynamoIDSConstants.OUTPUT_PAYLOAD, assetPath);
        props.put(DynamicDeckDynamoIDSConstants.IDS_ADD_SOAP_ARGS, idspScriptArgs.toString());
    }
}
