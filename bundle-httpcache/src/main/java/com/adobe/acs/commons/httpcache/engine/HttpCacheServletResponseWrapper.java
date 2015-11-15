package com.adobe.acs.commons.httpcache.engine;

import org.apache.commons.io.output.TeeOutputStream;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import javax.servlet.ServletOutputStream;
import java.io.*;

/**
 * Wrapper for <code>SlingHttpServletResponse</code>. Wrapped to get hold of the copy of servlet response stream.
 */
public class HttpCacheServletResponseWrapper extends SlingHttpServletResponseWrapper {
    private final File tempCacheFile;
    private final PrintStream printStream;

    public HttpCacheServletResponseWrapper(SlingHttpServletResponse wrappedResponse, File file) throws
            FileNotFoundException {
        super(wrappedResponse);
        this.tempCacheFile = file;
        this.printStream = new PrintStream(file);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new TeeServletOutputStream(super.getOutputStream(), printStream);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(new TeeServletOutputStream(super.getOutputStream(), printStream));
    }

    public File getTempCacheFile(){
        return this.tempCacheFile;
    }
}
