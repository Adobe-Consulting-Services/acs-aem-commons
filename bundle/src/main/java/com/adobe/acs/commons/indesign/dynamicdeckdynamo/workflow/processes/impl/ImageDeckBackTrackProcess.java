package com.adobe.acs.commons.indesign.dynamicdeckdynamo.workflow.processes.impl;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.w3c.dom.Node;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants.DynamicDeckDynamoConstants;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;

public class ImageDeckBackTrackProcess extends DynamicDeckBackTrackProcess{
    


    public static void handleImageType(Resource assetResource, Node childNode, ResourceResolver resourceResolver, Boolean isArray) throws DynamicDeckDynamoException {
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
                        return;
                    }
                    Resource imageResource = resourceResolver.getResource(completeHrefValue);
                    if (DamUtil.isAsset(imageResource)) {
                        setNewPropertyValue(isArray, propertyPath, assetResource, completeHrefValue);

                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new DynamicDeckDynamoException("Exception while handling the image type.", e);
            }
        }
    }

    private static String getAssetPropertyPath(String nodeValue, ValueMap properties) {
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
