/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.sling.api.SlingHttpServletResponse;

public class BufferedSlingHttpServletResponse extends BufferedHttpServletResponse implements SlingHttpServletResponse {

    private final SlingHttpServletResponse wrappedResponse;

    public BufferedSlingHttpServletResponse(SlingHttpServletResponse wrappedResponse) throws IOException {
        super(wrappedResponse);
        this.wrappedResponse = wrappedResponse;
    }
    
    public BufferedSlingHttpServletResponse(SlingHttpServletResponse wrappedResponse, StringWriter writer, ByteArrayOutputStream outputStream)
            throws IOException {
        super(wrappedResponse, writer, outputStream);
        this.wrappedResponse = wrappedResponse;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return wrappedResponse.adaptTo(type);
    }

}
