package com.adobe.acs.commons.cmf;

import java.util.Set;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

public interface ContentMigrationProcessor {

	public Set<String> listAvailableMigrationSteps();
	
	
	/**
	 * Identify the resources, which would be affected by a certain ContentMigrationStep.
	 * 
	 * @param contentMigrationStep  the name of the ContentMigrationStep which should be used
	 * @param path determines the subtree which should be validated
	 * @param resolver the resourceresolver to use	
	 * @return null if the ContentMigrationStep is not available, or a list of resources as defined by the
	 *   respective COntentMigrationProcessor. Returns an empty list of no resources can be identified
	 * @throws NoSuchContentMigrationStepException 
	 */
	public IdentifiedResources identifyAffectedResources (String contentMigrationStep, String path, ResourceResolver resolver) throws NoSuchContentMigrationStepException;
	
	
	
	public void migrateResources (IdentifiedResources resources, ResourceResolver resolver) throws NoSuchContentMigrationStepException, PersistenceException;
	
}
