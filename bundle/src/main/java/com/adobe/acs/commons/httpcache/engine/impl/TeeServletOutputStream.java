package com.adobe.acs.commons.httpcache.engine.impl;

import org.apache.commons.io.output.TeeOutputStream;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes to 2 outputstream. Required to take copy of the servlet response.
 */
public class TeeServletOutputStream extends ServletOutputStream {
    private final TeeOutputStream teeOutputStream;

    public TeeServletOutputStream(OutputStream one, OutputStream two) {
        // Uses Apache IO TeeOutputStream
        teeOutputStream = new TeeOutputStream(one, two);
    }

    @Override
    public void write(int arg0) throws IOException {
        this.teeOutputStream.write(arg0);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        this.teeOutputStream.write(b, off, len);
    }

    @Override
    public void write(byte b[]) throws IOException {
        this.teeOutputStream.write(b);
    }

    @Override
    public void flush() throws IOException {
        this.teeOutputStream.flush();
    }

    @Override
    public void close() throws IOException {
        this.teeOutputStream.close();
    }
}
