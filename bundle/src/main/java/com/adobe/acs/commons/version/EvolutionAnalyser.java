package com.adobe.acs.commons.version;

import org.apache.sling.api.resource.Resource;

public interface EvolutionAnalyser {

	public EvolutionContext getEvolutionContext(Resource resource);
	
}
