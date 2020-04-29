package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception.DynamicDeckDynamoException;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils.XMLResourceIterator;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ProviderType;

import java.io.InputStream;
import java.util.List;

/**
 * This service will be used to parse xml.
 */
@ProviderType
public interface XMLGeneratorService {

    String generateInddXML(InputStream xmlInputStream, List<XMLResourceIterator> assetResourceIterList,
                           Resource masterResource, Resource deckResource, ResourceResolver resourceResolver, List<String> inddImageList) throws DynamicDeckDynamoException;
}
