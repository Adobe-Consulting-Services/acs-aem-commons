package com.adobe.acs.commons.replication.status;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface ReplicationResourceLocator {

	/**
     * Returns the Resource responsible for tracking replication properties for a given path. 
     * <p>
     * Pages and Assets return their respective content resource while any other path returns itself
     *
     * @param path 			   The path to retrieve the resource for.
     * @param resourceResolver The resource resolver must have access to read the specified path.
     */
	public Resource getReplicationStatusResource(String path, ResourceResolver resourceResolver);
	
}
