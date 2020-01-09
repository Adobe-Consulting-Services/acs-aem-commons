package com.adobe.acs.commons.synth.impl.support;

import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;


class SyntheticRequestParameter implements RequestParameter {
    
    private static final Logger LOG = LoggerFactory.getLogger(SyntheticRequestParameter.class);
    
    private String name;
    private String encoding = StandardCharsets.UTF_8.name();
    private String value;
    private byte[] content;
    
    public SyntheticRequestParameter(String name, String value) {
        this.name = name;
        this.value = value;
        this.content = null;
    }
    
    void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public String getEncoding() {
        return this.encoding;
    }
    
    public byte[] get() {
        if (this.content == null) {
            try {
                this.content = this.getString().getBytes(this.getEncoding());
            } catch (UnsupportedEncodingException e) {
                LOG.warn("Unsupported encoding '{}', using platform default", this.getEncoding(), e);
                this.content = this.getString().getBytes();
            }
        }
        
        return this.content;
    }
    
    public String getContentType() {
        return null;
    }
    
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.get());
    }
    
    public String getFileName() {
        return null;
    }
    
    public long getSize() {
        return (long) this.get().length;
    }
    
    public String getString() {
        return this.value;
    }
    
    public String getString(String encoding) throws UnsupportedEncodingException {
        return new String(this.get(), encoding);
    }
    
    public boolean isFormField() {
        return true;
    }
    
    public String toString() {
        return this.getString();
    }
}
