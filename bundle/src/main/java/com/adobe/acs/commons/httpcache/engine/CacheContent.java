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
package com.adobe.acs.commons.httpcache.engine;

import com.adobe.acs.commons.httpcache.engine.impl.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.store.TempSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, List<String>> headers = new HashMap<String, List<String>>();
    /** Response content as input stream */
    private InputStream dataInputStream;
    /** Temp sink attached to this cache content */
    private TempSink tempSink;

    /**
     * Construct <code>CacheContent</code> using parameters. Prefer constructing an instance using <code>build</code>
     * method.
     *
     * @param charEncoding
     * @param contentType
     * @param headers
     * @param dataInputStream
     */
    public CacheContent(String charEncoding, String contentType, Map<String, List<String>> headers, InputStream
            dataInputStream) {

        this.charEncoding = charEncoding;
        this.contentType = contentType;
        this.headers = headers;
        this.dataInputStream = dataInputStream;
    }

    /**
     * No argument constructor for the build method.
     */
    public CacheContent() {
    }

    /**
     * Construct from the custom servlet response wrapper..
     *
     * @param responseWrapper
     * @return
     */
    public CacheContent build(HttpCacheServletResponseWrapper responseWrapper) throws HttpCacheDataStreamException {
        // Extract information from response and populate state of the instance.
        this.charEncoding = responseWrapper.getCharacterEncoding();
        this.contentType = responseWrapper.getContentType();

        // Extracting header K,V.
        List<String> headerNames = new ArrayList<String>();
        headerNames.addAll(responseWrapper.getHeaderNames());
        for (String headerName : headerNames) {
            List<String> values = new ArrayList<String>();
            values.addAll(responseWrapper.getHeaders(headerName));
            headers.put(headerName, values);
        }

        // Get hold of the temp sink.
        this.tempSink = responseWrapper.getTempSink();

        // Get hold of the response content available in sink.
        this.dataInputStream = responseWrapper.getTempSink().createInputStream();

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
    public InputStream getInputDataStream() {
        return dataInputStream;
    }

    /**
     * Get the temp size attached to this cache content.
     * @return
     */
    public TempSink getTempSink(){
        return this.tempSink;
    }
}
