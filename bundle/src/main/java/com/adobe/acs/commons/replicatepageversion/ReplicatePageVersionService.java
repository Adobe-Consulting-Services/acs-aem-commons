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
            Iterator<Resource> resourceIterator, String agent, Date date)
            throws RepositoryException, ReplicationException;

    Version getAppropriateVersion(Resource resource, Date date, Session session)
            throws RepositoryException;

    List<Version> findAllVersions(String path, Session session)
            throws RepositoryException;

}
