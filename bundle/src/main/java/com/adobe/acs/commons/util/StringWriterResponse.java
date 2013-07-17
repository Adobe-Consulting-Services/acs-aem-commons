package com.adobe.acs.commons.util;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StringWriterResponse extends SlingHttpServletResponseWrapper {
    private StringWriter stringWriter = new StringWriter();
    private PrintWriter printWriter = new PrintWriter(stringWriter);

    public StringWriterResponse(SlingHttpServletResponse slingHttpServletResponse) {
        super(slingHttpServletResponse);
    }

    public PrintWriter getWriter() {
        return printWriter;
    }

    public String getString() {
        return stringWriter.toString();
    }

    public void clearWriter() {
        printWriter.close();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }
}