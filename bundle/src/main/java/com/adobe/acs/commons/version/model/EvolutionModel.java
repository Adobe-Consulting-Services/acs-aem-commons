package com.adobe.acs.commons.version.model;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;

import com.adobe.acs.commons.version.EvolutionContext;

@Model(adaptables = SlingHttpServletRequest.class)
public class EvolutionModel {

	@Inject
	ResourceResolver resolver;

	private final SlingHttpServletRequest request;
	private final String path;
	
	public EvolutionModel(SlingHttpServletRequest request){
		this.request = request;
		this.path = request.getParameter("path");
	}

	public String getResourcePath() {
		return path;
	}

	public EvolutionContext getEvolution() {
		if (StringUtils.isNotEmpty(path)) {
			Resource resource = resolver.resolve(path);
			if(resource != null && !ResourceUtil.isNonExistingResource(resource)){
				return new EvolutionContext(resource);	
			}
		}
		return null;
	}

}
