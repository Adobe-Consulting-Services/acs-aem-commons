package com.adobe.acs.commons.replicatepageversion;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONObject;

import com.day.cq.replication.Agent;
import com.day.cq.replication.ReplicationException;

public interface ReplicatePageVersionService {
    
    List<Agent> getAgents();

    JSONObject locateVersionAndReplicateResource(ResourceResolver resolver,
            String pageRoot, String assetRoot, String agent, Date date);
    
    Agent getAgent(String agentId);
    
    List<Resource> getResources(ResourceResolver resolver, String root);
    
    void replicateResource(ResourceResolver resolver,
            Iterator<Resource> resourceIterator, String agent, Date date) throws RepositoryException, ReplicationException;
    
    Version getAppropriateVersion(Resource resource, Date date,
            Session session) throws RepositoryException;
    
    List<Version> findAllVersions(String path, Session session)
            throws RepositoryException;

}
