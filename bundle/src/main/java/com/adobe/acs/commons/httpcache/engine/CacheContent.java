package com.adobe.acs.commons.httpcache.engine;

import java.io.File;
import java.util.*;

/**
 * Represents response content to be cached.
 */
public class CacheContent {
    /** Response character encoding */
    private String charEncoding;
    /** Response content type */
    private String contentType;
    /** Response headers */
    private Map<String, List<String>> headers = new HashMap<>();
    /** Response content in file */
    private File responseContent;

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
    public CacheContent build(HttpCacheServletResponseWrapper responseWrapper) {
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
        this.responseContent = responseWrapper.getTempCacheFile();

        return this;
    }
}
