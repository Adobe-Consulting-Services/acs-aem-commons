package com.adobe.acs.commons.util;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

public class BufferedServletOutputTest {
    
    // Test for https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues/2371
    @Test
    public void testClosingWithOutputStream() throws IOException {
        
        ServletOutputStream innerOut = Mockito.mock(ServletOutputStream.class);
        ServletResponse wrappedResponse = Mockito.mock(ServletResponse.class);
        Mockito.when(wrappedResponse.getOutputStream()).thenReturn(innerOut);
        
        BufferedServletOutput bso = new BufferedServletOutput(wrappedResponse);
        ServletOutputStream out = bso.getOutputStream();
        bso.flushBuffer();
        bso.close();
        
    }

}
