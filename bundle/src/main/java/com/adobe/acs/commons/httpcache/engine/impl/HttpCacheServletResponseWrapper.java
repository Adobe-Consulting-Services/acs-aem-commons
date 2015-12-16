package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.store.TempSink;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Wrapper for <code>SlingHttpServletResponse</code>. Wrapped to get hold of the copy of servlet response stream.
 */
public class HttpCacheServletResponseWrapper extends SlingHttpServletResponseWrapper {
    private static final Logger log = LoggerFactory.getLogger(HttpServletResponseWrapper.class);

    private PrintWriter printWriter;
    private ServletOutputStream servletOutputStream;
    private final TempSink tempSink;

    public HttpCacheServletResponseWrapper(SlingHttpServletResponse wrappedResponse, TempSink tempSink) throws
            IOException {
        super(wrappedResponse);
        this.tempSink = tempSink;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.printWriter != null) {
            throw new IllegalStateException("Cannot invoke getOutputStream() once getWriter() has been called.");
        } else if (this.servletOutputStream == null) {
            try {
                this.servletOutputStream = new TeeServletOutputStream(super.getOutputStream(), tempSink
                        .createOutputStream());
            } catch (HttpCacheDataStreamException e) {
                log.error("Temp sink is unable to provide an output stream.");
            }
        }

        return this.servletOutputStream;

    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.servletOutputStream != null) {
            throw new IllegalStateException("Cannot invoke getWriter() once getOutputStream() has been called.");
        } else if (this.printWriter == null) {
            try {
                this.printWriter = new TeePrintWriter(super.getWriter(), new PrintWriter(tempSink.createOutputStream
                        ()));
            } catch (HttpCacheDataStreamException e) {
                log.error("Temp sink is unable to provide an output stream.");
            }
        }

        return this.printWriter;
    }

    public TempSink getTempSink() {
        return tempSink;
    }
}
