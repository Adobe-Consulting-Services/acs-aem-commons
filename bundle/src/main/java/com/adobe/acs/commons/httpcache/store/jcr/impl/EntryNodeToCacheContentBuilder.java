package com.adobe.acs.commons.httpcache.store.jcr.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import com.adobe.acs.commons.httpcache.engine.CacheContent;

public class EntryNodeToCacheContentBuilder
{
    private final Node entryNode;


    private String contentType, charEncoding;
    private int status;
    private final InputStream inputStream;
    private final Map<String, List<String>> headers = new HashMap<>();

    public EntryNodeToCacheContentBuilder(Node entryNode) throws RepositoryException
    {
        this.entryNode = entryNode;

        retrieveHeaders();
        retrieveProperties();
        inputStream = retrieveInputStream();
    }

    private void retrieveProperties() throws RepositoryException
    {
        final PropertyIterator propertyIterator = entryNode.getProperties();

        while(propertyIterator.hasNext()){
            final Property property = propertyIterator.nextProperty();
            final String propName = property.getName();
            final Value value = property.getValue();

            if(propName.equals("content-type"))
                contentType = value.getString();
            else if(propName.equals("char-encoding"))
                charEncoding = value.getString();
            else if(propName.equals("status"))
                status = (int) value.getLong();
        }
    }

    public CacheContent build() throws RepositoryException
    {
        return new CacheContent(
            status,
            charEncoding,
            contentType,
            headers,
            inputStream
        );
    }

    private void retrieveHeaders() throws RepositoryException
    {
       final Node headerNode = entryNode.getNode("headers");

       final PropertyIterator propertyIterator = headerNode.getProperties();

       while(propertyIterator.hasNext()){
           final Property property = propertyIterator.nextProperty();
           final String name = property.getName();
           if(! name.contains("jcr:") && !name.contains("sling:")){
               Value[] values = property.getValues();
               List<String> stringValues = new ArrayList<>(values.length);

               for(Value value : values)
                   stringValues.add(value.getString());

               headers.put(name, stringValues);
           }
       }

    }

    private InputStream retrieveInputStream() throws RepositoryException
    {
        Node contentsNode = entryNode.getNode("contents");
        Property binaryProperty = contentsNode.getProperty("binary");
        return binaryProperty.getBinary().getStream();
    }

}
