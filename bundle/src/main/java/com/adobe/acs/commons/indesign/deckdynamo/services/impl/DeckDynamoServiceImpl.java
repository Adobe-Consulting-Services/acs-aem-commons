package com.adobe.acs.commons.indesign.deckdynamo.services.impl;

import com.adobe.acs.commons.indesign.deckdynamo.constants.DeckDynamoConstants;
import com.adobe.acs.commons.indesign.deckdynamo.constants.DeckDynamoIDSConstants;
import com.adobe.acs.commons.indesign.deckdynamo.exception.DeckDynamoException;
import com.adobe.acs.commons.indesign.deckdynamo.osgiconfigurations.DeckDynamoConfigurationService;
import com.adobe.acs.commons.indesign.deckdynamo.pojos.XMLResourceIterator;
import com.adobe.acs.commons.indesign.deckdynamo.services.DeckDynamoService;
import com.adobe.acs.commons.indesign.deckdynamo.services.XMLGeneratorService;
import com.adobe.acs.commons.indesign.deckdynamo.utils.DeckDynamoUtils;
import com.adobe.dam.print.ids.PrintFormat;
import com.adobe.dam.print.ids.StringConstants;
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
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component(immediate = true, service = DeckDynamoService.class)
public class DeckDynamoServiceImpl implements DeckDynamoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeckDynamoServiceImpl.class);
    private static final String DEFAULT_PAGES_REGEX = "^page[0-9]*.jpg$";
    private static final PrintFormat[] formats = {PrintFormat.JPG, PrintFormat.INDD, PrintFormat.PDF};
    private static final List<PrintFormat> allFormats = Arrays.asList(formats);

    @Reference
    private DeckDynamoConfigurationService configurationService;
    @Reference
    private XMLGeneratorService xmlGeneratorService;
    @Reference
    private JobManager jobManager;

    @Override
    public String createDeck(String deckName, String masterAssetPath, List<Resource> assetResourceList, String templateFolderPath,
                             String destinationFolderPath, ResourceResolver resourceResolver)
            throws DeckDynamoException {

        String inddTemplatePath = DeckDynamoUtils.findFileUnderFolderByExtension(templateFolderPath, DeckDynamoConstants.INDD_EXTENSION, resourceResolver);
        String annotatedXmlPath = DeckDynamoUtils.findFileUnderFolderByExtension(templateFolderPath, DeckDynamoConstants.XML_EXTENSION, resourceResolver);

        if (StringUtils.isEmpty(inddTemplatePath) || StringUtils.isEmpty(annotatedXmlPath)) {
            throw new DeckDynamoException("Supplied INDD template folder path doesn't contain InDesign Template file OR template XML file : " + templateFolderPath);
        }

        StringBuilder idspScriptArgs = new StringBuilder();
        List<String> inddImageList = new ArrayList<>();

        Resource destinationFolderResource = resourceResolver.getResource(destinationFolderPath);
        if (null == destinationFolderResource) {
            throw new DeckDynamoException("Destination folder resource is null, hence exiting the deck generation process.");
        }


        Asset damAsset = DeckDynamoUtils.createUniqueAsset(destinationFolderResource, JcrUtil.createValidName(deckName) + DeckDynamoConstants.DOT + DeckDynamoConstants.INDD_EXTENSION, resourceResolver);
        if (damAsset == null) {
            throw new DeckDynamoException("Dynamic Deck document not created at the destination : " + destinationFolderResource.getPath());
        }

        InputStream templateXmlInputStream = DeckDynamoUtils.getAssetInputStreamByPath(resourceResolver, annotatedXmlPath);
        if (null == templateXmlInputStream) {
            throw new DeckDynamoException("InDesign Template XML is null, hence exiting the deck generation process");
        }

        List<XMLResourceIterator> assetItrList = new ArrayList<>();
        assetItrList.add(new XMLResourceIterator(DeckDynamoConstants.XML_SECTION_TYPE_GENERIC, assetResourceList.listIterator()));

        String processedXmlPath = xmlGeneratorService.generateInddXML(templateXmlInputStream, assetItrList,
                resourceResolver.getResource(masterAssetPath), damAsset.adaptTo(Resource.class), resourceResolver, inddImageList);

        StringBuilder exportFormats = DeckDynamoUtils.addExportFormat(damAsset, allFormats);
        StringBuilder imagePaths = DeckDynamoUtils.getImagePaths(inddImageList, configurationService.getPlaceholderImagePath());

        addIdsScriptArgs(inddTemplatePath, damAsset.adaptTo(Resource.class), idspScriptArgs, imagePaths, processedXmlPath, exportFormats);
        DeckDynamoUtils.commit(resourceResolver);

        return configurationService.getTemplateRootPath();
    }

    /**
     * This method fetch asset list from collection/smart-collection.
     * <p>
     * TODO Planning to create service account and remove the dependency of resourceResolver from this service
     *
     * @param collectionPath
     * @param resourceResolver
     * @return
     */
    @Override
    public List<Resource> fetchAssetListFromCollection(String collectionPath, ResourceResolver resourceResolver) throws DeckDynamoException {
        List<Resource> arrayList = new ArrayList<>();
        if (StringUtils.isBlank(collectionPath)) {
            throw new DeckDynamoException("Resource Path is Null/Empty ");
        }
        Resource collectionResource = resourceResolver.getResource(collectionPath);
        if (collectionResource == null) {
            throw new DeckDynamoException("No Resource exists at Supplied Path :" + collectionPath);
        }

        if (DamUtil.isSmartCollection(collectionResource)) {
            return DeckDynamoUtils.fetchSmartCollectionResourceList(collectionResource);
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
        String[] tagsArray = StringUtils.split(tagsString, DeckDynamoConstants.COMMA);
        Arrays.stream(tagsArray).forEach(tag -> {
            RangeIterator<Resource> tags = tagManager.find(tag);
            tags.forEachRemaining(taggedResource -> {
                if (taggedResource.getPath().startsWith(DeckDynamoConstants.DAM_ROOT) && taggedResource.getPath().endsWith(DeckDynamoConstants.SLASH + DeckDynamoConstants.DAM_METADATA)) {
                    Asset taggedAsset = taggedResource.getParent().getParent().adaptTo(Asset.class);
                    if (null != taggedAsset) {
                        arrayList.add(taggedAsset.adaptTo(Resource.class));
                    }
                }
            });
        });
        return arrayList;
    }

    private void addExportJobProperty(Asset assetObject, Object exportJobId) {
        if (assetObject == null) {
            LOGGER.error("Asset is null, hence export properties cannot be added.");
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
        contentProperties.put(DeckDynamoConstants.PN_INDD_TEMPLATE_TYPE, DeckDynamoConstants.DECK_TYPE);

    }


    private Job addJob(Asset master, Map<String, Object> props, JobManager jobManager) throws DeckDynamoException {
        Job offloadingJob = jobManager.addJob(DeckDynamoIDSConstants.IDS_EXTENDSCRIPT_JOB, props);
        if (offloadingJob == null) {
            throw new DeckDynamoException("Job manager is not able to create job");
        }
        addExportJobProperty(master, offloadingJob.getId());
        return offloadingJob;
    }

    private void addIdsScriptArgs(String templatePath, Resource masterAssetResource,
                                  StringBuilder idspScriptArgs, StringBuilder imagePaths, String formattedXMLPath,
                                  StringBuilder exportFormats) throws DeckDynamoException {
        Map<String, Object> props = new HashMap<>();
        String[] scriptPaths = new String[]{
                DeckDynamoIDSConstants.IDS_SCRIPT_ROOT_PATH + "json2.jsx/" + JcrConstants.JCR_CONTENT,
                DeckDynamoIDSConstants.IDS_SCRIPT_ROOT_PATH + "cq-lib.jsx/" + JcrConstants.JCR_CONTENT,
                DeckDynamoIDSConstants.IDS_SCRIPT_ROOT_PATH + "dynamic-deck.jsx/" + JcrConstants.JCR_CONTENT};


        idspScriptArgs.append(DeckDynamoUtils.createIDSPScriptArg(DeckDynamoIDSConstants.IDS_ASSET_NAME, masterAssetResource.getName()));
        idspScriptArgs.append(DeckDynamoUtils.createIDSPScriptArg(DeckDynamoIDSConstants.IDS_TEMPLATE_PATH, templatePath));
        idspScriptArgs.append(DeckDynamoUtils.createIDSPScriptArg(DeckDynamoIDSConstants.IDS_ARGS_TAG_XML, formattedXMLPath));
        idspScriptArgs.append(DeckDynamoUtils.createIDSPScriptArg(DeckDynamoIDSConstants.IDS_ARGS_IMAGE_LIST, imagePaths.toString()));
        idspScriptArgs.append(DeckDynamoUtils.createIDSPScriptArg(DeckDynamoIDSConstants.IDS_ARGS_FORMATS, exportFormats.toString()));
        idspScriptArgs.append(DeckDynamoUtils.createIDSPScriptArg(DeckDynamoIDSConstants.IDS_ARGS_TYPE, DeckDynamoConstants.DECK_TYPE));

        addIDSProperties(masterAssetResource.getPath(), idspScriptArgs, props, scriptPaths);

        //TODO: Review and remove below code, it was from product codebase
        String mergedType = masterAssetResource.adaptTo(Asset.class).getMetadataValue(StringConstants.MERGED_TYPE);
        if (mergedType != null && mergedType.equals(StringConstants.MERGED_TYPE_TEMPLATE)) {
            props.put(DeckDynamoIDSConstants.IDS_JOB_DECOUPLED, true);
            props.put(DeckDynamoIDSConstants.IDS_JOB_PAGES_REGEX, DEFAULT_PAGES_REGEX);
        }

        addJob(masterAssetResource.adaptTo(Asset.class), props, jobManager);
    }

    private void addIDSProperties(String assetPath, StringBuilder idspScriptArgs, Map<String, Object> props, String[] scriptPaths) {
        props.put(DeckDynamoIDSConstants.IDS_JOB_SCRIPT, scriptPaths);
        props.put(DeckDynamoIDSConstants.IDS_JOB_PAYLOAD, assetPath);
        props.put(DeckDynamoIDSConstants.INPUT_PAYLOAD, assetPath);
        props.put(DeckDynamoIDSConstants.OUTPUT_PAYLOAD, assetPath);
        props.put(DeckDynamoIDSConstants.IDS_ADD_SOAP_ARGS, idspScriptArgs.toString());
    }
}
