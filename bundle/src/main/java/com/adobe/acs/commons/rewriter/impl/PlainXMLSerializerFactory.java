package com.adobe.acs.commons.rewriter.impl;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.SerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
@Property(name = "pipeline.type", value = "xml-serializer")
public class PlainXMLSerializerFactory implements SerializerFactory {

    private static final Logger log = LoggerFactory.getLogger(PlainXMLSerializerFactory.class);

    @Override
    public Serializer createSerializer() {
        try {
            return new PlainXMLSerializer();
        } catch (TransformerConfigurationException e) {
            log.error("Unable to create serializer", e);
            return null;
        }
    }
}
