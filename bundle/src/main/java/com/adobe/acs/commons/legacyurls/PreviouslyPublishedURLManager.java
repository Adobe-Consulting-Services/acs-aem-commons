package com.adobe.acs.commons.legacyurls;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;

public interface PreviouslyPublishedURLManager {
    public void create(ResourceResolver resourceResolver, String path, String... urls) throws RepositoryException;
    public void update(ResourceResolver resourceResolver, String path, String... urls) throws RepositoryException;
    public void delete(ResourceResolver resourceResolver, String... urls) throws RepositoryException;
    public Resource find(ResourceResolver resourceResolver, String url);
}
