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
package com.adobe.acs.commons.replicatepageversion.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.apache.sling.commons.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replicatepageversion.ReplicatePageVersionService;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.AgentIdFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;

@Component(
        label = "ACS AEM Commons - replicate specific version of a resource tree",
        description = "Service used to replicate specific version of a resource tree through a "
                + "specific replication agent", immediate = false,
        metatype = false)
@Service
public class ReplicatePageVersionServiceImpl implements
        ReplicatePageVersionService {

    private static final Logger log = LoggerFactory
            .getLogger(ReplicatePageVersionServiceImpl.class);

    @Reference
    private Replicator replicator;

    @Override
    public final Map<String, String> locateVersionAndReplicateResource(
            ResourceResolver resolver, String[] rootPaths, String[] agents,
            Date date) {
        Map<String, String> map = new HashMap<String, String>();
        List<Resource> resources = null;
        Iterator<Resource> resourceIterator = null;
        boolean error = false;
        String message = "";
        try {
            if (rootPaths != null && rootPaths.length > 0) {

                for (int k = 0; k < rootPaths.length; k++) {
                    resources = getResources(resolver,
                            getNormalizedPath(rootPaths[k]));
                    resourceIterator = resources.iterator();

                    replicateResource(resolver, resourceIterator, agents, date);

                    resources = null;
                }

            }

            map.put("status", "replicated");

        } catch (RepositoryException e) {
            error = true;
            log.error("replication failed", e);
            message = e.getMessage();
        } catch (ReplicationException e) {
            error = true;
            log.error("replication failed", e);
            message = e.getMessage();
        } finally {
            if (error) {

                map.put("error", message);
                map.put("status", "error");

            }
        }

        return map;
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
        if (!node.isNodeType("nt:hierarchyNode")) {
            return;
        }
        resources.add(res);
        Iterator<Resource> iter = resolver.listChildren(res);
        while (iter.hasNext()) {
            buildResourceList(resolver, iter.next(), resources);
        }
    }

    private void replicateResource(ResourceResolver resolver,
            Iterator<Resource> resourceIterator, String[] agents, Date date)
            throws RepositoryException, ReplicationException {
        Session session = resolver.adaptTo(Session.class);
        Resource resource = null;
        Version v = null;
        ReplicationOptions opts = new ReplicationOptions();

        AgentIdFilter agentFilter = new AgentIdFilter(agents);
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

    private Version getAppropriateVersion(Resource resource, Date date,
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

    private List<Version> findAllVersions(String path, Session session)
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
            } else if (node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                VersionIterator iter = session.getWorkspace()
                        .getVersionManager().getVersionHistory(node.getPath())
                        .getAllVersions();
                while (iter.hasNext()) {
                    Version v = iter.nextVersion();
                    versions.add(v);
                }
            }
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
