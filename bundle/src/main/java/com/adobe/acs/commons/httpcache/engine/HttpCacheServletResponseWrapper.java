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

import com.adobe.acs.commons.httpcache.engine.impl.TeePrintWriter;
import com.adobe.acs.commons.httpcache.engine.impl.TeeServletOutputStream;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.store.TempSink;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;

/**
 * Wrapper for <code>SlingHttpServletResponse</code>. Wrapped to get hold of the copy of servlet response stream.
 */
public class HttpCacheServletResponseWrapper extends SlingHttpServletResponseWrapper {

    public enum ResponseWriteMethod {
        OUTPUTSTREAM,
        PRINTWRITER
    }

    private static final Logger log = LoggerFactory.getLogger(HttpServletResponseWrapper.class);

    private PrintWriter printWriter;
    private ServletOutputStream servletOutputStream;
    private final TempSink tempSink;

    private ResponseWriteMethod writeMethod;

    public HttpCacheServletResponseWrapper(SlingHttpServletResponse wrappedResponse, TempSink tempSink) throws
            IOException {
        super(wrappedResponse);
        this.tempSink = tempSink;
    }


    @Override
    @SuppressWarnings("squid:S2095") // closing is responsibility of caller
    public ServletOutputStream getOutputStream() throws IOException {
        if (ResponseWriteMethod.PRINTWRITER.equals(this.writeMethod)) {
            throw new IllegalStateException("Cannot invoke getOutputStream() once getWriter() has been called.");
        } else if (this.servletOutputStream == null) {
            try {
                this.servletOutputStream = new TeeServletOutputStream(super.getOutputStream(), tempSink
                        .createOutputStream());
                this.writeMethod = ResponseWriteMethod.OUTPUTSTREAM;
            } catch (HttpCacheDataStreamException e) {
                log.error("Temp sink is unable to provide an output stream.");
            }
        }

        return this.servletOutputStream;

    }

    @Override
    @SuppressWarnings("squid:S2095") // closing is responsibility of caller
    public PrintWriter getWriter() throws IOException {
        if (ResponseWriteMethod.OUTPUTSTREAM.equals(this.writeMethod)) {
            throw new IllegalStateException("Cannot invoke getWriter() once getOutputStream() has been called.");
        } else if (this.printWriter == null) {
            try {
                final Writer tempWriter = new OutputStreamWriter(tempSink.createOutputStream(), getResponse().getCharacterEncoding());
                this.printWriter = new TeePrintWriter(super.getWriter(), new PrintWriter(tempWriter));
                this.writeMethod = ResponseWriteMethod.PRINTWRITER;
            } catch (HttpCacheDataStreamException e) {
                log.error("Temp sink is unable to provide an output stream.");
            }
        }

        return this.printWriter;
    }

    @Override
    public Collection<String> getHeaderNames() {
         try {
            return super.getHeaderNames();
        } catch (AbstractMethodError e) {
             log.debug("Known issue when internal sling redirects are made - the call to getHeaders() will throw an exception.", e);
            return Collections.EMPTY_LIST;
        }
    }

    public TempSink getTempSink() {
        return tempSink;
    }

    public ResponseWriteMethod getWriteMethod() {
        return writeMethod;
    }
}
