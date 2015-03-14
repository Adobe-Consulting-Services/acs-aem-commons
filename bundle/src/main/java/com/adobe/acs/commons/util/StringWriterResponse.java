/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.util;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import aQute.bnd.annotation.ProviderType;

import java.io.PrintWriter;
import java.io.StringWriter;

@ProviderType
public final class StringWriterResponse extends SlingHttpServletResponseWrapper {
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