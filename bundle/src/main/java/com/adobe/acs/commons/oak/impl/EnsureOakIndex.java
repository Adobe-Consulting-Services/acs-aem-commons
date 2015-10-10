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
package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.CustomChecksumGeneratorOptions;
import com.adobe.acs.commons.util.AemCapabilityHelper;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Component(label = "ACS AEM Commons - Ensure Oak Index",
        description = "Component Factory to manage Oak indexes.",
        configurationFactory = true,
        metatype = true)
public class EnsureOakIndex {
    private static final Logger log = LoggerFactory.getLogger(EnsureOakIndex.class);

    private static final String PN_REINDEX = "reindex";

    private static final String PN_REINDEX_COUNT = "reindexCount";

    private static final String PN_RECREATE_ON_UPDATE = "recreateOnUpdate";

    private static final String PN_FORCE_REINDEX = "forceReindex";

    private static final String PN_DELETE = "delete";

    private static final String PN_IGNORE = "ignore";

    private static final String NT_OAK_QUERY_INDEX_DEFINITION = "oak:QueryIndexDefinition";

    private static final String NT_OAK_UNSTRUCTURED = "oak:Unstructured";

    private static final String[] IGNORE_PROPERTIES = new String[]{
            // Jcr Properties
            JcrConstants.JCR_PRIMARYTYPE,
            JcrConstants.JCR_LASTMODIFIED,
            JcrConstants.JCR_LAST_MODIFIED_BY,
            JcrConstants.JCR_MIXINTYPES,
            JcrConstants.JCR_CREATED,
            JcrConstants.JCR_CREATED_BY,
            // EnsureOakIndex properties
            PN_RECREATE_ON_UPDATE,
            PN_FORCE_REINDEX,
            PN_DELETE,
            PN_IGNORE,
            // Oak properties
            PN_REINDEX,
            PN_REINDEX_COUNT
    };

    @Reference
    private AemCapabilityHelper capabilityHelper;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private static final String DEFAULT_ENSURE_DEFINITIONS_PATH = StringUtils.EMPTY;

    @Property(label = "Ensure Definitions Path",
            description = "The absolute path to the resource containing the "
                    + "ACS AEM Commons ensure definitions",
            value = DEFAULT_ENSURE_DEFINITIONS_PATH)
    public static final String PROP_ENSURE_DEFINITIONS_PATH = "ensure-definitions.path";

    private static final String DEFAULT_OAK_INDEXES_PATH = "/oak:index";

    @Property(label = "Oak Indexes Path",
            description = "The absolute path to the oak:index to update; Defaults to [ /oak:index ]",
            value = DEFAULT_OAK_INDEXES_PATH)
    public static final String PROP_OAK_INDEXES_PATH = "oak-indexes.path";

    @Activate
    protected final void activate(Map<String, Object> config) throws RepositoryException {
        if (!capabilityHelper.isOak()) {
            log.info("Cowardly refusing to create indexes on non-Oak instance.");
            return;
        }

        final String ensureDefinitionsPath = PropertiesUtil.toString(config.get(PROP_ENSURE_DEFINITIONS_PATH),
                DEFAULT_ENSURE_DEFINITIONS_PATH);

        final String oakIndexesPath = PropertiesUtil.toString(config.get(PROP_OAK_INDEXES_PATH),
                DEFAULT_OAK_INDEXES_PATH);

        log.info("Ensuring Oak Indexes [ {} ~> {} ]", ensureDefinitionsPath, oakIndexesPath);


        ResourceResolver resourceResolver = null;
        try {

            if (StringUtils.isBlank(ensureDefinitionsPath)) {
                throw new IllegalArgumentException("OSGi Configuration Property `"
                        + PROP_ENSURE_DEFINITIONS_PATH + "` " + "cannot be blank.");
            } else if (StringUtils.isBlank(oakIndexesPath)) {
                throw new IllegalArgumentException("OSGi Configuration Property `"
                        + PROP_OAK_INDEXES_PATH + "` " + "cannot be blank.");
            }

            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

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
            throw new IllegalArgumentException("Unable to find Ensure Definitions resource at "
                    + ensureDefinitionsPath);
        } else if (oakIndexes == null) {
            throw new IllegalArgumentException("Unable to find Oak Indexes source resource at " + oakIndexesPath);
        }

        final Iterator<Resource> ensureDefinitionsIterator = ensureDefinitions.listChildren();
        if (!ensureDefinitionsIterator.hasNext()) {
            log.info("Ensure Definitions path [ {} ] does NOT have children to process", ensureDefinitions.getPath());
        }

        // Iterate over each ensure definition
        while (ensureDefinitionsIterator.hasNext()) {
            final Resource ensureDefinition = ensureDefinitionsIterator.next();
            final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
            final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());

            log.debug("Ensuring Oak Index [ {} ] ~> [ {} ]", ensureDefinition.getPath(),
                    oakIndexesPath + "/" + ensureDefinition.getName());

            try {
                Resource ensuredOakIndex = null;
                if (ensureDefinitionProperties.get(PN_IGNORE, false)) {
                    // IGNORE
                    log.debug("Ignoring index definition at [ {} ]", ensureDefinition.getPath());
                } else if (ensureDefinitionProperties.get(PN_DELETE, false)) {
                    // DELETE
                    if (oakIndex != null) {
                        this.delete(oakIndex);
                    } else {
                        // Oak index does not exist
                        log.info("Requesting deletion of a non-existent Oak Index at [ {} ]\n."
                                + "Consider removing the Ensure Definition at [ {} ] if it is no longer needed.",
                                oakIndexesPath + "/" + ensureDefinition.getName(),
                                ensureDefinition.getPath());
                    }
                } else if (oakIndex == null) {
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

                    if (ensureDefinitionProperties.get(PN_RECREATE_ON_UPDATE, false)) {
                        // Recreate on Update
                        this.delete(oakIndex);
                        ensuredOakIndex = this.create(ensureDefinition, oakIndexes);
                    } else {
                        // Normal Update
                        ensuredOakIndex = this.update(ensureDefinition, oakIndexes);
                    }

                    // Force re-index
                    if (ensureDefinitionProperties.get(PN_FORCE_REINDEX, false)) {
                        this.forceRefresh(ensuredOakIndex);
                    }
                }

            } catch (OakIndexDefinitionException e) {
                log.error("Skipping... " + e.getMessage());
            }
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

        oakIndex.getResourceResolver().commit();

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

        ensuredDefinition.getResourceResolver().commit();

        log.info("Created Oak Index at [ {} ] with Ensure Definition [ {} ]", oakIndex.getPath(),
                ensuredDefinition.getPath());

        return ensuredDefinition.getResourceResolver().getResource(oakIndex.getPath());
    }

    /**
     * Update the oak index with the ensure definition.
     *
     * @param ensureDefinition the ensure definition
     * @param oakIndexes       the parent oak index folder
     * @return the updated oak index resource
     * @throws RepositoryException
     * @throws IOException
     */
    private Resource update(final Resource ensureDefinition, final Resource oakIndexes)
            throws RepositoryException, IOException {

        final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
        final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());
        final ModifiableValueMap oakIndexProperties = oakIndex.adaptTo(ModifiableValueMap.class);

        if (!needsUpdate(ensureDefinition, oakIndex)) {
            log.info("Skipping update... Oak Index at [ {} ] is the same as [ {} ]",
                    oakIndex.getPath(), ensureDefinition.getPath());
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

        ensureDefinition.getResourceResolver().commit();

        log.info("Updated Oak Index at [ {} ] with configuration [ {} ]", oakIndex.getPath(),
                ensureDefinition.getPath());

        return oakIndex;
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

        // Compile checksum for the ensureDefinition node system
        final CustomChecksumGeneratorOptions ensureDefinitionOptions = new CustomChecksumGeneratorOptions();
        ensureDefinitionOptions.addIncludedNodeTypes(new String[]{ NT_OAK_UNSTRUCTURED });
        ensureDefinitionOptions.addExcludedProperties(IGNORE_PROPERTIES);

        final Map<String, String> srcChecksum =
                ChecksumGenerator.generateChecksum(session, ensureDefinition.getPath(), ensureDefinitionOptions);

        // Compile checksum for the oakIndex node system
        final CustomChecksumGeneratorOptions oakIndexOptions = new CustomChecksumGeneratorOptions();
        oakIndexOptions.addIncludedNodeTypes(new String[]{ NT_OAK_QUERY_INDEX_DEFINITION });
        oakIndexOptions.addExcludedProperties(IGNORE_PROPERTIES);

        final Map<String, String> destChecksum =
                ChecksumGenerator.generateChecksum(session, oakIndex.getPath(), oakIndexOptions);

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
            final String path = oakIndex.getPath();

            // Remove the node and its descendants
            oakIndex.adaptTo(Node.class).remove();

            final long start = System.currentTimeMillis();
            oakIndex.getResourceResolver().commit();
            log.info("Deleted Oak Index at [ {} ] in {} ms", path, System.currentTimeMillis() - start);
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
            throw new OakIndexDefinitionException("Resource is null");
        }

        Node node = ensureDefinition.adaptTo(Node.class);

        if (node == null) {
            throw new OakIndexDefinitionException("Resource " + ensureDefinition.getPath()
                    + " cannot be adapted to a Node");
        } else if (!node.isNodeType(NT_OAK_UNSTRUCTURED)) {
            throw new OakIndexDefinitionException("Resource " + ensureDefinition.getPath()
                    + " is not of jcr:primaryType " + NT_OAK_UNSTRUCTURED);
        }

        final ValueMap properties = ensureDefinition.getValueMap();
        if (StringUtils.isBlank(properties.get("type", String.class))) {
            throw new OakIndexDefinitionException("Ensure Definition at " + ensureDefinition.getPath() + " missing "
                    + "required property 'text'");
        }
    }

    private class OakIndexDefinitionException extends Exception {
        public OakIndexDefinitionException(String message) {
            super(message);
        }
    }
}