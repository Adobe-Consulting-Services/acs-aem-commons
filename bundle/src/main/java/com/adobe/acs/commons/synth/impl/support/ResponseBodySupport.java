package com.adobe.acs.commons.synth.impl.support;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.*;
import java.nio.charset.StandardCharsets;


class ResponseBodySupport {
    
    private static final Logger LOG = LoggerFactory.getLogger(ResponseBodySupport.class);
    
    private ByteArrayOutputStream outputStream;
    private ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;
    
    public ResponseBodySupport() {
        this.reset();
    }
    
    public final void reset() {
        this.outputStream = new ByteArrayOutputStream();
        this.servletOutputStream = null;
        this.printWriter = null;
    }
    
    public ServletOutputStream getOutputStream() {
        if (this.servletOutputStream == null) {
            this.servletOutputStream = new ServletOutputStream() {
                public void write(int b) {
                    ResponseBodySupport.this.outputStream.write(b);
                }
                
                public boolean isReady() {
                    return true;
                }
                
                public void setWriteListener(WriteListener writeListener) {
                    throw new UnsupportedOperationException();
                }
            };
        }
        
        return this.servletOutputStream;
    }
    
    public PrintWriter getWriter(String charset) {
        if (this.printWriter == null) {
            try {
                this.printWriter = new PrintWriter(new OutputStreamWriter(this.getOutputStream(), this.defaultCharset(charset)));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Unsupported encoding: " + this.defaultCharset(charset), e);
            }
        }
        
        return this.printWriter;
    }
    
    public byte[] getOutput() {
        if (this.printWriter != null) {
            this.printWriter.flush();
        }
        
        if (this.servletOutputStream != null) {
            try {
                this.servletOutputStream.flush();
            } catch (IOException e) {
                LOG.warn("Error flushing output stream", e);
            }
        }
        
        return this.outputStream.toByteArray();
    }
    
    public String getOutputAsString(String charset) {
        try {
            return new String(this.getOutput(), this.defaultCharset(charset));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unsupported encoding: " + this.defaultCharset(charset), e);
        }
    }
    
    private String defaultCharset(String charset) {
        return StringUtils.defaultString(charset, StandardCharsets.UTF_8.name());
    }
}
