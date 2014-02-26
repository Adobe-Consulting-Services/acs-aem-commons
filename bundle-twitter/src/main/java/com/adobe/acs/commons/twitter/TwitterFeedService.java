package com.adobe.acs.commons.twitter;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ResourceResolver;

public interface TwitterFeedService {
	
	public void refreshTwitterFeed(ResourceResolver resourceResolver, String[] twitterComponentPaths) throws RepositoryException;

}
