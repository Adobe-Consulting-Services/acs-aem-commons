/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum.impl;

import org.osgi.annotation.versioning.ProviderType;
import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.DefaultChecksumGeneratorOptions;
import com.google.gson.stream.JsonWriter;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Utility that generates checksums for JCR paths.
 * The checksum is calculated using a depth first traversal
 * and calculates an aggregate checksum on the nodes with the specified node types
 * (via {@link ChecksumGeneratorOptions}).
 */
@ProviderType
@SuppressWarnings("squid:S2070") // SHA1 not used cryptographically
public final class JSONGenerator {
    private static final Logger log = LoggerFactory.getLogger(ChecksumGenerator.class);

    private JSONGenerator() {
        // Private cstor for static util
    }

    public static void generateJSON(Session session, String path,
                                    JsonWriter out) throws RepositoryException, IOException {
        Set<String> paths = new HashSet<>();
        paths.add(path);
        generateJSON(session, paths, new DefaultChecksumGeneratorOptions(), out);
    }

    public static void generateJSON(Session session, Set<String> paths,
                                    ChecksumGeneratorOptions opts, JsonWriter out)
            throws RepositoryException, IOException {
        Node node = null;

        if (paths.size() > 1) {
            out.beginArray();
        }

        for (String path : paths) {
            out.beginObject();
            try {
                if (session.itemExists(path)) {
                    Item item = session.getItem(path);
                    if (item.isNode()) {
                        node = (Node) item;
                    }
                }
                traverseTree(node, opts, out);
            } catch (PathNotFoundException e) {
                out.name("ERROR");
                out.value("WARN: Path doesn't exist: " + path);
            } catch (RepositoryException e) {
                out.name("ERROR");
                out.value("Unable to read path: " + e.getMessage());
            } finally {
                out.endObject();
            }
        }

        if (paths.size() > 1) {
            out.endArray();
        }
    }

    private static void traverseTree(Node node, ChecksumGeneratorOptions opts,
                                     JsonWriter out) throws IOException {
        Set<String> nodeTypes = opts.getIncludedNodeTypes();
        Set<String> nodeTypeExcludes = opts.getExcludedNodeTypes();
        if (node != null) {
            String primaryNodeType;
            try {
                primaryNodeType = node.getPrimaryNodeType().getName();
                if (nodeTypes.contains(primaryNodeType)
                        && !nodeTypeExcludes.contains(primaryNodeType)) {
                    generateNodeJSON(node, opts, out);
                } else {
                    NodeIterator nodeIterator = node.getNodes();
                    while (nodeIterator.hasNext()) {
                        primaryNodeType = node.getPrimaryNodeType().getName();
                        Node child = nodeIterator.nextNode();
                        if (nodeTypes.contains(primaryNodeType)) {
                            generateNodeJSON(child, opts, out);
                        } else {
                            traverseTree(child, opts, out);
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error while traversing tree {}", e.getMessage());
            }
        }
    }

    private static void generateNodeJSON(Node node,
                                         ChecksumGeneratorOptions opts, JsonWriter out)
            throws RepositoryException, IOException {
        out.name(node.getPath());
        out.beginObject();

        outputProperties(node, opts, out);

        outputChildNodes(node, opts, out);

        out.endObject();
    }

    private static void generateSubnodeJSON(Node node,
                                            ChecksumGeneratorOptions opts, JsonWriter out)
            throws RepositoryException, IOException {

        out.name(node.getName());
        out.beginObject();

        outputProperties(node, opts, out);

        outputChildNodes(node, opts, out);

        out.endObject();

    }

    /**
     * @param node
     * @param opts
     * @param out
     * @throws RepositoryException
     * @throws JSONException
     * @throws ValueFormatException
     * @throws IOException 
     */
    private static void outputProperties(Node node,
                                         ChecksumGeneratorOptions opts, JsonWriter out)
            throws RepositoryException, ValueFormatException, IOException {
        Set<String> excludes = opts.getExcludedProperties();

        SortedMap<String, Property> props = new TreeMap<>();
        PropertyIterator propertyIterator = node.getProperties();

        // sort the properties by name as the JCR makes no guarantees on property order
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.nextProperty();
            //skip the property if it is in the excludes list
            if (excludes.contains(property.getName())) {
                continue;
            } else {
                props.put(property.getName(), property);
            }
        }

        for (Property property : props.values()) {
            outputProperty(property, opts, out);
        }
    }

    @SuppressWarnings("squid:S3776")
    private static void outputProperty(Property property, ChecksumGeneratorOptions opts, JsonWriter out)
            throws RepositoryException, IOException {
        Set<String> sortValues = opts.getSortedProperties();
        if (property.isMultiple()) {
            out.name(property.getName());
            // create an array for multi value output
            out.beginArray();
            boolean isSortedValues = sortValues.contains(property.getName());
            Value[] values = property.getValues();
            TreeMap<String, Value> sortedValueMap = new TreeMap<>();
            for (Value v : values) {
                int type = v.getType();
                if (type == PropertyType.BINARY) {
                    if (isSortedValues) {
                        try {
                            java.io.InputStream stream =
                                    v.getBinary().getStream();
                            String ckSum = DigestUtils.sha1Hex(stream);
                            stream.close();
                            sortedValueMap.put(ckSum, v);
                        } catch (IOException e) {
                            sortedValueMap.put("ERROR: generating hash for binary of "
                                    + property.getPath() + " : " + e.getMessage(), v);
                        }
                    } else {
                        outputPropertyValue(property, v, out);
                    }
                } else {
                    String val = v.getString();
                    if (isSortedValues) {
                        sortedValueMap.put(val, v);
                    } else {
                        outputPropertyValue(property, v, out);
                    }
                }
            }
            if (isSortedValues) {
                for (Value v : sortedValueMap.values()) {
                    outputPropertyValue(property, v, out);
                }
            }
            out.endArray();
            // end multi value property output
        } else {
            out.name(property.getName());
            outputPropertyValue(property, property.getValue(), out);
        }
    }

    /**
     * @param node
     * @param opts
     * @param out
     * @throws RepositoryException
     * @throws IOException 
     * @throws JSONException
     */
    private static void outputChildNodes(Node node, ChecksumGeneratorOptions opts, JsonWriter out)
            throws RepositoryException, IOException {
        Set<String> nodeTypeExcludes = opts.getExcludedNodeTypes();

        NodeIterator nodeIterator = node.getNodes();

        TreeMap<String, Node> childSortMap = new TreeMap<>();
        boolean hasOrderedChildren = false;
        try {
            hasOrderedChildren = node.getPrimaryNodeType().hasOrderableChildNodes();
        } catch (Exception expected) {
            // ignore
        }
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            if (!nodeTypeExcludes
                    .contains(child.getPrimaryNodeType().getName())) {
                if (hasOrderedChildren) {
                    //output child node if parent is has orderable children
                    out.name(child.getName());
                    out.beginObject();
                    generateSubnodeJSON(child, opts, out);
                    out.endObject();
                } else {
                    // otherwise put the child nodes into a sorted map
                    // to output them with consistent ordering
                    childSortMap.put(child.getName(), child);
                }
            }
        }
        // output the non-ordered child nodes in sorted order (lexicographically)
        for (Node child : childSortMap.values()) {
            out.name(child.getName());
            out.beginObject();
            generateSubnodeJSON(child, opts, out);
            out.endObject();
        }
    }

    private static void outputPropertyValue(Property property, Value value, JsonWriter out)
            throws RepositoryException, IOException {

        if (value.getType() == PropertyType.STRING) {
            out.value(value.getString());
        } else if (value.getType() == PropertyType.BINARY) {
            try {
                java.io.InputStream stream = value.getBinary().getStream();
                String ckSum = DigestUtils.sha1Hex(stream);
                stream.close();
                out.value(ckSum);
            } catch (IOException e) {
                out.value("ERROR: calculating hash for binary of " + property.getPath() + " : " + e.getMessage());
            }
        } else if (value.getType() == PropertyType.BOOLEAN) {
            out.value(value.getBoolean());
        } else if (value.getType() == PropertyType.DATE) {
            Calendar cal = value.getDate();
            if (cal != null) {
                out.beginObject();
                out.name("type");
                out.value(PropertyType.nameFromValue(value.getType()));
                out.name("val");
                out.value(cal.getTime().toString());
                out.endObject();
            }
        } else if (value.getType() == PropertyType.LONG) {
            out.value(value.getLong());
        } else if (value.getType() == PropertyType.DOUBLE) {
            out.value(value.getDouble());
        } else if (value.getType() == PropertyType.DECIMAL) {
            out.value(value.getDecimal());
        } else {
            out.beginObject();
            out.name("type");
            out.value(PropertyType.nameFromValue(value.getType()));
            out.name("val");
            out.value(value.getString());
            out.endObject();
        }
    }
}
