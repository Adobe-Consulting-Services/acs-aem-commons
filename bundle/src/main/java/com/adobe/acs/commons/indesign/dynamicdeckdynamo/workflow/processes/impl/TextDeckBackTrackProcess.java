package com.adobe.acs.commons.indesign.dynamicdeckdynamo.workflow.processes.impl;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.w3c.dom.Node;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants.DynamicDeckDynamoConstants;

public class TextDeckBackTrackProcess extends DynamicDeckBackTrackProcess {

    public static void handleTextType(Resource assetResource, Node childNode, Boolean isArray) {
        Node propertyPathNode = childNode.getAttributes()
                .getNamedItem(DynamicDeckDynamoConstants.XML_ATTR_PROPERTY_PATH);
        if (propertyPathNode != null) {
            String propertyPath = getAssetPropertyPath(propertyPathNode.getNodeValue(), assetResource.getValueMap());
            String textValue = childNode.getTextContent();
            setNewPropertyValue(isArray, propertyPath, assetResource, textValue);
        }
    }

    private  static String getAssetPropertyPath(String nodeValue, ValueMap properties) {
        String propertyPath = nodeValue;
        if (!properties.containsKey(nodeValue)) {
            propertyPath = "jcr:content/metadata/" + propertyPath;
        }
        return propertyPath;
    }

    private static  void setNewPropertyValue(Boolean isArray, String propertyPath, Resource assetResource,
            String nodeContentValue) {
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
}
