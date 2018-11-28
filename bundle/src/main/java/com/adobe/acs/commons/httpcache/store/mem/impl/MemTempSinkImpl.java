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
            return new ByteArrayInputStream(new byte[0]);
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
