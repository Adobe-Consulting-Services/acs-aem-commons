package com.adobe.acs.commons.replication.status.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.status.ReplicationResourceLocator;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * ACS AEM Commons - Replication Node Resolver
 * OSGi Service for finding the resource responsible for tracking replication status on a given node.
 */
@Component
@Service
public class ReplicationResourceLocatorImpl implements ReplicationResourceLocator {

	private static final Logger log = LoggerFactory.getLogger(ReplicationResourceLocatorImpl.class);
	
	@Override
	public Resource getReplicationStatusResource(String path, ResourceResolver resourceResolver) {
		final Page page = resourceResolver.adaptTo(PageManager.class).getContainingPage(path);
		final Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(path));

		Resource resource;

		if (page != null) {
		    // Page
		    resource = page.getContentResource();
		    log.trace("Candidate Page for setting replicateBy is [ {} ]", resource.getPath());
		} else if (asset != null) {
		    // DAM Asset
		    final Resource assetResource = resourceResolver.getResource(asset.getPath());
		    resource = assetResource.getChild(JcrConstants.JCR_CONTENT);
		    log.trace("Candidate Asset for setting replicateBy is [ {} ]", resource.getPath());
		} else {
		    // Some other resource
		    resource = resourceResolver.getResource(path);
		    log.trace("Candidate Resource for setting replicateBy is [ {} ]", resource.getPath());
		}
		return resource;
	}


}
