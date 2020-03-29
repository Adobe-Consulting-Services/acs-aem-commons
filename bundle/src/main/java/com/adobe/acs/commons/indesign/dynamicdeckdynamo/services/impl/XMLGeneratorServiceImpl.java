package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.impl;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants.DynamicDeckDynamoConstants;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.pojos.XMLResourceIterator;
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
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

        InputStream resultInputStream;
        Asset processXmlAsset = null;
        final String xmlName = "processedXml.xml";

        if (null == masterResource) {
            masterResource = deckResource;
        }

        File targetFile = new File("targetFile-" + Calendar.getInstance().getTimeInMillis() + DynamicDeckDynamoConstants.DASH
                + StringUtils.substringBeforeLast(xmlName, DynamicDeckDynamoConstants.DOT) + ".tmp");

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Files.copy(xmlInputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(targetFile);

            if (doc.hasChildNodes()) {
                populateDocNodes(doc, assetItrList, masterResource, imageList);
            }

            Result outputTarget = new StreamResult(outputStream);
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.newTransformer().transform(new DOMSource(doc), outputTarget);
            resultInputStream = new ByteArrayInputStream(outputStream.toByteArray());

            Resource subassetsFolderResource = resourceResolver.getResource(
                    DynamicDeckUtils.getOrCreateFolder(resourceResolver, DamConstants.SUBASSETS_FOLDER, deckResource));
            if (null != subassetsFolderResource) {
                AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
                if (assetManager == null) {
                    LOGGER.error("Asset manager is null");
                    return null;
                }
                processXmlAsset = assetManager.createAsset(
                        subassetsFolderResource.getPath() + DynamicDeckDynamoConstants.SLASH + xmlName, resultInputStream,
                        DynamicDeckDynamoConstants.XML_MIME_TYPE, true);
                LOGGER.debug("XML stored at {}", processXmlAsset.getPath());
            } else {
                LOGGER.debug("Asset subfolder is null, where processed xml asset needs to be created {}",
                        deckResource.getPath());
            }
        } catch (IOException | ParserConfigurationException | SAXException | TransformerException e) {
            LOGGER.error("Error while generation of xml.", e);
        } finally {
            if (!targetFile.delete()) {
                throw new DynamicDeckDynamoException("Temporary file cannot be deleted or it's null");
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
            String metadataProperty = DynamicDeckDynamoConstants.DAM_METADATA + DynamicDeckDynamoConstants.SLASH + propertyPath;
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
                        + StringUtils.substringAfterLast(propertyValue, DynamicDeckDynamoConstants.SLASH));
                if (!isImageAdded(imageList, assetResource)) {
                    imageList.add(assetResource.getPath());
                }
            }

        }

    }

    private boolean isImageAdded(List<String> imageList, Resource assetResource) {
        String assetName = assetResource.getName();
        for (String path : imageList) {
            if (StringUtils.equals(StringUtils.substringAfterLast(path, DynamicDeckDynamoConstants.SLASH), assetName)) {
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
