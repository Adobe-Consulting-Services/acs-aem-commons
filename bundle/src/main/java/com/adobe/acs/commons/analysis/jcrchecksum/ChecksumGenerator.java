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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Utility that generates checksums for JCR paths.  The checksum is calculated using a depth first traversal
 * and calculates an aggregate checksum on the nodes with the specified node types
 * (via {@link ChecksumGeneratorOptions}).
 */
@ProviderType
public final class ChecksumGenerator {
    private static final Logger log = LoggerFactory.getLogger(ChecksumGenerator.class);

    private ChecksumGenerator() {
        // Private cstor for static util
    }


    /**
     * Convenience method for  generateChecksum(session, path, new DefaultChecksumGeneratorOptions()).
     *
     * @param session the session
     * @param path tthe root path to generate checksums for
     * @return the map of abs path ~> checksums
     * @throws RepositoryException
     * @throws IOException
     */
    public static Map<String, String> generateChecksum(Session session, String path) throws RepositoryException,
            IOException {
        return generateChecksum(session, path, new DefaultChecksumGeneratorOptions());
    }

    /**
     * Traverses the content tree whose root is defined by the path param, respecting the {@link
     * ChecksumGeneratorOptions}.
     * Generates map of checksum hashes in the format [ ABSOLUTE PATH ] : [ CHECKSUM OF NODE SYSTEM ]
     *
     * @param session the session
     * @param path the root path to generate checksums for
     * @param options the {@link ChecksumGeneratorOptions} that define the checksum generation
     * @return the map of abs path ~> checksums
     * @throws RepositoryException
     * @throws IOException
     */
    public static Map<String, String> generateChecksum(Session session, String path, ChecksumGeneratorOptions options)
            throws RepositoryException, IOException {

        Node node = session.getNode(path);

        if (node == null) {
            log.warn("Path [ {} ] not found while generating checksums", path);
            return new LinkedHashMap<String, String>();
        }

        return traverseTree(node, options);
    }

    /**
     *
     * @param node
     * @param options
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    private static Map<String, String> traverseTree(Node node, ChecksumGeneratorOptions options) throws
            RepositoryException,
            IOException {

        final Map<String, String> checksums = new LinkedHashMap<String, String>();

        if (isChecksumable(node, options)) {

            // Tree-traversal has found a node to checksum (checksum will include all valid sub-tree nodes)

            checksums.putAll(generatedNodeChecksum(node, options));

        } else {

            // Traverse the tree for checksum-able node systems

            NodeIterator children = node.getNodes();

            while (children.hasNext()) {
                // Check each child with recursive logic; if child is checksum-able the call into traverseTree will
                // handle this case
                checksums.putAll(traverseTree(children.nextNode(), options));
            }
        }

        return checksums;
    }


    /**
     * Ensures the node's primary type is included in the Included Node Types and NOT in the Excluded Node Types.
     *
     * @param node    the candidate node
     * @param options the checksum options containing the included and excluded none types
     * @return true if the node represents a checksum-able node system
     * @throws RepositoryException
     */
    private static boolean isChecksumable(Node node, ChecksumGeneratorOptions options) throws RepositoryException {
        final Set<String> nodeTypeIncludes = options.getIncludedNodeTypes();
        final Set<String> nodeTypeExcludes = options.getExcludedNodeTypes();

        final String primaryNodeType = node.getPrimaryNodeType().getName();

        return nodeTypeIncludes.contains(primaryNodeType) && !nodeTypeExcludes.contains(primaryNodeType);
    }

    /**
     * Generates a checksum for a single node and its node sub-system, respecting the options.
     * @param node the node whose subsystem to create a checksum for
     * @param options the {@link ChecksumGeneratorOptions} options
     * @return a map containing 1 entry in the form [ node.getPath() ] : [ CHECKSUM OF NODE SYSTEM ]
     * @throws RepositoryException
     * @throws IOException
     */
    private static Map<String, String> generatedNodeChecksum(Node node, ChecksumGeneratorOptions options)
            throws RepositoryException, IOException {

        final Set<String> nodeTypeExcludes = options.getExcludedNodeTypes();

        final Map<String, String> checksums = new LinkedHashMap<String, String>();

        /* Create checksums for Node's properties */
        checksums.put(node.getPath(), aggregateChecksums(generatePropertyChecksums(node, options)));

        /* Then process node's children */

        final Map<String, String> sortedChecksums = new TreeMap<String, String>();
        final boolean hasOrderedChildren = hasOrderedChildren(node);
        final NodeIterator children = node.getNodes();

        while (children.hasNext()) {
            final Node child = children.nextNode();

            if (!nodeTypeExcludes.contains(child.getPrimaryNodeType().getName())) {
                if (hasOrderedChildren) {
                    // Use the order dictated by the JCR
                    checksums.putAll(generatedNodeChecksum(child, options));
                } else {
                    // If order is not dictated by JCR, collect so we can sort later
                    sortedChecksums.putAll(generatedNodeChecksum(child, options));
                }
            }
        }

        if (!hasOrderedChildren && sortedChecksums.size() > 0) {
            // Order is not dictated by JCR, so add the lexigraphically sorted entries to the checksums string
            checksums.putAll(sortedChecksums);
        }

        /* Compute aggregate of the node and its children's checksums */
        log.debug(toString(checksums));

        final Map<String, String> aggregateChecksum = new LinkedHashMap<String, String>();
        aggregateChecksum.put(node.getPath(), aggregateChecksums(checksums));
        return aggregateChecksum;
    }

    /**
     * Returns a lexicographically sorted map of the [PROPERTY PATH] : [CHECKSUM OF PROPERTIES].
     * @param node  the node to collect and checksum the properties for
     * @param options the checksum generator options
     * @return the map of the properties and their checksums
     * @throws RepositoryException
     */
    protected static SortedMap<String, String> generatePropertyChecksums(final Node node,
                                                                         final ChecksumGeneratorOptions options)

            throws RepositoryException, IOException {

        SortedMap<String, String> propertyChecksums = new TreeMap<String, String>();
        PropertyIterator properties = node.getProperties();

        while (properties.hasNext()) {
            final Property property = properties.nextProperty();

            if (options.getExcludedProperties().contains(property.getName())) {
                // Skip this property as it is excluded
                log.debug("Excluding property: {}", node.getPath() + "/@" + property.getName());
                continue;
            }

            /* Accept the property for checksuming */

            final List<String> checksums = new ArrayList<String>();

            final List<Value> values = getPropertyValues(property);

            for (final Value value : values) {
                if (value.getType() == PropertyType.BINARY) {
                    checksums.add(getBinaryChecksum(value));
                } else {
                    checksums.add(getStringChecksum(value));
                }
            }

            if (!options.getSortedProperties().contains(property.getName())) {
                Collections.sort(checksums);
            }

            if (log.isDebugEnabled()) {
                log.debug("Property: {} ~> {}",
                        node.getPath() + "/@" + property.getName(),
                        StringUtils.join(checksums, ","));
            }

            propertyChecksums.put(property.getPath(), StringUtils.join(checksums, ","));
        }

        return propertyChecksums;
    }

    /**
     * Normalizes a property values to a list; allows single and multi-values to be treated the same in code.
     * @param property the propert to get the value(s) from
     * @return a list of the property's value(s)
     * @throws RepositoryException
     */
    private static List<Value> getPropertyValues(final Property property) throws RepositoryException {
        final List<Value> values = new ArrayList<Value>();

        if (property.isMultiple()) {
            values.addAll(Arrays.asList(property.getValues()));
        } else {
            values.add(property.getValue());
        }

        return values;
    }

    /**
     * Gets the checksum for a Binary value.
     * @param value the Value
     * @return the checksum
     * @throws RepositoryException
     * @throws IOException
     */
    protected static String getBinaryChecksum(final Value value) throws RepositoryException, IOException {
        InputStream stream = null;

        try {
            stream = value.getBinary().getStream();
            return DigestUtils.shaHex(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Gets the checksum for a String value.
     * @param value the Value
     * @return the checksum
     * @throws RepositoryException
     */
    protected static String getStringChecksum(final Value value) throws RepositoryException {
        return DigestUtils.shaHex(value.getString());
    }

    /**
     * Checks if node has ordered children.
     * @param node the node
     * @return true if the node has ordered children
     * @throws RepositoryException
     */
    protected static boolean hasOrderedChildren(final Node node) throws RepositoryException {
        boolean hasOrderedChildren = false;

        try {
            hasOrderedChildren = node.getPrimaryNodeType().hasOrderableChildNodes();
        } catch (UnsupportedOperationException e) {
            // This is an exception thrown in the test scenarios using the Mock JCR API
            // This would not happen using the actual JCR APIs
            // Allow other exceptions to be thrown and break processing normally
        }

        return hasOrderedChildren;
    }

    /**
     * Aggregates a set of checksum entries into a single checksum value.
     * @param checksums the checksums
     * @return the checksum value
     */
    protected static String aggregateChecksums(final Map<String, String> checksums) {
        StringBuilder checksum = new StringBuilder();

        for (Map.Entry<String, String> entry : checksums.entrySet()) {
            checksum.append(entry.getKey() + "=" + entry.getValue());
        }

        return DigestUtils.shaHex(checksum.toString());
    }

    /**
     * Helper method for debugging output.
     * @param map a map
     * @return a String representation of the map.
     */
    private static String toString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey() + " = " + entry.getValue() + "\n");
        }

        return sb.toString();
    }
}