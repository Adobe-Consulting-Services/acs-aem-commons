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
import com.adobe.acs.commons.util.AemCapabilityHelper;
import com.adobe.acs.commons.util.InfoWriter;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.JcrConstants;
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

    private static final String PN_MARK_FOR_DELETION = "delete";

    private static final String PN_MARK_FOR_IGNORE = "ignore";

    private static final String NT_OAK_QUERY_INDEX_DEFINITION = "oak:QueryIndexDefinition";

    private static final String NT_OAK_UNSTRUCTURED = "oak:Unstructured";

    private static final String[] IGNORE_PROPERTIES = new String[]{
            JcrConstants.JCR_PRIMARYTYPE,
            PN_MARK_FOR_DELETION,
            PN_MARK_FOR_IGNORE,
            PN_REINDEX,
            PN_REINDEX_COUNT
    };

    @Reference
    private AemCapabilityHelper capabilityHelper;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private static final String DEFAULT_SOURCE_OAK_INDEX_PATH = StringUtils.EMPTY;

    @Property(label = "Index Definitions Source",
            description = "The absolute path the folder that contains the index definitions to ensure")
    public static final String PROP_SOURCE_OAK_INDEX_PATH = "src.path";

    private static final String DEFAULT_DESTINATION_OAK_INDEX_PATH = "/oak:index";

    @Property(label = "Oak Index Destination",
            description = "The absolute path to the oak:index to update; Defaults to [ /oak:index ]",
            value = DEFAULT_DESTINATION_OAK_INDEX_PATH)
    public static final String PROP_DESTINATION_OAK_INDEX_PATH = "dest.path";


    @Property(label = "Force Reindex",
            description = "Sets reindex=true on the following index if they've been created or updated",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_FORCE_REINDEX_OF = "force-reindex";

    @Activate
    protected final void activate(Map<String, Object> config) throws RepositoryException {
        if (!capabilityHelper.isOak()) {
            log.info("Cowardly refusing to create indexes on non-Oak instance.");
            return;
        }

        final String srcPath = PropertiesUtil.toString(config.get(PROP_SOURCE_OAK_INDEX_PATH),
                DEFAULT_SOURCE_OAK_INDEX_PATH);

        final String destPath = PropertiesUtil.toString(config.get(PROP_DESTINATION_OAK_INDEX_PATH),
                DEFAULT_DESTINATION_OAK_INDEX_PATH);

        final String[] forceReindexNames =
                PropertiesUtil.toStringArray(config.get(PROP_FORCE_REINDEX_OF), new String[]{});


        final InfoWriter iw = new InfoWriter();
        iw.title("Ensuring Oak Index");
        iw.message(" * {} ~> {} ", srcPath, destPath);
        iw.line();
        if (forceReindexNames.length > 0) {
            iw.message("Force reindex of:", forceReindexNames);
        }
        iw.end();
        log.info(iw.toString());


        ResourceResolver resourceResolver = null;
        try {

            if (StringUtils.isBlank(srcPath)) {
                throw new IllegalArgumentException("OSGi Configuration Property `"
                        + PROP_SOURCE_OAK_INDEX_PATH + "` " + "cannot be blank.");
            } else if (StringUtils.isBlank(destPath)) {
                throw new IllegalArgumentException("OSGi Configuration Property `"
                        + PROP_DESTINATION_OAK_INDEX_PATH + "` " + "cannot be blank.");
            }

            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            try {
                this.ensure(resourceResolver, srcPath, destPath, forceReindexNames);
            } catch (PersistenceException e) {
                log.error("Could not ensure management of oak index [ {} ]", destPath, e);
            } catch (IOException e) {
                log.error("Could not ensure management of oak index [ {} ]", destPath, e);
            }
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
        } catch (LoginException e) {
            log.error("Could not get an admin resource resolver to ensure oak indexes", e);
        } catch (Exception e) {
            log.error("Unknown error occurred while ensuring indexes", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /**
     * Main work method. Responsible for ensuring the ensure defintions under srcPath are reflected in the real oak
     * index under destPath.
     *
     * @param resourceResolver the resource resolver (must have permissions to read srcPath and modify destPath)
     * @param srcPath          the path containing the ensure definitions
     * @param destPath         the path of the real oak index
     * @param forceReindex     true to force reindex (@reindex=true) on any create or updated indexes.
     * @throws RepositoryException
     * @throws IOException
     */
    private void ensure(final ResourceResolver resourceResolver, final String srcPath, final String destPath,
                        final String[] forceReindex)
            throws RepositoryException, IOException {

        final Resource src = resourceResolver.getResource(srcPath);
        final Resource dest = resourceResolver.getResource(destPath);

        if (src == null) {
            throw new IllegalArgumentException("Unable to find Oak Index source resource at " + srcPath);
        } else if (dest == null) {
            throw new IllegalArgumentException("Unable to find Oak Index source resource at " + destPath);
        }

        final Iterator<Resource> srcChildren = src.listChildren();
        if (!srcChildren.hasNext()) {
            log.info("Oak Index source folder [ {} ] does NOT have children to process", src.getPath());
        }

        // Iterate over each ensure definition
        while (srcChildren.hasNext()) {
            final Resource srcChild = srcChildren.next();
            final ValueMap srcProperties = srcChild.getValueMap();
            final Resource destChild = dest.getChild(srcChild.getName());

            log.debug("Ensuring Oak Index [ {} ] ~> [ {} ]", srcChild.getPath(), destPath + "/" + srcChild.getName());

            try {
                Resource indexResource = null;
                if (srcProperties.get(PN_MARK_FOR_IGNORE, false)) {
                    log.debug("Ignoring index definition at [ {} ]", srcChild.getPath());
                } else if (srcProperties.get(PN_MARK_FOR_DELETION, false)) {
                    // Delete Index
                    this.delete(destChild);
                } else if (destChild == null) {
                    // Create Index
                    validateSourceIndexDefinition(srcChild);
                    indexResource = this.create(srcChild, dest);
                    this.forceRefresh(indexResource, forceReindex);
                } else {
                    // Update Index
                    validateSourceIndexDefinition(srcChild);
                    indexResource = this.update(srcChild, dest);
                    this.forceRefresh(indexResource, forceReindex);
                }

            } catch (OakIndexDefinitionException e) {
                log.error("Skipping... " + e.getMessage());
            }
        }
    }

    /**
     * Forces index refresh for create or updates (that require updating).
     *
     * @param indexResource the index representing the oak index
     * @param forceReindex  the oak index names to force reindexing on
     * @throws PersistenceException
     */
    private void forceRefresh(final Resource indexResource, final String[] forceReindex) throws PersistenceException {
        if (indexResource == null) {
            return;
        } else if (!ArrayUtils.contains(forceReindex, indexResource.getName())) {
            return;
        }

        final ModifiableValueMap mvm = indexResource.adaptTo(ModifiableValueMap.class);
        mvm.put(PN_REINDEX, true);

        indexResource.getResourceResolver().commit();

        log.info("Forcing reindex of [ {} ]", indexResource.getPath());
    }

    /**
     * Create the oak index based on the ensure definition.
     *
     * @param src        the ensure definition
     * @param destFolder the parent oak index folder
     * @return the updated oak index resource
     * @throws PersistenceException
     * @throws RepositoryException
     */
    private Resource create(final Resource src, final Resource destFolder) throws PersistenceException,
            RepositoryException {

        final Node destNode = JcrUtil.copy(
                src.adaptTo(Node.class),
                destFolder.adaptTo(Node.class),
                src.getName());

        destNode.setPrimaryType(NT_OAK_QUERY_INDEX_DEFINITION);

        src.getResourceResolver().commit();

        log.info("Created Oak Index at [ {} ] with configuration from [ {} ]", destNode.getPath(), src.getPath());

        return src.getResourceResolver().getResource(destNode.getPath());
    }

    /**
     * Update the oak index with the ensure definition.
     *
     * @param src        the ensure definition
     * @param destFolder the parent oak index folder
     * @return the updated oak index resource
     * @throws RepositoryException
     * @throws IOException
     */
    private Resource update(final Resource src, final Resource destFolder) throws RepositoryException, IOException {
        final ValueMap srcProperties = src.getValueMap();
        final Resource dest = destFolder.getChild(src.getName());
        final ModifiableValueMap destProperties = dest.adaptTo(ModifiableValueMap.class);

        if (!needsUpdate(src, dest)) {
            log.info("Skipping update... Oak Index at [ {} ] is the same as [ {} ]", dest.getPath(), src.getPath());
            return null;
        }

        // Handle oak:QueryIndexDefinition node
        // Do NOT delete it as this will delete the existing index below it

        // Clear out existing properties
        Set<String> keys = new HashSet<String>(destProperties.keySet());

        for (final String key : keys) {
            if (JcrConstants.JCR_PRIMARYTYPE.equals(key)) {
                continue;
            }

            destProperties.remove(key);
        }

        // Add new properties
        for (final Map.Entry<String, Object> entry : srcProperties.entrySet()) {
            if (JcrConstants.JCR_PRIMARYTYPE.equals(entry.getKey())) {
                continue;
            }

            destProperties.put(entry.getKey(), entry.getValue());
        }

        // Handle all sub-nodes (ex. Lucene Property Indexes)

        Iterator<Resource> children;

        // Delete child nodes
        children = dest.listChildren();
        while (children.hasNext()) {
            children.next().adaptTo(Node.class).remove();
        }

        // Deep copy over child nodes
        children = src.listChildren();
        while (children.hasNext()) {
            final Resource child = children.next();
            JcrUtil.copy(child.adaptTo(Node.class), dest.adaptTo(Node.class), child.getName());
        }

        src.getResourceResolver().commit();

        log.info("Updated Oak Index at [ {} ] with configuration [ {} ]", dest.getPath(), src.getPath());

        return dest;
    }

    /**
     * Determines if the ensure definition is the same as the the same-named oak:index definition.
     *
     * @param src  the ensure index definition
     * @param dest the oak index definition
     * @return true if the ensure definition and the oak index definition are different
     * @throws IOException
     * @throws RepositoryException
     */
    private boolean needsUpdate(Resource src, Resource dest) throws IOException, RepositoryException {
        final Session session = src.getResourceResolver().adaptTo(Session.class);

        // Compile checksum for the src node system
        final CustomChecksumGeneratorOptions srcOptions = new CustomChecksumGeneratorOptions();
        srcOptions.addIncludedNodeTypes(new String[]{NT_OAK_UNSTRUCTURED});
        srcOptions.addExcludedProperties(IGNORE_PROPERTIES);

        final Map<String, String> srcChecksum = ChecksumGenerator.generateChecksum(session, src.getPath(), srcOptions);

        // Compile checksum for the dest node system
        final CustomChecksumGeneratorOptions destOptions = new CustomChecksumGeneratorOptions();
        destOptions.addIncludedNodeTypes(new String[]{NT_OAK_QUERY_INDEX_DEFINITION});
        destOptions.addExcludedProperties(IGNORE_PROPERTIES);

        final Map<String, String> destChecksum =
                ChecksumGenerator.generateChecksum(session, dest.getPath(), destOptions);

        // Compare checksums
        return !StringUtils.equals(srcChecksum.get(src.getPath()), destChecksum.get(dest.getPath()));
    }

    /**
     * Delete the oak index node.
     *
     * @param dest the oak index node to delete
     * @throws RepositoryException
     * @throws PersistenceException
     */
    private void delete(final Resource dest) throws RepositoryException, PersistenceException {
        if (dest == null) {
            return;
        }

        final String path = dest.getPath();

        dest.adaptTo(Node.class).remove();

        final long start = System.currentTimeMillis();
        dest.getResourceResolver().commit();
        log.info("Deleted Oak Index at [ {} ] in {} ms", path, System.currentTimeMillis() - start);
    }

    /**
     * Validate is the ensure definition is in a valid format; uses for create and updates.
     *
     * @param resource the ensure definition resource
     * @throws RepositoryException
     * @throws OakIndexDefinitionException
     */
    private void validateSourceIndexDefinition(Resource resource)
            throws RepositoryException, OakIndexDefinitionException {

        if (resource == null) {
            throw new OakIndexDefinitionException("Resource is null");
        }

        Node node = resource.adaptTo(Node.class);

        if (node == null) {
            throw new OakIndexDefinitionException("Resource " + resource.getPath() + " cannot be adapted to a Node");
        } else if (!node.isNodeType(NT_OAK_UNSTRUCTURED)) {
            throw new OakIndexDefinitionException("Resource " + resource.getPath() + " is not of jcr:primaryType "
                    + NT_OAK_UNSTRUCTURED);
        }

        final ValueMap properties = resource.getValueMap();
        if (StringUtils.isBlank(properties.get("type", String.class))) {
            throw new OakIndexDefinitionException("Source oak definition at " + resource.getPath() + " missing "
                    + "required property 'text'");
        }
    }

    private class OakIndexDefinitionException extends Exception {
        public OakIndexDefinitionException(String message) {
            super(message);
        }
    }
}