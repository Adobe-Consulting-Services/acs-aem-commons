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
