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
/**
 * This has to be only run on author. Please add the osgi config node only to config.author
 */
package com.adobe.acs.commons.sitemaps.impl;

import static com.adobe.acs.commons.sitemaps.impl.SiteMapConstants.EVENT_ACTION;

import java.util.HashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.jcrclustersupport.ClusterAware;
import com.day.cq.replication.ReplicationAction;

@Component(label = " ACS AEM Commons - page replication event listener to clear sitemap cache", immediate=true,policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = EventConstants.EVENT_TOPIC, value = ReplicationAction.EVENT_TOPIC , propertyPrivate = true)})
public class PageReplicationEventListener implements EventHandler,ClusterAware {
    private static final Logger log = LoggerFactory
            .getLogger(PageReplicationEventListener.class);
    private static final String TOPIC = "com/adobe/acs/commons/sitemaps/flushjob";
    private boolean isMaster = Boolean.FALSE;
  
    @Reference
    private JobManager jobManager;
    

    
    @Override
    public void handleEvent(Event event) {
        if(isMaster){
            ReplicationAction action = ReplicationAction.fromEvent(event);
            ValueMap jobProperties = getJobProperties(action);
            Job siteMapGenerationJob = jobManager.addJob(TOPIC, null, jobProperties);
            log.debug("event offloaded to the sitemap flush job"+siteMapGenerationJob.getId());
        }
    }



    @Override
    public void bindRepository(String repositoryId, String clusterId, boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public void unbindRepository() {
       isMaster = Boolean.FALSE;
    }
    private ValueMap getJobProperties(ReplicationAction action){
        ValueMap jobProperties = new ValueMapDecorator(new HashMap<String, Object>());
        jobProperties.put(EVENT_ACTION, action);
        return jobProperties;
    }
}
