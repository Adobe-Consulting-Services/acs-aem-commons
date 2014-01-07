package com.adobe.acs.commons.replicatepageversion.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replicatepageversion.DurboReplicationAgentsFilter;
import com.adobe.acs.commons.replicatepageversion.ReplicatePageVersionService;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.AgentIdFilter;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;

@Component(
        label = "ACS AEM Commons - replicate specific version of a page",
        description = "Service used to replicate specific version of a page/asset through a specific replication agent",
        immediate = false, metatype = false)
@Service
public class ReplicatePageVersionServiceImpl implements
        ReplicatePageVersionService {

    private static final Logger log = LoggerFactory
            .getLogger(ReplicatePageVersionServiceImpl.class);

    @Reference
    private AgentManager agentManager;

    @Reference
    private transient Replicator replicator;

    @Override
    public final List<Agent> getAgents() {
        AgentFilter agentFilter = new DurboReplicationAgentsFilter();
        final List<Agent> agents = new ArrayList<Agent>();
        for (final Agent agent : agentManager.getAgents().values()) {
            if (agentFilter.isIncluded(agent)) {
                agents.add(agent);
            }
        }
        return agents;
    }

    @Override
    public final Agent getAgent(String agentId) {
        for (Agent agent : agentManager.getAgents().values()) {
            if (agent.getId().equals(agentId)) {
                return agent;
            }
        }
        return null;
    }

    @Override
    public final JSONObject locateVersionAndReplicateResource(ResourceResolver resolver,
            String pageRoot, String assetRoot, String agent, Date date) {
        JSONObject obj = new JSONObject();
        List<Resource> resources = null;
        Iterator<Resource> resourceIterator = null;
        boolean error = false;
        try {
            if (pageRoot != null) {
                resources = getResources(resolver, pageRoot);
                resourceIterator = resources.iterator();
                replicateResource(resolver, resourceIterator, agent, date);
            }
            if (assetRoot != null) {
                resources = getResources(resolver, assetRoot);
                resourceIterator = resources.iterator();
                replicateResource(resolver, resourceIterator, agent, date);
            }
            obj.put("status", "replicated");
            obj.put("agentPath", resolver.map(getAgent(agent)
                    .getConfiguration().getConfigPath()));
        } catch (RepositoryException e) {
            error = true;
        } catch (ReplicationException e) {
            error = true;
        } catch (JSONException e) {
            error = true;
        } finally {
            if (error) {
                try {
                    obj.put("error", "System Error.");
                    obj.put("status", "error");
                } catch (JSONException e) {
                    log.error("exception occured", e);
                }

            }
        }

        return obj;
    }
    @Override
    public final List<Resource> getResources(ResourceResolver resolver, String root) {

        Resource res = resolver.getResource(root);
        List<Resource> resources = new ArrayList<Resource>();
        try {
            buildResourceList(resolver, res, resources);
        } catch (RepositoryException e) {
            log.error("exception occured", e);
        }
        return resources;
    }

    private void buildResourceList(ResourceResolver resolver, Resource res,
            List<Resource> resources) throws RepositoryException {
        Node node = res.adaptTo(Node.class);
        if (!node.isNodeType("nt:hierarchyNode")) {
            return;
        }
        resources.add(res);
        Iterator<Resource> iter = resolver.listChildren(res);
        while (iter.hasNext()) {
            buildResourceList(resolver, iter.next(), resources);
        }
    }

    @Override
    public final void replicateResource(ResourceResolver resolver,
            Iterator<Resource> resourceIterator, String agent, Date date)
            throws RepositoryException, ReplicationException {
        Session session = resolver.adaptTo(Session.class);
        Resource resource = null;
        Version v = null;
        ReplicationOptions opts = new ReplicationOptions();

        AgentIdFilter agentFilter = new AgentIdFilter(agent);
        opts.setFilter(agentFilter);
        while (resourceIterator.hasNext()) {

            resource = resourceIterator.next();

            v = getAppropriateVersion(resource, date, session);
            if (v == null) {
                continue;
            }

            opts.setRevision(v.getName());

            replicator.replicate(session, ReplicationActionType.ACTIVATE,
                    resource.getPath(), opts);
            log.info("replicating  path:" + resource.getPath());

        }
    }

    public final Version getAppropriateVersion(Resource resource, Date date,
            Session session) throws RepositoryException {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        String path = resource.getPath();
        List<Version> versions = findAllVersions(path, session);
        Collections.sort(versions, new Comparator<Version>() {
            public int compare(Version v1, Version v2) {
                try {
                    return v2.getCreated().compareTo(v1.getCreated());
                } catch (RepositoryException e) {
                    return 0;
                }
            }
        });

        for (Version v : versions) {
            try {
                if (v.getCreated().compareTo(cal) < 1) {
                    return v;
                }
            } catch (RepositoryException e) {
                log.error("exception occured", e);
            }
        }
        return null;

    }

    @Override
    public final List<Version> findAllVersions(String path, Session session)
            throws RepositoryException {
        List<Version> versions = new ArrayList<Version>();

        Node node = session.getNode(path);
        if (node.hasNode(NameConstants.NN_CONTENT)) {
            Node contentNode = node.getNode(NameConstants.NN_CONTENT);
            if (contentNode.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                VersionIterator iter = session.getWorkspace()
                        .getVersionManager()
                        .getVersionHistory(contentNode.getPath())
                        .getAllVersions();
                while (iter.hasNext()) {
                    Version v = iter.nextVersion();
                    versions.add(v);
                }
            }else if(node.isNodeType(JcrConstants.MIX_VERSIONABLE)){
                VersionIterator iter = session.getWorkspace()
                        .getVersionManager()
                        .getVersionHistory(node.getPath())
                        .getAllVersions();
                while (iter.hasNext()) {
                    Version v = iter.nextVersion();
                    versions.add(v);
                }
            }
        }

        return versions;
    }

}
