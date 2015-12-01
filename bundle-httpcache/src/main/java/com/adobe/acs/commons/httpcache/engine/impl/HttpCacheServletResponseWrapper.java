package com.adobe.acs.commons.httpcache.engine.impl;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import javax.servlet.ServletOutputStream;
import java.io.*;

/**
 * Wrapper for <code>SlingHttpServletResponse</code>. Wrapped to get hold of the copy of servlet response stream.
 */
public class HttpCacheServletResponseWrapper extends SlingHttpServletResponseWrapper {
    private final File tempCacheFile;
    private final FileOutputStream fileOutputStream;

    private PrintWriter printWriter = null;
    private ServletOutputStream servletOutputStream = null;

    public HttpCacheServletResponseWrapper(SlingHttpServletResponse wrappedResponse, File file) throws
            IOException {
        super(wrappedResponse);
        this.tempCacheFile = file;
        this.fileOutputStream = new FileOutputStream(file);

    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.printWriter != null) {
            throw new IllegalStateException("Cannot invoke getOutputStream() once getWriter() has been called.");
        } else if (this.servletOutputStream == null) {
            this.servletOutputStream = new TeeServletOutputStream(super.getOutputStream(), fileOutputStream);
        }

        return this.servletOutputStream;

    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.servletOutputStream != null) {
            throw new IllegalStateException("Cannot invoke getWriter() once getOutputStream() has been called.");
        } else if (this.printWriter == null) {
            this.printWriter = new TeePrintWriter(super.getWriter(), new PrintWriter(fileOutputStream));
        }

        return this.printWriter;
    }

    public File getTempCacheFile() {
        return this.tempCacheFile;
    }
}
