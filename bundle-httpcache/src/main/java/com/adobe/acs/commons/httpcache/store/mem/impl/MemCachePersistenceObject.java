package com.adobe.acs.commons.httpcache.store.mem.impl;

import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Value for cache item in mem store.
 */
class MemCachePersistenceObject {
    /** Response character encoding */
    private String charEncoding;
    /** Response content type */
    private String contentType;
    /** Response headers */
    Multimap<String, String> headers;
    /** Byte array to hold the data from the stream */
    private byte[] bytes;

    /** Create <code>MemCachePersistenceObject</code>. Use <code>buildForCaching</code> method to initialize
     * parameters. */
    MemCachePersistenceObject() {
    }

    /**
     * Construct a Mem cache value suitable for caching. This constructor takes deep copy of parameters making the
     * object suitable for caching avoiding any memory leaks.
     *
     * @param charEncoding
     * @param contentType
     * @param headers
     * @param dataInputStream
     * @throws HttpCacheDataStreamException
     */
    public MemCachePersistenceObject buildForCaching(String charEncoding, String contentType, Map<String,
            List<String>> headers, InputStream dataInputStream) throws HttpCacheDataStreamException {
        // Taken copy of arguments before caching them to avoid chances of memory leak.
        // Take copy of originals
        this.charEncoding = new String(charEncoding);
        this.contentType = new String(contentType);

        // Iterate headers and take a copy.
        this.headers = HashMultimap.create();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                this.headers.put(new String(entry.getKey()), new String(value));
            }
        }

        // Read input stream and place it in a byte array.
        try {
            this.bytes = IOUtils.toByteArray(dataInputStream);
        } catch (IOException e) {
            throw new HttpCacheDataStreamException("Unable to get byte array out of stream", e);
        }
        return this;
    }

    /**
     * Get char encoding.
     *
     * @return
     */
    public String getCharEncoding() {
        return charEncoding;
    }

    /**
     * Get content type
     *
     * @return
     */
    public String getContentType() {
        return contentType;
    }


    /**
     * Get the header in multimap format.
     *
     * @return Returned in <code>Map<String, List<String>></code> format.
     */
    public Map<String, List<String>> getHeaders() {
        return (Map<String, List<String>>) (Map<?, ?>) Multimaps.asMap(headers);
    }

    /**
     * Get the data byte array
     *
     * @return
     */
    public byte[] getBytes() {
        return bytes;
    }
}
