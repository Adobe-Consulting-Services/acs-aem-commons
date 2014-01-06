package com.adobe.acs.commons.replicatepageversion;

import java.util.Date;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONObject;

import com.day.cq.replication.Agent;

public interface ReplicatePageVersionService {
    List<Agent> getAgents();

    JSONObject locateVersionAndResource(ResourceResolver resolver,
            String pageRoot, String assetRoot, String agent, Date date);

}
