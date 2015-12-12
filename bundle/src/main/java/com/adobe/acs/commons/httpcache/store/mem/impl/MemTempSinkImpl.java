package com.adobe.acs.commons.httpcache.store.mem.impl;

import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.store.TempSink;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TempSink implementation for In-Mem cache store.
 */
public class MemTempSinkImpl implements TempSink {
    /** Byte array as sink */
    private byte[] sink;
    private ByteArrayOutputStream byteArrayOutputStream;

    @Override
    public OutputStream createOutputStream() {

        if (null == byteArrayOutputStream) {
            byteArrayOutputStream = new ByteArrayOutputStream();
        }
        return byteArrayOutputStream;
    }

    @Override
    public InputStream createInputStream() throws HttpCacheDataStreamException {

        if (null != byteArrayOutputStream && null == sink) {
            sink = byteArrayOutputStream.toByteArray();
        }
        if (null != sink) {
            return new ByteArrayInputStream(sink);
        } else {
            throw new HttpCacheDataStreamException("Nothing available in sink.");
        }
    }

    @Override
    public long length() {
        if (null == sink) {
            return -1;
        }
        return sink.length;
    }
}
