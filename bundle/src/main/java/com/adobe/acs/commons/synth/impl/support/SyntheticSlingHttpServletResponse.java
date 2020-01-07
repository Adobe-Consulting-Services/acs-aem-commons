package com.adobe.acs.commons.synth.impl.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.osgi.annotation.versioning.ConsumerType;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
SyntheticSlingHttpServletResponse - captures the response to be read out to string. Not exposed as API but just used by the executor:
@see com.adobe.acs.commons.synth.SyntheticSlingHttpRequestExecutor
 */
@ConsumerType
public class SyntheticSlingHttpServletResponse extends SlingAdaptable implements SlingHttpServletResponse {
    
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    private String contentType;
    private String characterEncoding;
    private int contentLength;
    private int status = SlingHttpServletResponse.SC_OK;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private boolean isCommitted;
    private Locale locale;
    private final HeaderSupport headerSupport;
    private final ResponseBodySupport bodySupport;
    private final CookieSupport cookieSupport;
    
    public SyntheticSlingHttpServletResponse() {
        this.locale = Locale.US;
        this.headerSupport = new HeaderSupport();
        this.bodySupport = new ResponseBodySupport();
        this.cookieSupport = new CookieSupport();
    }
    
    public String getContentType() {
        if (this.contentType != null) {
            return this.contentType + (StringUtils.isNotBlank(this.characterEncoding) ? (";charset=" + this.characterEncoding) : "");
        }
        return null;
    }
    
    public void setContentType(String type) {
        this.contentType = type;
        if (StringUtils.contains(this.contentType, ";charset=")) {
            this.characterEncoding = StringUtils.substringAfter(this.contentType, ";charset=");
            this.contentType = StringUtils.substringBefore(this.contentType, ";charset=");
        }
        
    }
    
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }
    
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }
    
    public void setContentLength(int len) {
        this.contentLength = len;
    }
    
    public int getContentLength() {
        return this.contentLength;
    }
    
    public void setStatus(int sc, String sm) {
        this.setStatus(sc);
    }
    
    public void setStatus(int sc) {
        this.status = sc;
    }
    
    public int getStatus() {
        return this.status;
    }
    
    public void sendError(int sc, String msg) {
        this.setStatus(sc);
    }
    
    public void sendError(int sc) {
        this.setStatus(sc);
    }
    
    public void sendRedirect(String location) {
        this.setStatus(SlingHttpServletResponse.SC_FOUND);
        this.setHeader("Location", location);
    }
    
    public void addHeader(String name, String value) {
        this.headerSupport.addHeader(name, value);
    }
    
    public void addIntHeader(String name, int value) {
        this.headerSupport.addIntHeader(name, value);
    }
    
    public void addDateHeader(String name, long date) {
        this.headerSupport.addDateHeader(name, date);
    }
    
    public void setHeader(String name, String value) {
        this.headerSupport.setHeader(name, value);
    }
    
    public void setIntHeader(String name, int value) {
        this.headerSupport.setIntHeader(name, value);
    }
    
    public void setDateHeader(String name, long date) {
        this.headerSupport.setDateHeader(name, date);
    }
    
    public boolean containsHeader(String name) {
        return this.headerSupport.containsHeader(name);
    }
    
    public String getHeader(String name) {
        return this.headerSupport.getHeader(name);
    }
    
    public Collection<String> getHeaders(String name) {
        return this.headerSupport.getHeaders(name);
    }
    
    public Collection<String> getHeaderNames() {
        return this.headerSupport.getHeaderNames();
    }
    
    public PrintWriter getWriter() {
        return this.bodySupport.getWriter(this.getCharacterEncoding());
    }
    
    public ServletOutputStream getOutputStream() {
        return this.bodySupport.getOutputStream();
    }
    
    public void reset() {
        if (this.isCommitted()) {
            throw new IllegalStateException("Response already committed.");
        } else {
            this.bodySupport.reset();
            this.headerSupport.reset();
            this.cookieSupport.reset();
            this.status = SlingHttpServletResponse.SC_OK;
            this.contentLength = 0;
        }
    }
    
    public void resetBuffer() {
        if (this.isCommitted()) {
            throw new IllegalStateException("Response already committed.");
        } else {
            this.bodySupport.reset();
        }
    }
    
    public int getBufferSize() {
        return this.bufferSize;
    }
    
    public void setBufferSize(int size) {
        this.bufferSize = size;
    }
    
    public void flushBuffer() {
        this.isCommitted = true;
    }
    
    public boolean isCommitted() {
        return this.isCommitted;
    }
    
    public byte[] getOutput() {
        return this.bodySupport.getOutput();
    }
    
    public String getOutputAsString() {
        return this.bodySupport.getOutputAsString(this.getCharacterEncoding());
    }
    
    public void addCookie(Cookie cookie) {
        this.cookieSupport.addCookie(cookie);
    }
    
    public Cookie getCookie(String name) {
        return this.cookieSupport.getCookie(name);
    }
    
    public Cookie[] getCookies() {
        return this.cookieSupport.getCookies();
    }
    
    public Locale getLocale() {
        return this.locale;
    }
    
    public void setLocale(Locale loc) {
        this.locale = loc;
    }
    
    public String encodeRedirectUrl(String url) {
        throw new UnsupportedOperationException();
    }
    
    public String encodeRedirectURL(String url) {
        throw new UnsupportedOperationException();
    }
    
    public String encodeUrl(String url) {
        throw new UnsupportedOperationException();
    }
    
    public String encodeURL(String url) {
        throw new UnsupportedOperationException();
    }
    
    public void setContentLengthLong(long len) {
        throw new UnsupportedOperationException();
    }
}
