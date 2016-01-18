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
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class EnsureOakIndexJobHandler implements Runnable {
    static final Logger log = LoggerFactory.getLogger(EnsureOakIndexJobHandler.class);

    private final EnsureOakIndex ensureOakIndex;

    private String oakIndexesPath;

    private String ensureDefinitionsPath;

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

    static final String[] IGNORE_PROPERTIES = new String[]{
            // Jcr Properties
            JcrConstants.JCR_PRIMARYTYPE,
            JcrConstants.JCR_LASTMODIFIED,
            JcrConstants.JCR_LAST_MODIFIED_BY,
            JcrConstants.JCR_MIXINTYPES,
            JcrConstants.JCR_CREATED,
            JcrConstants.JCR_CREATED_BY,
            PN_RECREATE_ON_UPDATE,
            PN_FORCE_REINDEX,
            PN_DELETE,
            PN_IGNORE,
            PN_DISABLE,
            PN_REINDEX,
            PN_REINDEX_COUNT
    };

    EnsureOakIndexJobHandler(EnsureOakIndex ensureOakIndex, String oakIndexPath, String ensureDefinitionsPath) {
        this.ensureOakIndex = ensureOakIndex;
        this.oakIndexesPath = oakIndexPath;
        this.ensureDefinitionsPath = ensureDefinitionsPath;
    }

    @Override
    public void run() {
        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = this.ensureOakIndex.getResourceResolverFactory().getAdministrativeResourceResolver(null);

            try {
                this.ensure(resourceResolver, ensureDefinitionsPath, oakIndexesPath);
            } catch (PersistenceException e) {
                log.error("Could not ensure management of Oak Index [ {} ]", oakIndexesPath, e);
            } catch (IOException e) {
                log.error("Could not ensure management of Oak Index [ {} ]", oakIndexesPath, e);
            }
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
        } catch (LoginException e) {
            log.error("Could not get an admin resource resolver to ensure Oak Indexes", e);
        } catch (Exception e) {
            log.error("Unknown error occurred while ensuring indexes", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
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
            throw new IllegalArgumentException("Unable to find Ensure Definitions resource at ["
                    + ensureDefinitionsPath + " ]");
        } else if (oakIndexes == null) {
            throw new IllegalArgumentException("Unable to find Oak Indexes resource at [ "
                    + oakIndexesPath + " ]");
        }

        final Iterator<Resource> ensureDefinitionsIterator = ensureDefinitions.listChildren();
        if (!ensureDefinitionsIterator.hasNext()) {
            log.info("Ensure Definitions path [ {} ] does NOT have children to process", ensureDefinitions.getPath());
        }

        final List<Resource> delayedProcessing = new ArrayList<Resource>();

        // First, handle all things that may not result in a a collective re-indexing
        // Includes: IGNORES, DELETES, DISABLED ensure definitions

        while (ensureDefinitionsIterator.hasNext()) {
            final Resource ensureDefinition = ensureDefinitionsIterator.next();
            final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
            final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());

            log.debug("Ensuring Oak Index [ {} ] ~> [ {} ]", ensureDefinition.getPath(),
                    oakIndexesPath + "/" + ensureDefinition.getName());

            Resource ensuredOakIndex = null;

            if (ensureDefinitionProperties.get(PN_IGNORE, false)) {
                // IGNORE
                log.debug("Ignoring index definition at [ {} ]", ensureDefinition.getPath());
            } else if (ensureDefinitionProperties.get(PN_DELETE, false)) {
                // DELETE
                if (oakIndex != null) {
                    this.delete(oakIndex);
                } else if (log.isInfoEnabled()) {
                    // Oak index does not exist
                    log.info("Requesting deletion of a non-existent Oak Index at [ {} ].\n"
                                    + "Consider removing the Ensure Definition at [ {} ] if it is no longer needed.",
                            oakIndexesPath + "/" + ensureDefinition.getName(),
                            ensureDefinition.getPath());
                }
            } else if (ensureDefinitionProperties.get(PN_DISABLE, false)) {
                // DISABLE index
                this.disableIndex(oakIndex);

            } else {
                // handle updates, creates and all reindexing stuff in the second round
                delayedProcessing.add(ensureDefinition);
            }
        }

        if (resourceResolver.hasChanges()) {
            log.info("Saving all DELETES, IGNORES, and DISABLES to [ {} ]", oakIndexesPath);

            resourceResolver.commit();
        }

        // Combine the index updates which will potentially result in a repository traversal into a single commit.

        // second iteration: handle CREATE, UPDATE and REINDEXING
        Iterator<Resource> delayedProcessingEnsureDefinitions = delayedProcessing.iterator();

        while (delayedProcessingEnsureDefinitions.hasNext()) {
            final Resource ensureDefinition = delayedProcessingEnsureDefinitions.next();
            final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
            final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());

            try {
                Resource ensuredOakIndex = null;
                if (oakIndex == null) {
                    // CREATE
                    validateEnsureDefinition(ensureDefinition);
                    ensuredOakIndex = this.create(ensureDefinition, oakIndexes);

                    // Force re-index
                    if (ensureDefinitionProperties.get(PN_FORCE_REINDEX, false)) {
                        this.forceRefresh(ensuredOakIndex);
                    }
                } else {
                    // UPDATE
                    validateEnsureDefinition(ensureDefinition);
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
                log.error("Skipping {} : {}", ensureDefinitions.getPath(), e.getMessage());
            }
        }

        if (resourceResolver.hasChanges()) {
            log.info("Saving all CREATE, UPDATES, and RE-INDEXES, re-indexing may start now.");
            resourceResolver.commit();
        }
    }

    /**
     * Forces index refresh for create or updates (that require updating).
     *
     * @param oakIndex the index representing the oak index
     * @throws PersistenceException
     */
    private void forceRefresh(final Resource oakIndex) throws PersistenceException {
        if (oakIndex == null) {
            return;
        }

        final ModifiableValueMap mvm = oakIndex.adaptTo(ModifiableValueMap.class);
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
    private Resource create(final Resource ensuredDefinition, final Resource oakIndexes) throws PersistenceException,
            RepositoryException {

        final Node oakIndex = JcrUtil.copy(
                ensuredDefinition.adaptTo(Node.class),
                oakIndexes.adaptTo(Node.class),
                ensuredDefinition.getName());

        oakIndex.setPrimaryType(NT_OAK_QUERY_INDEX_DEFINITION);

        log.info("Created Oak Index at [ {} ] with Ensure Definition [ {} ]", oakIndex.getPath(),
                ensuredDefinition.getPath());

        return ensuredDefinition.getResourceResolver().getResource(oakIndex.getPath());
    }

    /**
     * Update the oak index with the ensure definition.
     *
     * @param ensureDefinition the ensure definition
     * @param oakIndexes       the parent oak index folder
     * @param forceReindex    indicates if a recreate of the index is requested
     * @return the updated oak index resource
     * @throws RepositoryException
     * @throws IOException
     */
    private Resource update(final Resource ensureDefinition, final Resource oakIndexes, boolean forceReindex)
            throws RepositoryException, IOException {

        final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
        final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());
        final ModifiableValueMap oakIndexProperties = oakIndex.adaptTo(ModifiableValueMap.class);

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
        Set<String> keys = new HashSet<String>(oakIndexProperties.keySet());

        for (final String key : keys) {
            if (JcrConstants.JCR_PRIMARYTYPE.equals(key)) {
                continue;
            }

            oakIndexProperties.remove(key);
        }

        // Add new properties
        for (final Map.Entry<String, Object> entry : ensureDefinitionProperties.entrySet()) {
            if (JcrConstants.JCR_PRIMARYTYPE.equals(entry.getKey())) {
                continue;
            }

            oakIndexProperties.put(entry.getKey(), entry.getValue());
        }

        // Handle all sub-nodes (ex. Lucene Property Indexes)
        Iterator<Resource> children;

        // Delete child nodes
        children = oakIndex.listChildren();
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
    private void disableIndex(Resource oakIndex) throws PersistenceException {
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
    private boolean needsUpdate(Resource ensureDefinition, Resource oakIndex) throws IOException, RepositoryException {
        final Session session = ensureDefinition.getResourceResolver().adaptTo(Session.class);
        final ChecksumGenerator checksumGenerator = this.ensureOakIndex.getChecksumGenerator();

        // Compile checksum for the ensureDefinition node system
        final CustomChecksumGeneratorOptions ensureDefinitionOptions = new CustomChecksumGeneratorOptions();
        ensureDefinitionOptions.addIncludedNodeTypes(new String[]{NT_OAK_UNSTRUCTURED});
        ensureDefinitionOptions.addExcludedProperties(IGNORE_PROPERTIES);

        final Map<String, String> srcChecksum =
                checksumGenerator.generateChecksums(session, ensureDefinition.getPath(), ensureDefinitionOptions);

        // Compile checksum for the oakIndex node system
        final CustomChecksumGeneratorOptions oakIndexOptions = new CustomChecksumGeneratorOptions();
        oakIndexOptions.addIncludedNodeTypes(new String[]{NT_OAK_QUERY_INDEX_DEFINITION});
        oakIndexOptions.addExcludedProperties(IGNORE_PROPERTIES);

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
    private void delete(final Resource oakIndex) throws RepositoryException, PersistenceException {
        if (oakIndex == null) {
            log.warn("Requesting deletion of a non-existent oak index.");
            return;
        }

        if (oakIndex.adaptTo(Node.class) != null) {
            // Remove the node and its descendants
            oakIndex.adaptTo(Node.class).remove();
        } else {
            log.warn("Oak Index at [ {} ] could not be adapted to a Node for removal.", oakIndex.getPath());
        }
    }

    /**
     * Validate is the ensure definition is in a valid format; uses for create and updates.
     *
     * @param ensureDefinition the ensure definition ensureDefinition
     * @throws RepositoryException
     * @throws OakIndexDefinitionException
     */
    private void validateEnsureDefinition(Resource ensureDefinition)
            throws RepositoryException, OakIndexDefinitionException {

        if (ensureDefinition == null) {
            throw new EnsureOakIndex.OakIndexDefinitionException("Resource is null");
        }

        Node node = ensureDefinition.adaptTo(Node.class);

        if (node == null) {
            throw new EnsureOakIndex.OakIndexDefinitionException("Resource " + ensureDefinition.getPath()
                    + " cannot be adapted to a Node");
        } else if (!node.isNodeType(NT_OAK_UNSTRUCTURED)) {
            throw new EnsureOakIndex.OakIndexDefinitionException("Resource " + ensureDefinition.getPath()
                    + " is not of jcr:primaryType " + NT_OAK_UNSTRUCTURED);
        }

        final ValueMap properties = ensureDefinition.getValueMap();
        if (StringUtils.isBlank(properties.get("type", String.class))) {
            throw new EnsureOakIndex.OakIndexDefinitionException(
                    "Ensure Definition at "
                            + ensureDefinition.getPath()
                            + " missing required property 'text'");
        }
    }
}