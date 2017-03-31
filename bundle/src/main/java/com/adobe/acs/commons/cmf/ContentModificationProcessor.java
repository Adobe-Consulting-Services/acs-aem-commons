package com.adobe.acs.commons.cmf;

import java.util.Set;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

public interface ContentModificationProcessor {
	
	
	/**
	 * Identify the resources, which would be affected by a certain ContentModificationStep.
	 * 
	 * @param contentModificationStep  the name of the ContentModificationStep which should be used
	 * @param path determines the subtree which should be validated
	 * @param resolver the resourceresolver to use	
	 * @return null if the ContentModificationStep is not available, or a list of resources as defined by the
	 *   respective ContentModificationProcessor. Returns an empty list of no resources can be identified
	 * @throws NoSuchContentModificationStepException 
	 */
	public IdentifiedResources identifyAffectedResources (String contentModificationStep, String path, ResourceResolver resolver) throws NoSuchContentModificationStepException;
	
	
	
	public void modifyResources (IdentifiedResources resources, ResourceResolver resolver) throws NoSuchContentModificationStepException, PersistenceException;
	
}
