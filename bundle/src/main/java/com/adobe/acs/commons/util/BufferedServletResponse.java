/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Servlet response wrapper which buffers all output being written to {@link #getOutputStream()} or {@link #getWriter()}. The response cannot
 * be committed via {@link #flushBuffer} but only via {@link #close()}. Access to the underlying buffer is provided via
 * {@link #getBufferedBytes()} and {@link #getBufferedString()} respectively. 
 */
public class BufferedServletResponse extends ServletResponseWrapper implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(BufferedServletResponse.class);

    public enum ResponseWriteMethod {
        OUTPUTSTREAM, WRITER
    }

    private final StringWriter writer;
    private final PrintWriter printWriter;
    private final ByteArrayOutputStream outputStream;
    private final ServletOutputStream servletOutputStream;
    private boolean flushBuffer;
    private ResponseWriteMethod writeMethod;

    /** Creates a new response wrapper which buffers both the writer and the output stream.
     * 
     * @param wrappedResponse the wrapped response
     */
    public BufferedServletResponse(ServletResponse wrappedResponse) {
        this(wrappedResponse, new StringWriter(), new ByteArrayOutputStream());
    }

    /** Creates a new response wrapper using the given StringWriter and OutputStream as buffers.
     * 
     * @param wrappedResponse the wrapped response
     * @param writer          the writer to use as buffer (may be {@code null} in case you don't want to buffer the writer)
     * @param outputStream    the {@link ByteArrayOutputStream} to use as buffer for {@link #getOutputStream()) (may be {@code null} in case
     *                            you don't want to buffer the output stream)
     */
    public BufferedServletResponse(ServletResponse wrappedResponse, StringWriter writer, ByteArrayOutputStream outputStream) {
        super(wrappedResponse);
        this.writer = writer;
        if (writer != null) {
            this.printWriter = new PrintWriter(writer);
        } else {
            this.printWriter = null;
        }
        this.outputStream = outputStream;
        if (outputStream != null) {
            this.servletOutputStream = new ServletOutputStreamWrapper(outputStream);
        } else {
            this.servletOutputStream = null;
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (ResponseWriteMethod.WRITER.equals(this.writeMethod)) {
            throw new IllegalStateException("Cannot invoke getOutputStream() once getWriter() has been called.");
        }
        this.writeMethod = ResponseWriteMethod.OUTPUTSTREAM;
        if (servletOutputStream != null) {
            return servletOutputStream;
        } else {
            return super.getOutputStream();
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (ResponseWriteMethod.OUTPUTSTREAM.equals(this.writeMethod)) {
            throw new IllegalStateException("Cannot invoke getWriter() once getOutputStream() has been called.");
        }
        this.writeMethod = ResponseWriteMethod.WRITER;
        if (printWriter != null) {
            return printWriter;
        } else {
            return super.getWriter();
        }
    }

    /**
     * @return {@link ResponseWriteMethod#OUTPUTSTREAM} in case {@link #getOutputStream()} has been called,
     *         {@link ResponseWriteMethod#WRITER} in case {@link #getWriter()} has been called, {@code null} in case none of those have been
     *         called yet. */
    public ResponseWriteMethod getWriteMethod() {
        return writeMethod;
    }

    /**
     * 
     * @return the buffered string which is the content of the response being written via {@link #getWriter()}
     * @throws IllegalStateException in case {@link #getWriter()} has not been called yet or the writer was not buffered.
     */
    public String getBufferedString() {
        if (ResponseWriteMethod.OUTPUTSTREAM.equals(this.writeMethod)) {
            throw new IllegalStateException("Cannot invoke getBufferedString() once getWriter() has been called.");
        }
        if (writer == null) {
            throw new IllegalStateException("Cannot get buffered string, as the writer was not buffered!");
        }
        return writer.toString();
    }

    /**
     * 
     * @return the buffered bytes which which were written via {@link #getOutputStream()}
     * @throws IllegalStateException in case {@link #getOutputStream()} has not been called yet or the output stream was not buffered.
     */
    public byte[] getBufferedBytes() {
        if (ResponseWriteMethod.WRITER.equals(this.writeMethod)) {
            throw new IllegalStateException("Cannot invoke getBufferedBytes() once getOutputStream() has been called.");
        }
        if (outputStream == null) {
            throw new IllegalStateException("Cannot get buffered bytes, as the output stream was not buffered!");
        }
        return outputStream.toByteArray();
    }

    /** 
     * Closing leads to spooling the buffered output stream or writer to the underlying/wrapped response.
     * Also this will automatically commit the response in case {@link #flushBuffer} has been called previously!
     * 
     * @throws IOException */
    @Override
    public void close() throws IOException {
        if (ResponseWriteMethod.OUTPUTSTREAM.equals(this.writeMethod)) {
            super.getOutputStream().write(getBufferedBytes());
        } else if (ResponseWriteMethod.WRITER.equals(this.writeMethod)) {
            super.getWriter().write(getBufferedString());
        }
        if (flushBuffer) {
            super.flushBuffer();
        }
    }

    /**
     * Will not commit the response, but only make sure that the wrapped response's {@code flushBuffer()} is called, once this response is closed
     */
    @Override
    public void flushBuffer() throws IOException {
        log.error("Prevent committing the response, it will be committed deferred, i.e. once this response is closed");
        flushBuffer = true;
    }

}
