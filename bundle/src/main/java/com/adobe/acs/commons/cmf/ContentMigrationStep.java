package com.adobe.acs.commons.cmf;

import java.util.List;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;


/**
 * 
 *
 */


public interface ContentMigrationStep {
	
	public static final String STEP_NAME = "contentMigration.name";

	
	/**
	 * Identify the resources which should be considered for migration
	 * @param path the root resource of the subtree which should be considered
	 * @return the resources which are going to be migrated
	 */
	public List<Resource> identifyResources (Resource resource);
	
	
	public void migrate (Resource resource) throws PersistenceException;
	
}
