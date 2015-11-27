package com.adobe.acs.commons.oak.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.CustomChecksumGeneratorOptions;
import com.adobe.acs.commons.oak.impl.EnsureOakIndex.OakIndexDefinitionException;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;

class EnsureOakIndexJobHandler implements Runnable {
	
	 static final Logger log = LoggerFactory.getLogger(EnsureOakIndexJobHandler.class);
	
	/**
	 * 
	 */
	private final EnsureOakIndex ensureOakIndex;

	boolean doSave = false;
	
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
	        EnsureOakIndexJobHandler.PN_FORCE_REINDEX,
	        EnsureOakIndexJobHandler.PN_DELETE,
	        EnsureOakIndexJobHandler.PN_IGNORE,
	        EnsureOakIndexJobHandler.PN_DISABLE,
	        PN_REINDEX,
	        PN_REINDEX_COUNT
	};


	
	public EnsureOakIndexJobHandler (EnsureOakIndex ensureOakIndex, String oakIndexPath, String ensureDefinitionsPath) {
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
     * 
     * The handling is splitted, so that all reindexings can be combined into a single commit; this 
     * ensures, that a single repository traversal can be used to reindex all affected indexes.
     * 
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
        
        List<Resource> delayedProcessing = new ArrayList<Resource>();

        // Combine the index updates which will potentially result in a repository
        // traversal into a single commit.
        // But before handle all other things
        
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
        			log.info("Requesting deletion of a non-existent Oak Index at [ {} ]\n."
        					+ "Consider removing the Ensure Definition at [ {} ] if it is no longer needed.",
        					oakIndexesPath + "/" + ensureDefinition.getName(),
        					ensureDefinition.getPath());
        		}
        	} else if (ensureDefinitionProperties.get(PN_DISABLE,false)) {
        		// DISABLE index
        		this.disableIndex (oakIndex);

        	} else {
        		// handle updates, creates and all reindexing stuff in the second round
        		delayedProcessing.add(ensureDefinition);
        	}
        }
        if (doSave) {
        	log.info("Save all recorded changes to the repository");
        	resourceResolver.commit();
        	doSave = false;
        }
        
        
        
        // second iteration: handle CREATE, UPDATE and REINDEXING
        Iterator<Resource> dpIter = delayedProcessing.iterator();
        while (dpIter.hasNext()) {
        	final Resource ensureDefinition = dpIter.next();
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
                log.error("Skipping " + ensureDefinitions.getPath() + ": " + e.getMessage());
            }
            
        }
        if (doSave) {
        	log.info("Save all recorded changes to the repository, reindexing might start now");
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

        doSave = true;

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

        doSave = true;

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

        doSave = true;

        log.info("Updated Oak Index at [ {} ] with configuration [ {} ]", oakIndex.getPath(),
                ensureDefinition.getPath());

        return oakIndex;
    }
    
    /**
     * Disables an index, so it's no longer updated by Oak
     * @param oakIndex the index
     * @throws PersistenceException 
     */
    private void disableIndex (Resource oakIndex) throws PersistenceException {
    	final ModifiableValueMap oakIndexProperties = oakIndex.adaptTo(ModifiableValueMap.class);
    	oakIndexProperties.put(EnsureOakIndexJobHandler.PN_TYPE, EnsureOakIndexJobHandler.DISABLED);
    	doSave = true;
    	
    	log.info ("Disabled index at {}", oakIndex.getPath());
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
        ensureDefinitionOptions.addIncludedNodeTypes(new String[]{ EnsureOakIndexJobHandler.NT_OAK_UNSTRUCTURED });
        ensureDefinitionOptions.addExcludedProperties(IGNORE_PROPERTIES);

        final Map<String, String> srcChecksum =
                this.ensureOakIndex.checksumGenerator.generateChecksums(session, ensureDefinition.getPath(), ensureDefinitionOptions);

        // Compile checksum for the oakIndex node system
        final CustomChecksumGeneratorOptions oakIndexOptions = new CustomChecksumGeneratorOptions();
        oakIndexOptions.addIncludedNodeTypes(new String[]{ NT_OAK_QUERY_INDEX_DEFINITION });
        oakIndexOptions.addExcludedProperties(IGNORE_PROPERTIES);

        final Map<String, String> destChecksum =
                this.ensureOakIndex.checksumGenerator.generateChecksums(session, oakIndex.getPath(), oakIndexOptions);

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
            doSave = true;
            if (log.isInfoEnabled()) {
                log.info("Deleted Oak Index at [ {} ] in {} ms", path, System.currentTimeMillis() - start);
            }
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
            throw new EnsureOakIndex.OakIndexDefinitionException("Ensure Definition at " + ensureDefinition.getPath() + " missing "
                    + "required property 'text'");
        }
    }
	
}