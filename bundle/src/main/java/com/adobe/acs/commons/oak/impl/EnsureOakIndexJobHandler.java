/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.CustomChecksumGeneratorOptions;
import com.adobe.acs.commons.oak.impl.EnsureOakIndex.OakIndexDefinitionException;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EnsureOakIndexJobHandler implements Runnable {
    //@formatter:off
    static final Logger log = LoggerFactory.getLogger(EnsureOakIndexJobHandler.class);
    static final String PN_FORCE_REINDEX = "forceReindex";

    static final String PN_DELETE = "delete";

    static final String PN_IGNORE = "ignore";

    static final String PN_DISABLE = "disable";

    static final String NT_OAK_QUERY_INDEX_DEFINITION = "oak:QueryIndexDefinition";

    static final String NT_OAK_UNSTRUCTURED = "oak:Unstructured";

    static final String PN_TYPE = "type";

    static final String DISABLED = "disabled";

    static final String PN_RECREATE_ON_UPDATE = "recreateOnUpdate";

    static final String PN_REINDEX_COUNT = "reindexCount";

    static final String PN_REINDEX = "reindex";

    static final String ENSURE_OAK_INDEX_USER_NAME = "Ensure Oak Index";

    static final String[] MANDATORY_IGNORE_PROPERTIES = new String[]{
            // JCR Properties
            JcrConstants.JCR_PRIMARYTYPE,
            JcrConstants.JCR_LASTMODIFIED,
            JcrConstants.JCR_LAST_MODIFIED_BY,
            JcrConstants.JCR_MIXINTYPES,
            JcrConstants.JCR_CREATED,
            JcrConstants.JCR_CREATED_BY,
            // Ensure Oak Index Properties
            PN_RECREATE_ON_UPDATE,
            PN_FORCE_REINDEX,
            PN_DELETE,
            PN_IGNORE,
            PN_DISABLE,
            // Oak Index Properties
            PN_REINDEX,
            PN_REINDEX_COUNT,
    };
    private static final String[] NAME_PROPERTIES = new String[] {"propertyNames", "declaringNodeTypes"} ;

    static final String SERVICE_NAME = "ensure-oak-index";

    private final EnsureOakIndex ensureOakIndex;

    private final List<String> ignoreProperties = new ArrayList<>();

    private String oakIndexesPath;

    private String ensureDefinitionsPath;
    //@formatter:on

    EnsureOakIndexJobHandler(EnsureOakIndex ensureOakIndex, String oakIndexPath, String ensureDefinitionsPath) {
        this.ensureOakIndex = ensureOakIndex;
        this.oakIndexesPath = oakIndexPath;
        this.ensureDefinitionsPath = ensureDefinitionsPath;

        this.ignoreProperties.addAll(Arrays.asList(MANDATORY_IGNORE_PROPERTIES));

        if (ensureOakIndex != null) {
            this.ignoreProperties.addAll(ensureOakIndex.getIgnoreProperties());
        }
    }

    @Override
    @SuppressWarnings("squid:S1141")
    public void run() {
        Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
        try (ResourceResolver resourceResolver = this.ensureOakIndex.getResourceResolverFactory().getServiceResourceResolver(authInfo)) {

            // we should rethink this nested try here ...
            try {
                this.ensure(resourceResolver, ensureDefinitionsPath, oakIndexesPath);
            } catch (IOException e) {
                log.error("Could not ensure management of Oak Index [ {} ]", oakIndexesPath, e);
            }
        } catch (IllegalArgumentException e) {
            log.error("Could not ensure oak indexes due to illegal arguments.",e);
        } catch (LoginException e) {
            log.error("Could not get an admin resource resolver to ensure Oak Indexes", e);
        } catch (Exception e) {
            log.error("Unknown error occurred while ensuring indexes", e);
        }
    }

    /**
     * Main work method. Responsible for ensuring the ensure definitions under srcPath are reflected in the real oak
     * index under oakIndexesPath.
     * <p/>
     * The handling is split, so that all re-indexings can be combined into a single commit; this
     * ensures, that a single repository traversal can be used to reindex all affected indexes.
     *
     * @param resourceResolver      the resource resolver (must have permissions to read definitions and change indexes)
     * @param ensureDefinitionsPath the path containing the ensure definitions
     * @param oakIndexesPath        the path of the real oak index
     * @throws RepositoryException
     * @throws IOException
     */
    private void ensure(final ResourceResolver resourceResolver, final String ensureDefinitionsPath,
                        final String oakIndexesPath)
            throws RepositoryException, IOException {

        final Resource ensureDefinitions = resourceResolver.getResource(ensureDefinitionsPath);
        final Resource oakIndexes = resourceResolver.getResource(oakIndexesPath);

        if (ensureDefinitions == null) {
            throw new IllegalArgumentException("Unable to find Ensure Definitions resource at [ "
                    + ensureDefinitionsPath + " ]");
        } else if (oakIndexes == null) {
            throw new IllegalArgumentException("Unable to find Oak Indexes resource at [ "
                    + oakIndexesPath + " ]");
        }

        final Iterator<Resource> ensureDefinitionsIterator = ensureDefinitions.listChildren();
        if (!ensureDefinitionsIterator.hasNext()) {
            log.info("Ensure Definitions path [ {} ] does NOT have children to process", ensureDefinitions.getPath());
        }

        final List<Resource> delayedProcessing = new ArrayList<>();

        // First, handle all things that may not result in a a collective re-indexing
        // Includes: IGNORES, DELETES, DISABLED ensure definitions

        while (ensureDefinitionsIterator.hasNext()) {
            final Resource ensureDefinition = ensureDefinitionsIterator.next();
            final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());

            log.debug("Ensuring Oak Index [ {} ] ~> [ {}/{} ]",
                    ensureDefinition.getPath(), oakIndexesPath, ensureDefinition.getName());

            if (!handleLightWeightIndexOperations(
                    ensureDefinition, oakIndex)) {
                delayedProcessing.add(ensureDefinition);
            }
        }

        if (resourceResolver.hasChanges()) {
            log.info("Saving all DELETES, IGNORES, and DISABLES to [ {} ]", oakIndexesPath);
            resourceResolver.commit();
            log.debug("Commit succeeded");
        }

        // Combine the index updates which will potentially result in a repository traversal into a single commit.
        // second iteration: handle CREATE, UPDATE and REINDEXING
        Iterator<Resource> delayedProcessingEnsureDefinitions = delayedProcessing.iterator();

        while (delayedProcessingEnsureDefinitions.hasNext()) {
            final Resource ensureDefinition = delayedProcessingEnsureDefinitions.next();
            final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());

            handleHeavyWeightIndexOperations(oakIndexes, ensureDefinition,
                    oakIndex);
        }

        if (resourceResolver.hasChanges()) {
            log.info("Saving all CREATE, UPDATES, and RE-INDEXES, re-indexing may start now..");
            resourceResolver.commit();
            log.debug("Commit succeeded");
        }
    }

    /**
     * Handle CREATE and UPDATE operations.
     *
     * @param oakIndexes
     * @param ensureDefinition
     * @param oakIndex
     * @throws RepositoryException
     * @throws PersistenceException
     * @throws IOException
     */
    void handleHeavyWeightIndexOperations(final Resource oakIndexes,
                                          final @Nonnull Resource ensureDefinition, final @Nullable Resource oakIndex)
            throws RepositoryException, IOException {
        final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();

        try {
            Resource ensuredOakIndex = null;
            validateEnsureDefinition(ensureDefinition);
            if (oakIndex == null) {
                // CREATE
                ensuredOakIndex = this.create(ensureDefinition, oakIndexes);

                // Force re-index
                if (ensureDefinitionProperties.get(PN_FORCE_REINDEX, false)) {
                    this.forceRefresh(ensuredOakIndex);
                }
            } else {
                // UPDATE
                boolean forceReindex = ensureDefinitionProperties.get(PN_FORCE_REINDEX, false);

                if (ensureDefinitionProperties.get(PN_RECREATE_ON_UPDATE, false)) {
                    // Recreate on Update, refresh not required (is implicit)
                    this.delete(oakIndex);
                    this.create(ensureDefinition, oakIndexes);
                } else {
                    // Normal Update
                    this.update(ensureDefinition, oakIndexes, forceReindex);
                }
            }
        } catch (OakIndexDefinitionException e) {
            log.error("Skipping processing of {}", ensureDefinition.getPath(), e);
        }
    }

    /**
     * handle the operations IGNORE, DELETE and DISABLE
     *
     * @param ensureDefinition
     * @param oakIndex
     * @return true if the definition has been handled; if true the definition needs further processing
     * @throws RepositoryException
     * @throws PersistenceException
     */
    boolean handleLightWeightIndexOperations(
            final @Nonnull Resource ensureDefinition, final @Nullable Resource oakIndex)
            throws RepositoryException, PersistenceException {

        final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
        boolean result = true;


        if (ensureDefinitionProperties.get(PN_IGNORE, false)) {
            // IGNORE
            log.debug("Ignoring index definition at [ {} ]", ensureDefinition.getPath());
        } else if (ensureDefinitionProperties.get(PN_DELETE, false)) {
            // DELETE
            if (oakIndex != null) {
                this.delete(oakIndex);
            } else {
                // Oak index does not exist
                log.info("Requesting deletion of a non-existent Oak Index at [ {}/{} ].\nConsider removing the Ensure Definition at [ {} ] if it is no longer needed.",
                        oakIndexesPath, ensureDefinition.getName(),
                        ensureDefinition.getPath());
            }
        } else if (ensureDefinitionProperties.get(PN_DISABLE, false)) {
            // DISABLE index
            if (oakIndex != null) {
                this.disableIndex(oakIndex);
            } else {
                // Oak index does not exist
                log.info("Requesting disable of a non-existent Oak Index at [ {}/{} ].\nConsider removing the Ensure Definition at [ {} ] if it is no longer needed.",
                        oakIndexesPath, ensureDefinition.getName(), ensureDefinition.getPath());
            }
        } else {
            // handle updates, creates and all reindexing stuff in the second round
            result = false;
        }
        return result;
    }

    /**
     * Forces index refresh for create or updates (that require updating).
     *
     * @param oakIndex the index representing the oak index
     * @throws PersistenceException
     */
    public void forceRefresh(final @Nonnull Resource oakIndex) throws PersistenceException {

        final ModifiableValueMap mvm = oakIndex.adaptTo(ModifiableValueMap.class);
        if (mvm == null ) {
            String msg = String.format("Cannot adapt {} to a ModifiableValueMap (permissions?)", oakIndex.getPath());
            throw new PersistenceException(msg);
        }
        mvm.put(PN_REINDEX, true);

        log.info("Forcing re-index of [ {} ]", oakIndex.getPath());
    }

    /**
     * Create the oak index based on the ensure definition.
     *
     * @param ensuredDefinition the ensure definition
     * @param oakIndexes        the parent oak index folder
     * @return the updated oak index resource
     * @throws PersistenceException
     * @throws RepositoryException
     */
    public Resource create(final @Nonnull Resource ensuredDefinition, final @Nonnull Resource oakIndexes) throws PersistenceException,
            RepositoryException {

        final Node oakIndex = JcrUtil.copy(
                ensuredDefinition.adaptTo(Node.class),
                oakIndexes.adaptTo(Node.class),
                ensuredDefinition.getName());

        oakIndex.setPrimaryType(NT_OAK_QUERY_INDEX_DEFINITION);
        oakIndex.setProperty(JcrConstants.JCR_CREATED, Calendar.getInstance());
        oakIndex.setProperty(JcrConstants.JCR_CREATED_BY, ENSURE_OAK_INDEX_USER_NAME);

        log.info("Created Oak Index at [ {} ] with Ensure Definition [ {} ]", oakIndex.getPath(),
                ensuredDefinition.getPath());

        return ensuredDefinition.getResourceResolver().getResource(oakIndex.getPath());
    }

    /**
     * Update the oak index with the ensure definition.
     *
     * @param ensureDefinition the ensure definition
     * @param oakIndexes       the parent oak index folder
     * @param forceReindex     indicates if a recreate of the index is requested
     * @return the updated oak index resource
     * @throws RepositoryException
     * @throws IOException
     */
    @SuppressWarnings("squid:S3776")
    public Resource update(final @Nonnull Resource ensureDefinition, final @Nonnull Resource oakIndexes, boolean forceReindex)
            throws RepositoryException, IOException {

        final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
        final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());

        final Node oakIndexNode = oakIndex.adaptTo(Node.class);
        final Node ensureDefinitionNode = ensureDefinition.adaptTo(Node.class);

        if (!this.needsUpdate(ensureDefinition, oakIndex)) {
            if (ensureDefinitionProperties.get(PN_FORCE_REINDEX, false)) {
                log.info("Skipping update... Oak Index at [ {} ] is the same as [ {} ] and forceIndex flag is ignored",
                        oakIndex.getPath(), ensureDefinition.getPath());
            } else {
                log.info("Skipping update... Oak Index at [ {} ] is the same as [ {} ]",
                        oakIndex.getPath(), ensureDefinition.getPath());
            }

            return null;
        }

        // Handle oak:QueryIndexDefinition node
        // Do NOT delete it as this will delete the existing index below it

        // Clear out existing properties
        final Iterator<Property> existingOakIndexProperties = copyIterator(oakIndexNode.getProperties());
        while(existingOakIndexProperties.hasNext()) {
            final Property property = existingOakIndexProperties.next();
            final String propertyName = property.getName();

            if (this.ignoreProperties.contains(propertyName)) {
                continue;
            }

            JcrUtil.setProperty(oakIndexNode, propertyName, null);
        }


        // Add new properties
        final Iterator<Property> addProperties = copyIterator(ensureDefinitionNode.getProperties());
        while (addProperties.hasNext()) {
            final Property property = addProperties.next();

            if (this.ignoreProperties.contains(property.getName())) {
                // Skip ignored properties
                continue;
            }

            if (ArrayUtils.contains(NAME_PROPERTIES, property.getName()) && property.getType() != PropertyType.NAME) {
                log.warn("{}@{} property should be of type: Name[]", oakIndex.getPath(), property.getName());
            }

            JcrUtil.copy(property, oakIndexNode, property.getName());
        }

        JcrUtil.setProperty(oakIndexNode, JcrConstants.JCR_LAST_MODIFIED_BY, ENSURE_OAK_INDEX_USER_NAME);
        JcrUtil.setProperty(oakIndexNode, JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());

        // Handle all sub-nodes (ex. Lucene Property Indexes)

        // Delete child nodes
        Iterator<Resource> children = oakIndex.listChildren();
        while (children.hasNext()) {
            children.next().adaptTo(Node.class).remove();
        }

        // Deep copy over child nodes
        children = ensureDefinition.listChildren();
        while (children.hasNext()) {
            final Resource child = children.next();
            JcrUtil.copy(child.adaptTo(Node.class), oakIndex.adaptTo(Node.class), child.getName());
        }

        if (forceReindex) {
            log.info("Updated Oak Index at [ {} ] with configuration [ {} ], triggering reindex",
                    oakIndex.getPath(), ensureDefinition.getPath());
            forceRefresh(oakIndex);
        } else {
            // A reindexing should be required to make this change effective, so WARN if not present
            log.warn("Updated Oak Index at [ {} ] with configuration [ {} ], but no reindex requested!",
                    oakIndex.getPath(), ensureDefinition.getPath());
        }

        return oakIndex;
    }

    /**
     * Disables an index, so it's no longer updated by Oak.
     *
     * @param oakIndex the index
     * @throws PersistenceException
     */
    public void disableIndex(@Nonnull Resource oakIndex) throws PersistenceException {
        final ModifiableValueMap oakIndexProperties = oakIndex.adaptTo(ModifiableValueMap.class);
        oakIndexProperties.put(PN_TYPE, DISABLED);

        log.info("Disabled index at {}", oakIndex.getPath());
    }

    /**
     * Determines if the ensure definition is the same as the the same-named oak:index definition.
     *
     * @param ensureDefinition the ensure index definition
     * @param oakIndex         the oak index definition
     * @return true if the ensure definition and the oak index definition are different
     * @throws IOException
     * @throws RepositoryException
     */
    boolean needsUpdate(@Nonnull Resource ensureDefinition, @Nonnull Resource oakIndex) throws IOException, RepositoryException {
        final Session session = ensureDefinition.getResourceResolver().adaptTo(Session.class);
        final ChecksumGenerator checksumGenerator = this.ensureOakIndex.getChecksumGenerator();

        // Compile checksum for the ensureDefinition node system
        final CustomChecksumGeneratorOptions ensureDefinitionOptions = new CustomChecksumGeneratorOptions();
        ensureDefinitionOptions.addIncludedNodeTypes(new String[]{NT_OAK_UNSTRUCTURED});
        ensureDefinitionOptions.addExcludedProperties(this.ignoreProperties);

        final Map<String, String> srcChecksum =
                checksumGenerator.generateChecksums(session, ensureDefinition.getPath(), ensureDefinitionOptions);

        // Compile checksum for the oakIndex node system
        final CustomChecksumGeneratorOptions oakIndexOptions = new CustomChecksumGeneratorOptions();
        oakIndexOptions.addIncludedNodeTypes(new String[]{NT_OAK_QUERY_INDEX_DEFINITION});
        oakIndexOptions.addExcludedProperties(this.ignoreProperties);

        final Map<String, String> destChecksum =
                checksumGenerator.generateChecksums(session, oakIndex.getPath(), oakIndexOptions);

        // Compare checksums
        return !StringUtils.equals(srcChecksum.get(ensureDefinition.getPath()), destChecksum.get(oakIndex.getPath()));
    }

    /**
     * Delete the oak index node.
     *
     * @param oakIndex the oak index node to delete
     * @throws RepositoryException
     * @throws PersistenceException
     */
    public void delete(final @Nonnull Resource oakIndex) throws RepositoryException, PersistenceException {

        if (oakIndex.adaptTo(Node.class) != null) {
            // Remove the node and its descendants
            oakIndex.adaptTo(Node.class).remove();
        } else {
            log.warn("Oak Index at [ {} ] could not be adapted to a Node for removal.", oakIndex.getPath());
        }
    }

    /**
     * Validate that the ensure definition is in a valid format; uses for create and updates.
     *
     * @param ensureDefinition the ensure definition ensureDefinition
     * @throws RepositoryException
     * @throws OakIndexDefinitionException
     */
    public void validateEnsureDefinition(@Nonnull Resource ensureDefinition)
            throws RepositoryException, OakIndexDefinitionException {

        Node node = ensureDefinition.adaptTo(Node.class);

        if (node == null) {
            throw new EnsureOakIndex.OakIndexDefinitionException("Resource " + ensureDefinition.getPath()
                    + " cannot be adapted to a Node");
        } else if (!node.isNodeType(NT_OAK_UNSTRUCTURED)) {
            throw new EnsureOakIndex.OakIndexDefinitionException("Resource " + ensureDefinition.getPath()
                    + " is not of jcr:primaryType " + NT_OAK_UNSTRUCTURED);
        }

        final ValueMap properties = ensureDefinition.getValueMap();
        if (StringUtils.isBlank(properties.get(PN_TYPE, String.class))) {
            throw new EnsureOakIndex.OakIndexDefinitionException(
                    "Ensure Definition at "
                            + ensureDefinition.getPath()
                            + " missing required property 'type'");
        }
    }

    /**
     * Creates a copy of an iterator. This allows us to safely change the underlying structure of the src iterator, without disturbing the wrapping iteration;
     * @param src the src iterator to copy.
     * @return a copy of the src iterator.
     */
    private Iterator<Property> copyIterator(Iterator<Property> src) {
        List<Property> dest = new ArrayList<Property>();
        while (src.hasNext()) {
            dest.add(src.next());
        }

        return dest.iterator();
    }
}
