package com.adobe.acs.commons.httpcache.engine;

import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * Represents response content to be cached.
 */
public class CacheContent {
    private static final Logger log = LoggerFactory.getLogger(CacheContent.class);

    /** Response character encoding */
    private String charEncoding;
    /** Response content type */
    private String contentType;
    /** Response headers */
    private Map<String, List<String>> headers = new HashMap<>();
    /** Response content in file */
    private InputStream dataInputStream;

    /**
     * Making constructor private forcing instances to be made through <code>build</code> method.
     */
    private CacheContent() {
    }

    /**
     * Construct from the custom servlet response wrapper..
     *
     * @param responseWrapper
     * @return
     */
    public CacheContent build(HttpCacheServletResponseWrapper responseWrapper) throws HttpCacheDataStreamException {
        CacheContent cacheContent = new CacheContent();

        // Exract information from response and populate state of the instance.
        this.charEncoding = responseWrapper.getCharacterEncoding();
        this.contentType = responseWrapper.getContentType();

        // Extracting header K,V.
        responseWrapper.getHeaderNames();
        List<String> headerNames = Collections.list((Enumeration<String>) responseWrapper.getHeaderNames());
        for (String headerName : headerNames) {
            headers.put(headerName, Collections.list((Enumeration<String>) responseWrapper.getHeaders(headerName)));
        }

        // Get hold of the response content available in a temporary file.
        try {
            this.dataInputStream = new FileInputStream(responseWrapper.getTempCacheFile());
        } catch (FileNotFoundException e) {
            throw new HttpCacheDataStreamException("Temp cache file not found.", e);
        }

        return this;
    }

    /**
     * Get character encoding.
     *
     * @return
     */
    public String getCharEncoding() {
        return charEncoding;
    }

    /**
     * Get content type.
     *
     * @return
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get headers.
     *
     * @return Headers in map of list.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Get input stream of response content
     *
     * @return
     */
    public InputStream getDataStream() {
        return dataInputStream;
    }
}
