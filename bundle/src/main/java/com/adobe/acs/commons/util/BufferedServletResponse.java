/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

/** 
 * A wrapper around a {@link ServletResponse}  which buffers all output being written to {@link #getOutputStream()} or {@link #getWriter()}. The response cannot
 * be committed via {@link #flushBuffer} but only via {@link #close()}. Access to the underlying buffer is provided via
 * {@link #getBufferedServletOutput()}. 
 */
public class BufferedServletResponse extends ServletResponseWrapper implements Closeable {

    private final BufferedServletOutput bufferedOutput;

    public BufferedServletResponse(ServletResponse wrappedResponse) {
        super(wrappedResponse);
        this.bufferedOutput = new BufferedServletOutput(wrappedResponse);
    }

    public BufferedServletResponse(ServletResponse wrappedResponse, StringWriter writer, ByteArrayOutputStream outputStream) {
        super(wrappedResponse);
        this.bufferedOutput = new BufferedServletOutput(wrappedResponse, writer, outputStream);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return bufferedOutput.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return bufferedOutput.getWriter();
    }

    @Override
    public void flushBuffer() throws IOException {
        bufferedOutput.flushBuffer();
    }

    @Override
    public void resetBuffer() {
        bufferedOutput.resetBuffer();
    }

    @Override
    public void close() throws IOException {
        bufferedOutput.close();
    }
    
    public BufferedServletOutput getBufferedServletOutput() {
        return bufferedOutput;
    }

}
