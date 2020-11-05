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
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils.XMLResourceIterator;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.XMLGeneratorService;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils.DynamicDeckUtils;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.bind.BindConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * This class is used to parse Annotated Template XML and generate Indd XML
 */
@Component(service = XMLGeneratorService.class)
public class XMLGeneratorServiceImpl implements XMLGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLGeneratorServiceImpl.class);

    /**
     * This Method Parses an Annotated Template XML and Creates an INDD XML out of the Template. The INDD XML is sent to
     * INDD Server for generation of decks.
     */
    @Override
    public String generateInddXML(InputStream xmlInputStream,
                                  List<XMLResourceIterator> assetItrList, Resource masterResource, Resource deckResource,
                                  ResourceResolver resourceResolver, List<String> imageList) throws DynamicDeckDynamoException {

        InputStream processedXmlInputStream = null;
        File processedXmlTempFile = null;
        Asset processXmlAsset = null;
        final String xmlName = "processedXml.xml";

        if (null == masterResource) {
            masterResource = deckResource;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder dBuilder = dbf.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlInputStream);

            /*
            Processing the annotated xml to create the processed xml
             */
            if (doc.hasChildNodes()) {
                populateDocNodes(doc, assetItrList, masterResource, imageList);
            }

            // create a temp file 
            processedXmlTempFile = File.createTempFile("targetFile-" + Calendar.getInstance().getTimeInMillis() + ".tmp", null);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            Source domSource = new DOMSource(doc);
            Result result = new StreamResult(processedXmlTempFile);
            transformer.transform(domSource, result);

            processedXmlInputStream = new FileInputStream(processedXmlTempFile);

            Resource subassetsFolderResource = resourceResolver.getResource(
                    DynamicDeckUtils.getOrCreateFolder(resourceResolver, DamConstants.SUBASSETS_FOLDER, deckResource));
            if (null != subassetsFolderResource) {
                AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
                if (assetManager == null) {
                    throw new DynamicDeckDynamoException("Asset manager is null");
                }
                processXmlAsset = assetManager.createAsset(subassetsFolderResource.getPath() + "/" + xmlName, processedXmlInputStream,
                        DynamicDeckDynamoConstants.XML_MIME_TYPE, true);
                LOGGER.debug("XML stored at {}", processXmlAsset.getPath());
            } else {
                throw new DynamicDeckDynamoException("Asset subfolder is null, where processed xml asset needs to be created "
                        + deckResource.getPath());
            }


        } catch (IOException | ParserConfigurationException | SAXException | TransformerException e) {
            throw new DynamicDeckDynamoException("Error while generation of xml", e);
        } finally {
            try {
                Files.deleteIfExists(processedXmlTempFile.toPath());
            } catch (IOException e) {
                LOGGER.error("Exception occurred while deleting the temp file", e);
            }
            if (null != processedXmlInputStream) {
                try {
                    processedXmlInputStream.close();
                } catch (IOException e) {
                    LOGGER.error("Exception occurred while closing the processed xml inputstream", e);
                }
            }
        }
        return processXmlAsset == null ? null : processXmlAsset.getPath();
    }

    private void populateDocNodes(Document doc, List<XMLResourceIterator> assetItrList, Resource masterResource, List<String> imageList) {
        populateHeaderValues(doc, masterResource, imageList);
        populateIterableValues(doc, assetItrList, imageList);
    }

    private void populateIterableValues(Document doc, List<XMLResourceIterator> assetItrList, List<String> imageList) {
        NodeList documentChildren = doc.getChildNodes();
        List<Node> iterableSections = new ArrayList<>();
        findSections(documentChildren, DynamicDeckDynamoConstants.XML_SECTION_TYPE_ITERABLE, iterableSections);
        Map<String, List<Node>> iterableSectionTypes = new HashMap<>();
        segregateSections(iterableSections, iterableSectionTypes);
        for (XMLResourceIterator assetItr : assetItrList) {
            String itrType = assetItr.getKey();
            if (iterableSectionTypes.containsKey(itrType)) {
                ListIterator<Resource> assetsIterator = assetItr.getIterator();
                List<Node> iterablesList = iterableSectionTypes.get(itrType);
                assetsIterator.forEachRemaining(assetResource -> {
                    ValueMap assetProperties = assetResource.getValueMap();
                    for (Node node : iterablesList) {
                        duplicateNodeAndSetValues(node, assetProperties, assetResource, imageList);
                    }
                });
                for (Node node : iterablesList) {
                    node.getParentNode().removeChild(node);
                }
            }
        }

    }

    private void populateHeaderValues(Document doc, Resource masterResource, List<String> imageList) {

        NodeList documentChildren = doc.getChildNodes();
        ValueMap masterResourceValueMap = masterResource.getValueMap();
        List<Node> headerSections = new ArrayList<>();
        findSections(documentChildren, DynamicDeckDynamoConstants.XML_SECTION_TYPE_MASTER, headerSections);
        headerSections.listIterator().forEachRemaining(section -> populateMasterAssetPaths(section, masterResource));
        headerSections.listIterator().forEachRemaining(section -> processSection(masterResource, masterResourceValueMap, section, imageList));

    }

    private void populateMasterAssetPaths(Node section, Resource masterResource) {
        Node assetPath = section.hasAttributes() ? section.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_ASSETPATH) : null;
        if (assetPath != null) {
            assetPath.setNodeValue(masterResource.getPath());
        }
    }

    private void processSection(Resource assetResource, ValueMap assetProperties, Node section, List<String> imageList) {
        NodeList sectionChildren = section.getChildNodes();
        for (int i = 0; i < sectionChildren.getLength(); i++) {
            Node childElement = sectionChildren.item(i);
            Node propertyType = childElement.hasAttributes() ? childElement.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_FIELD_TYPE) : null;

            if (propertyType != null) {
                switch (propertyType.getNodeValue()) {
                    case DynamicDeckDynamoConstants.XML_FIELD_TYPE_IMAGE:
                        populateImageProperty(assetProperties, childElement, assetResource.getPath(), imageList, assetResource.getResourceResolver());
                        break;
                    case DynamicDeckDynamoConstants.XML_FIELD_TYPE_TEXT:
                        populateTextProperty(assetProperties, childElement, assetResource.getPath());
                        break;
                    default:
                }
            }

        }
    }

    private void duplicateNodeAndSetValues(Node node, ValueMap assetProperties, Resource assetResource, List<String> imageList) {
        if (assetResource != null) {
            Node nodeCopy = node.cloneNode(true);
            Node assetPath = nodeCopy.hasAttributes() ? nodeCopy.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_ASSETPATH) : null;
            if (assetPath != null) {
                assetPath.setNodeValue(assetResource.getPath());
            }
            processSection(assetResource, assetProperties, nodeCopy, imageList);
            node.getParentNode().appendChild(nodeCopy);
        }
    }

    private String processPropertyPath(ValueMap assetProperties, Node childElement, String assetPath) {
        Node propertyPathAttr = childElement.hasAttributes() ? childElement.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_PROPERTY_PATH) : null;
        String propertyValue = null;
        if (propertyPathAttr != null) {
            String propertyPath = propertyPathAttr.getNodeValue();
            String metadataProperty = "jcr:content/metadata/" + propertyPath;
            if (StringUtils.equals(propertyPath, DynamicDeckDynamoConstants.XML_ATTR_VAL_SELF)) {
                propertyValue = assetPath;
            } else {
                Node isArrayAttr = childElement.hasAttributes() ? childElement.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_IS_ARRAY) : null;
                if (isArrayAttr != null && StringUtils.equals(isArrayAttr.getNodeValue(), "true")) {
                    if (assetProperties.containsKey(metadataProperty)) {
                        propertyValue = getArrayValue(assetProperties, metadataProperty);
                    } else {
                        propertyValue = getArrayValue(assetProperties, propertyPath);
                    }
                } else {
                    if (assetProperties.containsKey(metadataProperty)) {
                        propertyValue = assetProperties.get(metadataProperty, String.class);
                    } else {
                        propertyValue = assetProperties.get(propertyPath, String.class);
                    }
                }
            }
        }
        return propertyValue;
    }

    private String getArrayValue(ValueMap assetProperties, String propertyPath) {
        String propertyValue = null;
        String indexValue = StringUtils.substringBetween(propertyPath, "[", "]");
        propertyPath = StringUtils.substringBeforeLast(propertyPath, "[");
        String[] propertyValueArray = assetProperties.get(propertyPath, String[].class);
        if (propertyValueArray != null && StringUtils.isNumeric(indexValue)) {
            int index = Integer.parseInt(indexValue);
            if (index < propertyValueArray.length) {
                propertyValue = propertyValueArray[index];
            }
        }
        return propertyValue;
    }

    private void populateTextProperty(ValueMap assetProperties, Node childElement, String assetPath) {
        String propertyValue = processPropertyPath(assetProperties, childElement, assetPath);
        if (StringUtils.isNotBlank(propertyValue)) {
            childElement.setTextContent(propertyValue);
        }

    }

    private void populateImageProperty(ValueMap assetProperties, Node childElement, String assetPath, List<String> imageList, ResourceResolver resourceResolver) {
        String propertyValue = processPropertyPath(assetProperties, childElement, assetPath);
        if (StringUtils.isNotBlank(propertyValue)) {
            Resource assetResource = resourceResolver.getResource(propertyValue);
            if (DamUtil.isAsset(assetResource)) {
                ((Element) childElement).setAttribute(BindConstants.XML_HREF, DynamicDeckDynamoConstants.FILE_PATH_PREFIX
                        + StringUtils.substringAfterLast(propertyValue, "/"));
                if (!isImageAdded(imageList, assetResource)) {
                    imageList.add(assetResource.getPath());
                }
            }

        }

    }

    private boolean isImageAdded(List<String> imageList, Resource assetResource) {
        String assetName = assetResource.getName();
        for (String path : imageList) {
            if (StringUtils.equals(StringUtils.substringAfterLast(path, "/"), assetName)) {
                return true;
            }
        }
        return false;
    }

    private void segregateSections(List<Node> iterableSections, Map<String, List<Node>> iterableSectionTypes) {
        for (Node section : iterableSections) {
            Node itrSecs = section.hasAttributes() ? section.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_ITERABLE_TYPE) : null;
            if (itrSecs != null) {
                String itrType = itrSecs.getNodeValue();
                List<Node> itrTypesList;
                if (!iterableSectionTypes.containsKey(itrType)) {
                    itrTypesList = new ArrayList<>();
                } else {
                    itrTypesList = iterableSectionTypes.get(itrType);
                }
                itrTypesList.add(section);
                iterableSectionTypes.put(itrType, itrTypesList);

            }

        }
    }

    private void findSections(NodeList nodeList, String type, List<Node> sectionList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);

            Node secType = childNode.hasAttributes() ? childNode.getAttributes().getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_SECTION_TYPE) : null;
            if (secType != null && secType.getNodeValue().equals(type)) {
                sectionList.add(childNode);

            } else {
                findSections(childNode.getChildNodes(), type, sectionList);
            }
        }

    }
}
