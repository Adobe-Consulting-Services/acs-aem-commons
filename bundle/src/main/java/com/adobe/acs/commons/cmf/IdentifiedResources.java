package com.adobe.acs.commons.cmf;

import java.util.List;

import org.apache.sling.api.resource.Resource;



public class IdentifiedResources {
	
	List <String> resources; // the paths only
	String contentMigrationStep;
	
	public IdentifiedResources (List<String> resources, String name) {
		this.resources = resources;
		this.contentMigrationStep = name;
	}
	
	
	public List <String> getResources () {
		return resources;
	}
	
	public String getContentMigrationStep () {
		return contentMigrationStep;
	}
	
	

}
