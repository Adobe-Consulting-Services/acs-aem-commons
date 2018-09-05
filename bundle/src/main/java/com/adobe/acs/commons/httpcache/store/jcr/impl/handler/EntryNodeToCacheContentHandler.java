/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.httpcache.store.jcr.impl.handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;

public class EntryNodeToCacheContentHandler
{
    private final Node entryNode;

    private String contentType;
    private String charEncoding;
    private int status;
    private InputStream inputStream;
    private final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private Binary binary;

    private static final String SLING_NAMESPACE = SlingConstants.NAMESPACE_PREFIX + ":";
    private static final String JCR_NAMESPACE = "jcr:";

    public EntryNodeToCacheContentHandler(Node entryNode) throws RepositoryException
    {
        this.entryNode = entryNode;

        if(entryNode != null){
            retrieveHeaders();
            retrieveProperties();
            inputStream = retrieveInputStream();
        }
    }

    private void retrieveProperties() throws RepositoryException
    {
        final PropertyIterator propertyIterator = entryNode.getProperties();

        while(propertyIterator.hasNext()){
            final Property property = propertyIterator.nextProperty();
            final String propName = property.getName();
            final Value value = property.getValue();

            if(propName.equals(JCRHttpCacheStoreConstants.PN_CONTENT_TYPE)) {
                contentType = value.getString();
            }
            else if(propName.equals(JCRHttpCacheStoreConstants.PN_CHAR_ENCODING)) {
                charEncoding = value.getString();
            }
            else if(propName.equals(JCRHttpCacheStoreConstants.PN_STATUS)) {
                status = (int) value.getLong();
            }
        }
    }

    public CacheContent get() throws RepositoryException
    {
        return new CacheContent(
            status,
            charEncoding,
            contentType,
            headers,
            inputStream
        );
    }

    public Binary getBinary(){
        return binary;
    }

    private void retrieveHeaders() throws RepositoryException
    {
        if(entryNode.hasNode(JCRHttpCacheStoreConstants.PATH_HEADERS)){
            final Node headerNode = entryNode.getNode(JCRHttpCacheStoreConstants.PATH_HEADERS);

            final PropertyIterator propertyIterator = headerNode.getProperties();

            while(propertyIterator.hasNext()){
                final Property property = propertyIterator.nextProperty();
                final String name = property.getName();
                if(! isNativeProperty(name)){
                    Value[] values = property.getValues();
                    List<String> stringValues = new ArrayList<String>(values.length);

                    for(Value value : values) {
                        stringValues.add(value.getString());
                    }

                    headers.put(name, stringValues);
                }
            }
        }
    }

    private boolean isNativeProperty(String propertyName)
    {
        return
                 propertyName.startsWith(JCR_NAMESPACE)
                         &&
                !propertyName.startsWith(SLING_NAMESPACE);
    }

    private InputStream retrieveInputStream() throws RepositoryException
    {
        if(entryNode.hasNode(JCRHttpCacheStoreConstants.PATH_CONTENTS)){
            final Node contentsNode = entryNode.getNode(JCRHttpCacheStoreConstants.PATH_CONTENTS);
            final Node jcrContent =   contentsNode.getNode(JcrConstants.JCR_CONTENT);

            final Property binaryProperty = jcrContent.getProperty(JcrConstants.JCR_DATA);
            binary =  binaryProperty.getBinary();

            return binary.getStream();
        }
        return null;

    }

}
