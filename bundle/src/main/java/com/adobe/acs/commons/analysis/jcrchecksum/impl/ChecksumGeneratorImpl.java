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

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.DefaultChecksumGeneratorOptions;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.util.Text;
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
@Component
@Service
@SuppressWarnings("squid:S2070") // SHA1 not used cryptographically
public class ChecksumGeneratorImpl implements ChecksumGenerator {
    private static final Logger log = LoggerFactory.getLogger(ChecksumGeneratorImpl.class);

    /**
     * Convenience method for  generateChecksums(session, path, new DefaultChecksumGeneratorOptions()).
     *
     * @param session the session
     * @param path tthe root path to generate checksums for
     * @return the map of abs path ~> checksums
     * @throws RepositoryException
     * @throws IOException
     */
    public Map<String, String> generateChecksums(Session session, String path) throws RepositoryException,
            IOException {
        return generateChecksums(session, path, new DefaultChecksumGeneratorOptions());
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
    public Map<String, String> generateChecksums(Session session, String path, ChecksumGeneratorOptions options)
            throws RepositoryException, IOException {

        Node node = session.getNode(path);

        if (node == null) {
            log.warn("Path [ {} ] not found while generating checksums", path);
            return new LinkedHashMap<>();
        }

        return traverseTree(node, options);
    }

    /**
     * Traverse the tree for candidate aggregate nodes.
     * @param node the current node being traversed
     * @param options the checksum generator options
     * @return a map of paths and checksums
     * @throws RepositoryException
     * @throws IOException
     */
    private Map<String, String> traverseTree(Node node, ChecksumGeneratorOptions options) throws
            RepositoryException,
            IOException {

        final Map<String, String> checksums = new LinkedHashMap<>();

        if (isExcludedSubTree(node, options)) {
            return checksums;
        } else if (isChecksumable(node, options) && !isExcludedNodeName(node, options)) {
            // Tree-traversal has found a node to checksum (checksum will include all valid sub-tree nodes)
            final String checksum = generatedNodeChecksum(node.getPath(), node, options);
            if (checksum != null) {
                checksums.put(node.getPath(), checksum);
                log.debug("Top Level Node: {} ~> {}", node.getPath(), checksum);
            }
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
     * Ensures the node's primary type is included in the Included Node Types and NOT in the Excluded Node Types and NOT in the Excluded Node Names.
     *
     * @param node    the candidate node
     * @param options the checksum options containing the included and excluded none types
     * @return true if the node represents a checksum-able node system
     * @throws RepositoryException
     */
    private boolean isChecksumable(Node node, ChecksumGeneratorOptions options) throws RepositoryException {
        final Set<String> nodeTypeIncludes = options.getIncludedNodeTypes();
        final Set<String> nodeTypeExcludes = options.getExcludedNodeTypes();

        final String primaryNodeType = node.getPrimaryNodeType().getName();

        return nodeTypeIncludes.contains(primaryNodeType) && !nodeTypeExcludes.contains(primaryNodeType);
    }

    /**
     * Generates a checksum for a single node and its node sub-system, respecting the options.
     * @param aggregateNodePath the absolute path of the node being aggregated into a checksum
     * @param node the node whose subsystem to create a checksum for
     * @param options the {@link ChecksumGeneratorOptions} options
     * @return a map containing 1 entry in the form [ node.getPath() ] : [ CHECKSUM OF NODE SYSTEM ]
     * @throws RepositoryException
     * @throws IOException
     */
    @SuppressWarnings("squid:S3776")
    protected String generatedNodeChecksum(final String aggregateNodePath,
                                                  final Node node,
                                                  final ChecksumGeneratorOptions options)
            throws RepositoryException, IOException {

        if (isExcludedSubTree(node, options)) { return ""; }

        final Map<String, String> checksums = new LinkedHashMap<>();

        if (!isExcludedNodeName(node, options)) {
            /* Create checksums for Node's properties */
            final String checksum = generatePropertyChecksums(aggregateNodePath, node, options);
            if (checksum != null) {
                checksums.put(getChecksumKey(aggregateNodePath, node.getPath()), checksum);
            }
        }

        /* Then process node's children */

        final Map<String, String> lexicographicallySortedChecksums = new TreeMap<>();
        final boolean hasOrderedChildren = hasOrderedChildren(node);
        final NodeIterator children = node.getNodes();

        while (children.hasNext()) {
            final Node child = children.nextNode();

            if (isExcludedSubTree(child, options)) {
                // Skip this node!
            } else if (!isExcludedNodeType(child, options)) {
                if (hasOrderedChildren) {
                    // Use the order dictated by the JCR
                    final String checksum = generatedNodeChecksum(aggregateNodePath, child, options);
                    if (checksum != null) {
                        checksums.put(getChecksumKey(aggregateNodePath, child.getPath()), checksum);

                        log.debug("Aggregated Ordered Node: {} ~> {}",
                                getChecksumKey(aggregateNodePath, child.getPath()), checksum);
                    }

                } else {
                    final String checksum = generatedNodeChecksum(aggregateNodePath, child, options);
                    if (checksum != null) {
                        // If order is not dictated by JCR, collect so we can sort later
                        lexicographicallySortedChecksums.put(getChecksumKey(aggregateNodePath, child.getPath()), checksum);

                        log.debug("Aggregated Unordered Node: {} ~> {}",
                                getChecksumKey(aggregateNodePath, child.getPath()), checksum);
                    }
                }
            }
        }

        if (!hasOrderedChildren && lexicographicallySortedChecksums.size() > 0) {
            // Order is not dictated by JCR, so add the lexicographically sorted entries to the checksums string
            checksums.putAll(lexicographicallySortedChecksums);
        }

        final String nodeChecksum = aggregateChecksums(checksums);
        log.debug("Node [ {} ] has a aggregated checksum of [ {} ]", getChecksumKey(aggregateNodePath, node.getPath()), nodeChecksum);

        return nodeChecksum;
    }

    /**
     * Returns a lexicographically sorted map of the [PROPERTY PATH] : [CHECKSUM OF PROPERTIES].
     * @param aggregateNodePath the absolute path of the node being aggregated into a checksum
     * @param node  the node to collect and checksum the properties for
     * @param options the checksum generator options
     * @return the map of the properties and their checksums
     * @throws RepositoryException
     */
    protected String generatePropertyChecksums(final String aggregateNodePath,
                                                      final Node node,
                                                      final ChecksumGeneratorOptions options)
            throws RepositoryException, IOException {

        SortedMap<String, String> propertyChecksums = new TreeMap<>();
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
                        getChecksumKey(aggregateNodePath, property.getPath()),
                        StringUtils.join(checksums, ","));
            }

            propertyChecksums.put(getChecksumKey(aggregateNodePath, property.getPath()),
                    StringUtils.join(checksums, ","));
        }

        return aggregateChecksums(propertyChecksums);
    }


    /**
     * Generates the relative key used for tracking nodes and properties.
     * @param aggregatePath the absolute path of the node being aggregated.
     * @param path the path of the item being checksumed
     * @return the key
     */
    protected String getChecksumKey(String aggregatePath, String path) {
        if ("/".equals(aggregatePath) && "/".equals(path)) {
            return "/";
        } else if ("/".equals(aggregatePath)) {
            return path;
        }

        String baseNodeName = Text.getName(aggregatePath);
        String relPath = StringUtils.removeStart(path, aggregatePath);

        return baseNodeName + relPath;
    }

    /**
     * Normalizes a property values to a list; allows single and multi-values to be treated the same in code.
     * @param property the propert to get the value(s) from
     * @return a list of the property's value(s)
     * @throws RepositoryException
     */
    private List<Value> getPropertyValues(final Property property) throws RepositoryException {
        final List<Value> values = new ArrayList<>();

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
    protected String getBinaryChecksum(final Value value) throws RepositoryException, IOException {
        InputStream stream = null;

        try {
            stream = value.getBinary().getStream();
            return DigestUtils.sha1Hex(stream);
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
        return DigestUtils.sha1Hex(value.getString());
    }

    /**
     * Checks if node has ordered children.
     * @param node the node
     * @return true if the node has ordered children
     * @throws RepositoryException
     */
    protected boolean hasOrderedChildren(final Node node) throws RepositoryException {
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
    protected String aggregateChecksums(final Map<String, String> checksums) {
        if (checksums.isEmpty()) { return null; }

        StringBuilder data = new StringBuilder();

        for (Map.Entry<String, String> entry : checksums.entrySet()) {
            data.append(entry.getKey() + "=" + entry.getValue());
        }

        return DigestUtils.sha1Hex(data.toString());
    }

    protected boolean isExcludedSubTree(final Node node, final ChecksumGeneratorOptions options) throws RepositoryException {
        for (String exclude : options.getExcludedSubTrees()) {
            if (isPathFragmentMatch(node, exclude)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isExcludedNodeName(final Node node, final ChecksumGeneratorOptions options) throws RepositoryException {
        for (String exclude : options.getExcludedNodeNames()) {
            if (isPathFragmentMatch(node, exclude)) {
                return true;
            }
        }

        return false;
    }


    protected boolean isExcludedNodeType(final Node node, final ChecksumGeneratorOptions options) throws RepositoryException {
        return options.getExcludedNodeTypes().contains(node.getPrimaryNodeType().getName());
    }

    private boolean isPathFragmentMatch(final Node node, final String fragmentPath) throws RepositoryException {
        final List<String> fragments = Arrays.asList(StringUtils.split(fragmentPath, "/"));

        Collections.reverse(fragments);

        Node current = node;
        for (String fragment : fragments) {

            fragment = StringUtils.stripToNull(fragment);

            if (current == null) {
                return false;
            } else if (StringUtils.startsWith(fragment,"[") && StringUtils.endsWith(fragment, "]")) {
                final String nodeType = StringUtils.stripToEmpty(StringUtils.substringBetween(fragment, "[", "]"));

                if (!current.isNodeType(nodeType)) {
                    return false;
                }
            } else {
                if (!StringUtils.equals(fragment, current.getName())) {
                    return false;
                }
            }

            current = current.getParent();
        }

        return true;
    }
}