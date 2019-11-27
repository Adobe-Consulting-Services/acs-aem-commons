/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Type adapter to convert JCR Nodes to JSON Objects (replacement for deprecated
 * NodeItemWriter.)
 */
public class JcrJsonAdapter extends TypeAdapter<Node> {

    @Override
    public void write(JsonWriter writer, Node t) throws IOException {
        if (t != null) {
            try {
                writer.beginObject();
                for (PropertyIterator pi = t.getProperties(); pi.hasNext();) {
                    Property p = (Property) pi.next();
                    writer.name(p.getName());
                    if (p.isMultiple()) {
                        writer.beginArray();
                        for (Value v : p.getValues()) {
                            writeValue(writer, v);
                        }
                    } else {
                        writeValue(writer, p.getValue());
                    }
                }
                for (NodeIterator ni = t.getNodes(); ni.hasNext();) {
                    Node child = ni.nextNode();
                    writer.name(child.getName());
                    write(writer, child);
                }
                writer.endObject();
            } catch (RepositoryException ex) {
                throw new IOException(ex);
            }
        }
    }

    private void writeValue(JsonWriter writer, Value v) throws IOException, RepositoryException {
        switch (v.getType()) {
            case PropertyType.BINARY:
                writer.value("(binary value)");
                break;
            case PropertyType.BOOLEAN:
                writer.value(v.getBoolean());
                break;
            case PropertyType.DATE:
            case PropertyType.LONG:
                writer.value(v.getLong());
                break;
            case PropertyType.DECIMAL:
            case PropertyType.DOUBLE:
                writer.value(v.getDecimal().toPlainString());
                break;
            default:
//                        case PropertyType.PATH:
//                        case PropertyType.STRING:
//                        case PropertyType.URI:
                writer.value(v.getString());
        }
    }

    @Override
    public Node read(JsonReader reader) throws IOException {
        throw new UnsupportedOperationException("JcrJsonAdaper.read(JsonReader) is not supported yet.");
    }

}