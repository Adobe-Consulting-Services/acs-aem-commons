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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class BufferingResponse extends HttpServletResponseWrapper {
    private StringWriter stringWriter;

    public BufferingResponse(final HttpServletResponse response) {
        super(response);
    }

    @Override
    public void resetBuffer() {
        if (this.stringWriter != null) {
            this.stringWriter = new StringWriter();
        }
        super.resetBuffer();
    }

    public PrintWriter getWriter() throws IOException {
        if (stringWriter == null) {
            stringWriter = new StringWriter();
        }
        return new PrintWriter(stringWriter);
    }

    public String getContents() {
        if (this.stringWriter != null) {
            return this.stringWriter.toString();
        }
        return null;
    }
}