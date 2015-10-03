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

package com.adobe.acs.commons.analysis.jcrchecksum;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.DefaultChecksumGeneratorOptions;
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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Utility that generates checksums for JCR paths.  The checksum is calculated using a depth first traversal
 * and calculates an aggregate checksum on the nodes with the specified node types (via {@link ChecksumGeneratorOptions}).
 */
@ProviderType
public final class ChecksumGenerator {
    private static final Logger log = LoggerFactory.getLogger(ChecksumGenerator.class);

    private ChecksumGenerator() {
        // Private cstor for static util
    }

    public static void generateChecksums(Session session, String path, PrintWriter out) throws RepositoryException {
        generateChecksums(session, path, new DefaultChecksumGeneratorOptions(), out);
        out.flush();
    }

    public static void generateChecksums(Session session, String path, ChecksumGeneratorOptions opts, PrintWriter out)
            throws RepositoryException {

        Node node = null;
        try {
            if (session.itemExists(path)) {
                Item item = session.getItem(path);
                if (item.isNode()) {
                    node = (Node) item;
                }
            }
        } catch (PathNotFoundException e) {
            log.warn("Path not found while generating checksums", e);
            return;
        } catch (RepositoryException e) {
            throw e;
        }

        traverseTree(node, opts, out);

        out.flush();
    }

    private static void traverseTree(Node node, ChecksumGeneratorOptions opts, PrintWriter out) {

        Set<String> nodeTypes = opts.getIncludedNodeTypes();
        Set<String> nodeTypeExcludes = opts.getExcludedNodeTypes();

        if (node != null) {
            String primaryNodeType;
            try {
                primaryNodeType = node.getPrimaryNodeType().getName();
                NodeIterator nIt;
                if (nodeTypes.contains(primaryNodeType) && !nodeTypeExcludes.contains(primaryNodeType)) {
                    generateChecksums(node, opts, out);
                } else {
                    nIt = node.getNodes();

                    while (nIt.hasNext()) {
                        primaryNodeType = node.getPrimaryNodeType().getName();
                        Node child = nIt.nextNode();

                        if (nodeTypes.contains(primaryNodeType)) {
                            generateChecksums(child, opts, out);
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

    private static String generateChecksums(Node node, ChecksumGeneratorOptions opts, PrintWriter out)
            throws RepositoryException {

        Set<String> nodeTypeIncludes = opts.getIncludedNodeTypes();
        Set<String> nodeTypeExcludes = opts.getExcludedNodeTypes();
        Set<String> propertyExcludes = opts.getExcludedProperties();
        Set<String> sortedProperties = opts.getSortedProperties();

        NodeIterator children = node.getNodes();
        StringBuilder checksums = new StringBuilder();
        String primaryNodeType = node.getPrimaryNodeType().getName();

        checksums.append(node.getName());
        SortedSet<String> childSortSet = new TreeSet<String>();
        boolean hasOrderedChildren = false;

        try {
            hasOrderedChildren = node.getPrimaryNodeType().hasOrderableChildNodes();
        } catch (UnsupportedOperationException e) {
            // This is an exception thrown in the test scenarios using the Mock JCR API
            // This would not happen using the actual JCR APIs
            // Allow other exceptions to be thrown and break processing normally
        }

        while (children.hasNext()) {
            Node child = children.nextNode();

            if (!nodeTypeExcludes.contains(child.getPrimaryNodeType().getName())) {
                if (hasOrderedChildren) {
                    checksums.append(child.getName()).append("=");
                    checksums.append(generateChecksums(child, opts, out));
                } else {
                    childSortSet.add(child.getName() + "=" + generateChecksums(child, opts, out));
                }
            }
        }

        if (!hasOrderedChildren) {
            for (String childChecksum : childSortSet) {
                checksums.append(childChecksum);
            }
        }

        SortedMap<String, String> props = new TreeMap<String, String>();
        PropertyIterator properties = node.getProperties();

        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            int type = property.getType();

            if (propertyExcludes.contains(property.getName())) {
                log.debug("Excluding property: {}", node.getPath() + "/@" + property.getName());
                continue;
            } else if (property.isMultiple()) {

                boolean isSorted = sortedProperties.contains(property.getName());
                Value[] values = property.getValues();
                StringBuffer sb = new StringBuffer();

                SortedSet<String> valSet = new TreeSet<String>();

                for (Value value : values) {
                    type = value.getType();

                    if (type == PropertyType.BINARY) {
                        try {
                            java.io.InputStream stream = value.getBinary().getStream();
                            String checksumOfBinary = DigestUtils.shaHex(stream);
                            stream.close();

                            if (isSorted) {
                                valSet.add(checksumOfBinary);
                            } else {
                                sb.append(checksumOfBinary);
                            }
                        } catch (IOException e) {
                            log.error("Error calculating hash for binary of {} : {}", property.getPath(), e.getMessage());
                        }
                    } else {
                        String checksumOfString = DigestUtils.shaHex(value.getString());
                        if (isSorted) {
                            valSet.add(checksumOfString);
                        } else {
                            sb.append(checksumOfString);
                        }
                    }
                }

                if (isSorted) {
                    for (String v : valSet) {
                        sb.append(v);
                    }
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("Multi-property: {} ~> {}", node.getPath() + "/@" + property.getName(), sb.toString());
                }

                props.put(property.getName(), sb.toString());

            } else if (type == PropertyType.BINARY) {
                try {
                    java.io.InputStream stream = property.getBinary().getStream();
                    String checksum = DigestUtils.shaHex(stream);
                    stream.close();
                    props.put(property.getName(), checksum);

                    log.debug("Binary property: {} ~> {}", node.getPath() + "/@" + property.getName(), checksum);
                } catch (IOException e) {
                    log.error("Error calculating hash for binary of {} : {}", property.getPath(), e.getMessage());
                }
            } else {
                String ckSum = DigestUtils.shaHex(property.getString());
                props.put(property.getName(), ckSum);
            }
        }

        
        for (String key : props.keySet()) {
            checksums.append(key).append("=").append(props.get(key));
        }

        log.debug("Collected checksums: {} ~> {}", node.getPath(), checksums);

        String sha = DigestUtils.shaHex(checksums.toString());
        
        if (nodeTypeIncludes.contains(primaryNodeType)) {
            log.debug("Node type of [ {} ] included as checksum-able node types {}", primaryNodeType, Arrays.asList(nodeTypeIncludes));
            out.print(node.getPath());
            out.print("\t");
            out.println(sha);
        }

        if (log.isDebugEnabled()) {
            log.debug("Final checksum: {} ~> {}", node.getPath(), sha);
        }
        return sha;
    }
}
