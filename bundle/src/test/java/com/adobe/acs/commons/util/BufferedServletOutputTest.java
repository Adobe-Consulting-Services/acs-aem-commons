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
