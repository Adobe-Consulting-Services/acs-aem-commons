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

package com.adobe.acs.commons.analysis.jcrchecksum;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ProviderType;

/**
 * Utility that generates checksums for JCR paths.  The checksum is calculated using a depth first traversal
 * and calculates an aggregate checksum on the nodes with the specified node types (via {@link ChecksumGeneratorOptions}).
 * 
 */
@ProviderType
public class JSONGenerator {
    private static final Logger log = LoggerFactory
        .getLogger(ChecksumGenerator.class);
    
    public static void generateJSON(Session session, String path,
        JSONWriter out) throws RepositoryException, JSONException {
        Set<String> paths = new HashSet<String>();
        paths.add(path);
        generateJSON(session, paths, new ChecksumGeneratorOptions(), out);
    }

    public static void generateJSON(Session session, Iterable<String> paths,
        ChecksumGeneratorOptions opts, JSONWriter out)
        throws RepositoryException, JSONException {
        Node node = null;

        for(String path :paths) {
            out.object();
            try {
                if (session.itemExists(path)) {
                    Item item = session.getItem(path);
                    if (item.isNode()) {
                        node = (Node) item;
                    }
                }
                traverseTree(node, opts, out);
            } catch (PathNotFoundException e) {
                out.value("WARN: Path doesn't exist: " + path);
            } catch (RepositoryException e) {
                out.value("ERROR: Unable to read path: " + e.getMessage());
            } finally {
                out.endObject();
            }
        }
    }

    private static void traverseTree(Node node, ChecksumGeneratorOptions opts,
        JSONWriter out) throws JSONException {
        HashSet<String> nodeTypes = opts.getNodeTypeIncludes();
        HashSet<String> nodeTypeExcludes = opts.getNodeTypeExcludes();
        if (node != null) {
            String primaryNodeType;
            try {
                primaryNodeType = node.getPrimaryNodeType().getName();
                NodeIterator nIt;
                if (nodeTypes.contains(primaryNodeType)
                    && !nodeTypeExcludes.contains(primaryNodeType)) {
                    generateJSON(node, opts, out);
                } else {
                    nIt = node.getNodes();
                    while (nIt.hasNext()) {
                        primaryNodeType = node.getPrimaryNodeType().getName();
                        Node child = nIt.nextNode();
                        if (nodeTypes.contains(primaryNodeType)) {
                            generateJSON(child, opts, out);
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

    private static void generateJSON(Node node,
        ChecksumGeneratorOptions opts, JSONWriter out)
        throws RepositoryException, JSONException {

        out.key(node.getPath());
        out.object();
        out.key("foo");
        out.value("fi");
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
     */
    private static void outputProperties(Node node,
        ChecksumGeneratorOptions opts, JSONWriter out)
        throws RepositoryException, JSONException, ValueFormatException {
        HashSet<String> excludes = opts.getPropertyExcludes();
        HashSet<String> sortValues = opts.getSortedMultiValueProperties();
                
        SortedMap<String, Property> props = new TreeMap<String, Property>();
        PropertyIterator pi = node.getProperties();
        
        // sort the properties by name as the JCR makes no guarantees on property order
        while (pi.hasNext()) {
            Property p = pi.nextProperty();
            //skip the property if it is in the excludes list
            if (excludes.contains(p.getName())) {
                continue;
            } else {
                props.put(p.getName(), p);
            }
        }
        pi = null;
        
        for (Property p : props.values()) {
            int type = p.getType();

            if (p.isMultiple()) {
                out.key(p.getName());
                // create an array for multi value output
                out.array();
                boolean isSortedValues = sortValues.contains(p.getName());
                Value[] values = p.getValues();
                TreeMap<String, Value> sortedValueMap = new TreeMap<String, Value>();
                for (Value v : values) {
                    type = v.getType();
                    if (type == PropertyType.BINARY) {
                        if (isSortedValues) {
                            try {
                                java.io.InputStream stream =
                                    v.getBinary().getStream();
                                String ckSum = DigestUtils.shaHex(stream);
                                stream.close();
                                sortedValueMap.put(ckSum, v);
                            } catch (IOException e) {
                                sortedValueMap.put(
                                    "ERROR: generating hash for binary of " + p.getPath() + " : " + e.getMessage(), v);
                            }
                        } else {
                            outputPropertyValue(p, v, out);
                        }
                    } else {
                        String val = v.getString();
                        if (isSortedValues) {
                            sortedValueMap.put(val, v);
                        } else {
                            outputPropertyValue(p, v, out);
                        }
                    }
                }
                if (isSortedValues) {
                    for (Value v : sortedValueMap.values()) {
                        outputPropertyValue(p, v, out);
                    }
                }
                out.endArray();
                // end multi value property output
            } else {
                out.key(p.getName());
                outputPropertyValue(p, p.getValue(), out);
            }
        }
//        if (nodeTypes.contains(primaryNodeType)) {
//            out.print(node.getPath());
//            out.print("\t");
//            out.println(DigestUtils.shaHex(checkSums.toString()));
//            out.flush();
//        }
    }

    /**
     * @param node
     * @param opts
     * @param out
     * @throws RepositoryException
     * @throws JSONException
     */
    private static void outputChildNodes(Node node,
        ChecksumGeneratorOptions opts, JSONWriter out)
        throws RepositoryException, JSONException {
        HashSet<String> nodeTypeExcludes = opts.getNodeTypeExcludes();
        
        NodeIterator nIt;
        nIt = node.getNodes();

        TreeMap<String, Node> childSortMap = new TreeMap<String, Node>();
        boolean hasOrderedChildren = false;
        try {
            hasOrderedChildren = node.getPrimaryNodeType().hasOrderableChildNodes();
        } catch (Exception e) {
        }
        while (nIt.hasNext()) {
            Node child = nIt.nextNode();
            if (!nodeTypeExcludes
                .contains(child.getPrimaryNodeType().getName())) {
                if (hasOrderedChildren) {
                    //output child node if parent is has orderable children
                    out.key(child.getName());
                    out.object();
                    generateJSON(child, opts, out);
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
            out.key(child.getName());
            out.object();
            generateJSON(child, opts, out);
            out.endObject();
        }
        childSortMap = null;
    }

    private static void outputPropertyValue(Property p, Value v, JSONWriter out) throws ValueFormatException, RepositoryException, JSONException {
        
        if (v.getType() == PropertyType.STRING) {
            out.value(v.getString());
        } else if (v.getType() == PropertyType.BINARY) {
            try {
                java.io.InputStream stream = v.getBinary().getStream();
                String ckSum = DigestUtils.shaHex(stream);
                stream.close();
                out.value(ckSum);
            } catch (IOException e) {
                out.value("ERROR: calculating hash for binary of " + p.getPath() + " : " + e.getMessage());
            }
        } else if(v.getType() == PropertyType.BOOLEAN) {
            out.value(v.getBoolean());
        } else if(v.getType() == PropertyType.DATE) {
            Calendar cal = v.getDate();
            if(cal != null) {
                out.object();
                out.key("type");
                out.value(PropertyType.nameFromValue(v.getType()));
                out.key("val");
                out.value(cal.getTime().toString());
                out.endObject();
            }
        } else if(v.getType() == PropertyType.LONG) {
            out.value(v.getLong());
        } else if(v.getType() == PropertyType.DOUBLE) {
            out.value(v.getDouble());
        } else if(v.getType() == PropertyType.DECIMAL) {
            out.value(v.getDecimal());
        } else {
            out.object();
            out.key("type");
            out.value(PropertyType.nameFromValue(v.getType()));
            out.key("val");
            out.value(v.getString());
            out.endObject();
        }
    }
}
