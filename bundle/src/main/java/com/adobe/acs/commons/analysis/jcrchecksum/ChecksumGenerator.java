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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Utility that generates checksums for JCR paths.  The checksum is calculated using a depth first traversal
 * and calculates an aggregate checksum on the nodes with the specified node types (via {@link ChecksumGeneratorOptions}).
 * 
 */
@ProviderType
public class ChecksumGenerator {
    private static final Logger log = LoggerFactory
        .getLogger(ChecksumGenerator.class);

    public static void generateChecksums(Session session, String path,
        PrintWriter out) throws RepositoryException {
        generateChecksums(session, path, new DefaultChecksumGeneratorOptions(), out);
    }

    public static void generateChecksums(Session session, String path,
        ChecksumGeneratorOptions opts, PrintWriter out)
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
            return;
        } catch (RepositoryException e) {
            throw e;
        }

        traverseTree(node, opts, out);
    }

    private static void traverseTree(Node node, ChecksumGeneratorOptions opts,
        PrintWriter out) {
        Set<String> nodeTypes = opts.getIncludedNodeTypes();
        Set<String> nodeTypeExcludes = opts.getExcludedNodeTypes();

        if (node != null) {
            String primaryNodeType;
            try {
                primaryNodeType = node.getPrimaryNodeType().getName();
                NodeIterator nIt;
                if (nodeTypes.contains(primaryNodeType)
                    && !nodeTypeExcludes.contains(primaryNodeType)) {
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

    private static String generateChecksums(Node node,
        ChecksumGeneratorOptions opts, PrintWriter out)
        throws RepositoryException {

        Set<String> nodeTypes = opts.getIncludedNodeTypes();
        Set<String> nodeTypeExcludes = opts.getExcludedNodeTypes();
        Set<String> excludes = opts.getExcludedProperties();
        Set<String> sortValues = opts.getSortedProperties();

        NodeIterator nIt;
        nIt = node.getNodes();
        StringBuilder checkSums = new StringBuilder();
        String primaryNodeType;
        primaryNodeType = node.getPrimaryNodeType().getName();

        checkSums.append(node.getName());
        SortedSet<String> childSortSet = new TreeSet<String>();
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
                    checkSums.append(child.getName()).append("=");
                    checkSums.append(generateChecksums(child, opts, out));
                    // out.append("child: " +
                    // child.getName()).append("=").append(generateChecksums(child,
                    // nodeTypes, excludes, out)).append("\n");
                } else {
                    childSortSet.add(child.getName() + "="
                        + generateChecksums(child, opts, out));
                }
            }
        }
        if (!hasOrderedChildren) {
            for (String childChksum : childSortSet) {
                checkSums.append(childChksum);
            }
        }

        SortedMap<String, String> props = new TreeMap<String, String>();
        PropertyIterator pi = node.getProperties();
        while (pi.hasNext()) {
            Property p = pi.nextProperty();
            int type = p.getType();
            if (excludes.contains(p.getName())) {
                continue;
            } else if (p.isMultiple()) {
                boolean isSortedValues = sortValues.contains(p.getName());
                Value[] values = p.getValues();
                StringBuffer sb = new StringBuffer();

                SortedSet<String> valSet = new TreeSet<String>();
                for (Value v : values) {
                    type = v.getType();
                    if (type == PropertyType.BINARY) {
                        try {
                            java.io.InputStream stream =
                                v.getBinary().getStream();
                            String ckSum = DigestUtils.shaHex(stream);
                            stream.close();
                            // checkSums.append(p.getName()).append(ckSum);
                            if (isSortedValues) {
                                valSet.add(ckSum);
                            } else {
                                sb.append(ckSum);
                            }
                        } catch (IOException e) {
                            log.error(
                                "Error calculating hash for binary of {} : {}",
                                p.getPath(), e.getMessage());
                        }
                    } else {
                        String ckSum = DigestUtils.shaHex(v.getString());
                        // checkSums.append(p.getName()).append("=").append(ckSum);
                        if (isSortedValues) {
                            valSet.add(ckSum);
                        } else {
                            sb.append(ckSum);
                        }
                    }
                }
                if (isSortedValues) {
                    for (String v : valSet) {
                        sb.append(v);
                    }
                }
                props.put(p.getName(), sb.toString());
            } else if (type == PropertyType.BINARY) {
                try {
                    java.io.InputStream stream = p.getBinary().getStream();
                    String ckSum = DigestUtils.shaHex(stream);
                    stream.close();
                    // checkSums.append(p.getName()).append("=").append(ckSum);
                    props.put(p.getName(), ckSum);
                } catch (IOException e) {
                    log.error("Error calculating hash for binary of {} : {}",
                        p.getPath(), e.getMessage());
                }
            } else {
                String ckSum = DigestUtils.shaHex(p.getString());
                // checkSums.append(p.getName()).append("=").append(ckSum);
                props.put(p.getName(), ckSum);
            }
        }
        for (String key : props.keySet()) {
            checkSums.append(key).append("=").append(props.get(key));
            // out.append(key).append("=").append(props.get(key)).append("\n");
        }
        if (nodeTypes.contains(primaryNodeType)) {
            out.print(node.getPath());
            out.print("\t");
            out.println(DigestUtils.shaHex(checkSums.toString()));
            out.flush();
        }
        props = null;

        return DigestUtils.shaHex(checkSums.toString());
    }
}
