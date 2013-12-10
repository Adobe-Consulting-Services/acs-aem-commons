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
package com.adobe.acs.commons.i18n.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.ResourceBundle;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import com.adobe.acs.commons.i18n.ResourceBundleExporter;

public class JsonExporter implements ResourceBundleExporter {

    private static final String CONTENT_TYPE_JSON = "application/json";

    public void export(final ResourceBundle bundle,
            final SlingHttpServletResponse response)
            throws IOException {
        // write to string writer so we can determine correct content length
        final StringWriter out = new StringWriter();
        final JSONWriter json = new JSONWriter(out);
        try {
            json.object();
            if ( bundle != null) {
                Enumeration<String> keys = bundle.getKeys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    Object resource = bundle.getObject(key);
                    if (resource != null) {
                        json.key(key);
                        if (resource.getClass().isArray()) {
                            json.array();
                            Object[] arrResource = (Object[]) resource;
                            for (Object res : arrResource) {
                                json.value(String.valueOf(res));
                            }
                            json.endArray();
                        } else {
                            json.value(String.valueOf(resource));
                        }
                    }
                }
            }
            json.endObject();
        } catch (final JSONException je) {
            throw (IOException) new IOException("Cannot write JSON").initCause(je);
        }
        // get bytes and set response headers
        final byte[] data = out.toString().getBytes(UTF8);
        response.setContentType(CONTENT_TYPE_JSON);
        response.setCharacterEncoding(UTF8);
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

}
