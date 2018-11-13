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
package com.adobe.acs.commons.httpcache.engine.impl;

import org.apache.commons.io.output.TeeOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes to 2 outputstream. Required to take copy of the servlet response.
 */
public class TeeServletOutputStream extends ServletOutputStream {
    private final TeeOutputStream teeOutputStream;
    private WriteListener listener = new WriteListener() {
        @Override
        public void onWritePossible() throws IOException {
            // Do nothing
        }

        @Override
        public void onError(Throwable throwable) {
            // Do nothing
        }
    };

    public TeeServletOutputStream(OutputStream one, OutputStream two) {
        // Uses Apache IO TeeOutputStream
        teeOutputStream = new TeeOutputStream(one, two);
    }

    @Override
    public void write(int character) throws IOException {
        try {
            this.teeOutputStream.write(character);
        } catch (IOException ex) {
            listener.onError(ex);
            throw ex;
        }
    }

    @Override
    public void write(byte[] oneByte, int off, int len) throws IOException {
        try {
            this.teeOutputStream.write(oneByte, off, len);
        } catch (IOException ex) {
            listener.onError(ex);
            throw ex;
        }
    }

    @Override
    public void write(byte[] oneByte) throws IOException {
        try {
            this.teeOutputStream.write(oneByte);
        } catch (IOException ex) {
            listener.onError(ex);
            throw ex;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            this.teeOutputStream.flush();
        } catch (IOException ex) {
            listener.onError(ex);
            throw ex;
        }
    }

    @Override
    public void close() throws IOException {
        this.teeOutputStream.close();
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        listener = writeListener;
        try {
            writeListener.onWritePossible();
        } catch (IOException e) {
            listener.onError(e);
            e.printStackTrace();
        }
    }
}
