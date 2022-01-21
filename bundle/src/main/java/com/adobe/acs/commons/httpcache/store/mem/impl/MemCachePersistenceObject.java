/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.store.mem.impl;

import com.adobe.acs.commons.httpcache.engine.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Value for cache item in mem store.
 */
public class MemCachePersistenceObject implements Serializable {
    /** Response status **/
    private int status;
    /** Response character encoding */
    private String charEncoding;
    /** Response content type */
    private String contentType;
    /** Response headers */
    transient Multimap<String, String> headers;
    /** Byte array to hold the data from the stream */
    private byte[] bytes;
    private HttpCacheServletResponseWrapper.ResponseWriteMethod writeMethod;

    AtomicInteger count = new AtomicInteger(0);

    /**
     * Create <code>MemCachePersistenceObject</code>. Use <code>buildForCaching</code> method to initialize parameters.
     */
    public MemCachePersistenceObject() {
        //empty constructor
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
    public MemCachePersistenceObject buildForCaching(int status, String charEncoding, String contentType, Map<String,
            List<String>> headers, InputStream dataInputStream, HttpCacheServletResponseWrapper.ResponseWriteMethod writeMethod) throws HttpCacheDataStreamException {

        this.status = status;
        this.charEncoding = charEncoding;
        this.contentType = contentType;
        this.writeMethod = writeMethod;

        // Iterate headers and take a copy.
        this.headers = HashMultimap.create();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                if (!"Sling-Tracer-Protocol-Version".equals(entry.getKey()) && !"Sling-Tracer-Request-Id".equals(entry.getKey())) {
                    // Do NOT cache Sling Tracer headers as this makes debugging difficult and confusing!
                    this.headers.put(entry.getKey(), value);
                }
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
     * Get response status
     * @return the status code
     */
    public int getStatus() {
        return status;
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
        Map<String, List<String>> map = new HashMap<String, List<String>>();

        // Convert com.google.common.collect.AbstractMapBasedMultimap$WrappedSet to List<String> value to avoid cast
        // exception
        for (Map.Entry<String, Collection<String>> entry : Multimaps.asMap(headers).entrySet()) {
            map.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }

        return map;
    }

    /**
     * Get the data byte array
     *
     * @return
     */
    public byte[] getBytes() {
        return Optional.ofNullable(bytes)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(new byte[0]);
    }


    /**
     * Increments the hit for this cache entry.
     */
    public void incrementHitCount() {
        count.incrementAndGet();
    }

    /**
     * @return the number of times this cache entry has been requested
     */
    public int getHitCount() {
        return count.get();
    }

    public HttpCacheServletResponseWrapper.ResponseWriteMethod getWriteMethod() {
        return writeMethod;
    }
}
