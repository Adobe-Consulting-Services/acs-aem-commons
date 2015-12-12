package com.adobe.acs.commons.httpcache.store;

import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Temporary sink for duplicate servlet response stream. Cache stores are expected to provide its own implementation.
 */
public interface TempSink {
    /**
     * Create an output stream to write to the sink.
     *
     * @return
     * @throws HttpCacheDataStreamException
     */
    OutputStream createOutputStream() throws HttpCacheDataStreamException;

    /**
     * Creates an input stream to read from the sink.
     *
     * @return
     * @throws HttpCacheDataStreamException
     */
    InputStream createInputStream() throws HttpCacheDataStreamException;

    /**
     * Get the length of the sink.
     *
     * @return Length of sink in bytes or -1 if unknown.
     */
    long length();
}
