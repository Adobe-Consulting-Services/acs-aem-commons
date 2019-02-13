/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.replication.impl;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.ReplicateVersion;
import com.adobe.acs.commons.replication.ReplicationResult;
import com.adobe.acs.commons.replication.ReplicationResult.Status;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.AgentIdFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;

/**
 * ACS AEM Commons - replicate specific version of a resource tree
 * Service used to replicate specific version of a resource tree through a
 * specific replication agent
 */
@Component
@Service
public class ReplicateVersionImpl implements
        ReplicateVersion {

    private static final Logger log = LoggerFactory
            .getLogger(ReplicateVersionImpl.class);

    @Reference
    private Replicator replicator;

    @Override
    public final List<ReplicationResult> replicate(
            ResourceResolver resolver, String[] rootPaths, String[] agents,
            Date date) {
        List<ReplicationResult> list = new ArrayList<ReplicationResult>();

            if (rootPaths != null) {
                for (String rootPath : rootPaths) {
                    String normalizedPath = getNormalizedPath(rootPath);
                    List<Resource> resources = getResources(resolver, normalizedPath);
                    
                    List<ReplicationResult> resultsForPath = 
                            replicateResource(resolver, resources, agents, date);
                    list.addAll(resultsForPath);

                }

            }


        return list;
    }

    private List<Resource> getResources(ResourceResolver resolver, String root) {

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
        if (!node.isNodeType(JcrConstants.NT_HIERARCHYNODE)) {
            return;
        }
        resources.add(res);

        for (Iterator<Resource> iter = resolver.listChildren(res); iter.hasNext();) {
            Resource resChild = iter.next();
            buildResourceList(resolver, resChild, resources);
        }
    }

    private List<ReplicationResult> replicateResource(ResourceResolver resolver,
            List<Resource> resources, String[] agents, Date date) {
        List<ReplicationResult> results = new ArrayList<ReplicationResult>();

        ReplicationOptions opts = new ReplicationOptions();

        AgentIdFilter agentFilter = new AgentIdFilter(agents);
        opts.setFilter(agentFilter);
        Session session = resolver.adaptTo(Session.class);
        for (Resource resource : resources) {
            String path = resource.getPath();
            try {
                Version v = getAppropriateVersion(resource, date, session);
                if (v == null) {
                    results.add(new ReplicationResult(path, Status.not_replicated));
                    continue;
                }

                String versionName = v.getName();
                opts.setRevision(versionName);

                replicator.replicate(session, ReplicationActionType.ACTIVATE, path, opts);

                results.add(new ReplicationResult(path, Status.replicated, versionName));

            } catch (RepositoryException e) {
                results.add(new ReplicationResult(path, Status.error));
                log.error("Exception while replicating version of " + path, e);
            } catch (ReplicationException e) {
                results.add(new ReplicationResult(path, Status.error));
                log.error("Exception while replicating version of " + path, e);
            }
        }

        return results;
    }

    private Version getAppropriateVersion(Resource resource, Date date,
            Session session) throws RepositoryException {

        String path = resource.getPath();
        List<Version> versions = findAllVersions(path, session);
        Collections.sort(versions, (v1, v2) -> {
            try {
                return v2.getCreated().compareTo(v1.getCreated());
            } catch (RepositoryException e) {
                return 0;
            }
        });
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
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

    private List<Version> findAllVersions(String path, Session session)
            throws RepositoryException {
        List<Version> versions = new ArrayList<Version>();

        Node node = session.getNode(path);
        if (node.hasNode(NameConstants.NN_CONTENT)) {
            Node contentNode = node.getNode(NameConstants.NN_CONTENT);
            if (contentNode.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                versions =   getVersions(contentNode.getPath(), session);
            } else if (node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                versions = getVersions(path, session);
            }
        }

        return versions;
    }

    private List<Version> getVersions(String nodePath, Session session) throws RepositoryException {
        List<Version> versions = new ArrayList<Version>();

        for (VersionIterator iter = session.getWorkspace()
                .getVersionManager().getVersionHistory(nodePath)
                .getAllVersions(); iter.hasNext();) {
            Version v = iter.nextVersion();
            versions.add(v);
        }

        return versions;
    }

    private String getNormalizedPath(String path) {
        String root = path;
        if (root == null || "".equals(root)) {
            return null;
        }
        while (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }

        if (root.length() == 0) {
            root = "/";
        }

        return root;
    }

}
