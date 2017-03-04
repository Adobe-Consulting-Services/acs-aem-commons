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
	
	
	/**
	 * Migrate the resource.
	 * 
	 * Performs all relevant operations on the resource. An implementation should not persist
	 * the changes, that means it should not call <code>resource.getResourceResolver().commit();</code>
	 * @param resource the resource which should be migrated
	 */
	public void migrate (Resource resource);
	
}
