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

package com.adobe.acs.commons.indesign.dynamicdeckdynamo.workflow.processes.impl;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants.DynamicDeckDynamoConstants;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils.DynamicDeckUtils;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.jcr.Session;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * This process tracks back the properties which are changed in the generated deck and updates its respective properties in all the assets.
 */
@Component(service = WorkflowProcess.class, property = {"process.label=Dynamic Deck Dynamo Write Back Process"})
public class DynamicDeckBackTrackProcess implements WorkflowProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDeckBackTrackProcess.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        ResourceResolver resourceResolver;
        try {

            resourceResolver = workflowSession.adaptTo(ResourceResolver.class);

            Session jcrSession = workflowSession.adaptTo(Session.class);
            if (null == jcrSession) {
                LOGGER.error("JCR Session is null");
                return;
            }

            DynamicDeckUtils.updateUserData(jcrSession);
            DynamicDeckUtils.updateUserData(resourceResolver.adaptTo(Session.class));

            Resource assetResource = DynamicDeckUtils.getAssetResourceFromPayload(workItem, resourceResolver);

            if (null == assetResource) {
                LOGGER.error("Asset resource from payload is null");
                return;
            }

            if (isFileEligibleToProcess(assetResource)) {
                InputStream xmlInputStream = DynamicDeckUtils.getInddXmlRenditionInputStream(assetResource);
                if (null == xmlInputStream) {
                    LOGGER.debug("File xml input stream is null, hence skipping the parsing process.");
                    return;
                }
                parseXML(xmlInputStream, resourceResolver);
            } else {
                LOGGER.info("File is not eligible to be parsed, hence skipping the parsing process.");
            }

        } catch (DynamicDeckDynamoException e) {
            LOGGER.error("Back track: Error while parsing asset xml", e);
            throw new WorkflowException("Error while performing back track operation", e);
        }


    }

    private void parseXML(InputStream xmlInputStream, ResourceResolver resourceResolver) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); 
            DocumentBuilder dBuilder = dbf.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlInputStream);

            if (doc.hasChildNodes()) {
                final String assetPath = StringUtils.EMPTY;
                readNode(doc.getChildNodes(), assetPath, resourceResolver);
            }

        } catch (ParserConfigurationException | SAXException | IOException | DOMException | TransformerFactoryConfigurationError | DynamicDeckDynamoException e) {
            LOGGER.error("Error while processing the xml template ", e);
        }
    }

    private void readNode(NodeList nodeList, String assetPath, ResourceResolver resourceResolver) throws DOMException, DynamicDeckDynamoException {

        for (int count = 0; count < nodeList.getLength(); count++) {

            Node tempNode = nodeList.item(count);

            // make sure it's element node.
            if (tempNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (tempNode.hasAttributes()) {
                // get attributes names and values
                NamedNodeMap nodeMap = tempNode.getAttributes();
                Node sectionType = nodeMap != null ? nodeMap.getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_SECTION_TYPE) : null;

                if (sectionType != null) {
                    assetPath = getAssetPath(nodeMap, assetPath);

                    if (StringUtils.isNotBlank(assetPath)) {
                        retrieveFieldValues(assetPath, tempNode, resourceResolver);
                    }
                }
            }

            if (tempNode.hasChildNodes()) {
                // loop again if has child nodes
                readNode(tempNode.getChildNodes(), assetPath, resourceResolver);

            }
        }
        DynamicDeckUtils.commit(resourceResolver);
    }

    private void retrieveFieldValues(String assetPath, Node sectionNode, ResourceResolver resourceResolver) throws DynamicDeckDynamoException {
        NodeList childNodes = sectionNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (childNode.hasAttributes()) {
                // get attributes names and values
                NamedNodeMap nodeMap = childNode.getAttributes();
                Node fieldTypeAttr = nodeMap != null ? nodeMap.getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_FIELD_TYPE) : null;
                Node dataSyncAttr = nodeMap != null ? nodeMap.getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_DATA_SYNC) : null;
                Node isArrayAttr = nodeMap != null ? nodeMap.getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_IS_ARRAY) : null;
                Boolean isArray = isArrayAttr != null && "true".equals(isArrayAttr.getNodeValue());

                if (fieldTypeAttr != null && dataSyncAttr != null && "true".equals(dataSyncAttr.getNodeValue())) {

                    Resource assetResource = resourceResolver.getResource(assetPath);
                    ModifiableValueMap mValueMap = null;
                    if (assetResource != null) {
                        mValueMap = assetResource.adaptTo(ModifiableValueMap.class);
                    }
                    if (mValueMap != null) {
                        String fieldType = fieldTypeAttr.getNodeValue();
                        switch (fieldType) {
                            case "image":
                                handleImageType(assetResource, childNode, resourceResolver, isArray);
                                break;
                            case "text":
                                handleTextType(assetResource, childNode, isArray);
                                break;
                            default:
                        }
                    }

                }
            }
        }
    }

    private void handleTextType(Resource assetResource, Node childNode, Boolean isArray) {
        Node propertyPathNode = childNode.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_PROPERTY_PATH);
        if (propertyPathNode != null) {
            String propertyPath = getAssetPropertyPath(propertyPathNode.getNodeValue(), assetResource.getValueMap());
            String textValue = childNode.getTextContent();
            setNewPropertyValue(isArray, propertyPath, assetResource, textValue);
        }
    }

    private void setNewPropertyValue(Boolean isArray, String propertyPath, Resource assetResource, String nodeContentValue) {
        String nodePath = StringUtils.substringBeforeLast(propertyPath, "/");
        ModifiableValueMap properties;
        if (StringUtils.isNotBlank(nodePath)) {
            Resource childResource = assetResource.getChild(nodePath);
            properties = childResource.adaptTo(ModifiableValueMap.class);
            propertyPath = StringUtils.substringAfterLast(propertyPath, "/");
        } else {
            properties = assetResource.adaptTo(ModifiableValueMap.class);
        }
        if (isArray) {
            String index = StringUtils.substringBetween(propertyPath, "[", "]");
            propertyPath = StringUtils.substringBeforeLast(propertyPath, "[");
            int indexVal = Integer.parseInt(index);
            String[] values = properties.get(propertyPath, String[].class);
            if (indexVal >= values.length) {
                values = Arrays.copyOf(values, indexVal + 1);
            }
            values[indexVal] = nodeContentValue;
            properties.put(propertyPath, values);

        } else {
            properties.put(propertyPath, nodeContentValue);
        }
    }

    private void handleImageType(Resource assetResource, Node childNode, ResourceResolver resourceResolver, Boolean isArray) throws DynamicDeckDynamoException {
        Node propertyPathNode = childNode.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_PROPERTY_PATH);
        if (propertyPathNode != null) {
            String propertyPath = getAssetPropertyPath(propertyPathNode.getNodeValue(), assetResource.getValueMap());
            Node hrefNode = childNode.getAttributes().getNamedItem(DavConstants.XML_HREF);
            try {
                if (hrefNode != null) {
                    String hrefValue = hrefNode.getNodeValue();
                    if (StringUtils.contains(hrefValue, "INDD-SERVER-DOCUMENTS/")) {
                        return;
                    }
                    String completeHrefValue = null;
                    if (hrefValue.contains(DamConstants.MOUNTPOINT_ASSETS)) {
                        String hrefEncodedValue = StringUtils.substringAfter(
                                URLDecoder.decode(hrefValue, StandardCharsets.UTF_8.toString()), DamConstants.MOUNTPOINT_ASSETS);
                        completeHrefValue = DamConstants.MOUNTPOINT_ASSETS + hrefEncodedValue;
                    }
                    if (null == completeHrefValue) {
                        LOGGER.error("Back track root path is not correct {}", hrefValue);
                        return;
                    }
                    Resource imageResource = resourceResolver.getResource(completeHrefValue);
                    if (DamUtil.isAsset(imageResource)) {
                        setNewPropertyValue(isArray, propertyPath, assetResource, completeHrefValue);

                    } else {
                        LOGGER.error("ERROR: DATA SYNC : Invalid asset embedded. Asset not found in repository {}", hrefValue);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new DynamicDeckDynamoException("Exception while handling the image type.", e);
            }
        }
    }

    private String getAssetPropertyPath(String nodeValue, ValueMap properties) {
        String propertyPath = nodeValue;
        if (!properties.containsKey(nodeValue)) {
            propertyPath = "jcr:content/metadata/" + propertyPath;
        }
        return propertyPath;
    }

    private String getAssetPath(NamedNodeMap nodeMap, String assetPath) {
        Node assetPathNode = nodeMap.getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_ASSETPATH);
        if (assetPathNode != null) {
            return assetPathNode.getNodeValue();
        }

        return assetPath;
    }

    private boolean isFileEligibleToProcess(Resource assetResource) {

        Resource metadataResource = assetResource.getResourceResolver()
                .getResource(assetResource.getPath() + FileSystem.SEPARATOR + NameConstants.NN_CONTENT
                        + FileSystem.SEPARATOR + DamConstants.METADATA_FOLDER);
        if (null == metadataResource) {
            LOGGER.error("Metadata resource is null, hence returning false");
            return false;
        }
        ValueMap metadataValueMap = metadataResource.getValueMap();

        Resource jcrContentResource = assetResource.getResourceResolver()
                .getResource(assetResource.getPath() + FileSystem.SEPARATOR + NameConstants.NN_CONTENT);
        if (null == jcrContentResource) {
            LOGGER.error("JCR Content resource is null, hence returning false");
            return false;
        }
        ValueMap jcrContentValueMap = jcrContentResource.getValueMap();


        String assetMimeType = metadataValueMap.get(DamConstants.DC_FORMAT, String.class);
        String assetTemplateType = jcrContentValueMap.get(DynamicDeckDynamoConstants.PN_INDD_TEMPLATE_TYPE, String.class);

        String[] eligibleAssetMimeType = {DynamicDeckDynamoConstants.INDESIGN_MIME_TYPE};
        return StringUtils.isNotEmpty(assetMimeType) && ArrayUtils.contains(eligibleAssetMimeType, assetMimeType)
                && StringUtils.isNotBlank(assetTemplateType) && assetTemplateType.equals(DynamicDeckDynamoConstants.DECK_TYPE);
    }

}
