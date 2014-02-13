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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobConsumer.JobResult;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.adobe.acs.commons.sitemaps.SiteMapGenerator;
import com.day.cq.jcrclustersupport.ClusterAware;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;

@Component(label = " ACS AEM Commons - page replication event listener to clear sitemap cache", immediate=true,policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = EventConstants.EVENT_TOPIC, value = ReplicationAction.EVENT_TOPIC , propertyPrivate = true),
    @Property(name = JobConsumer.PROPERTY_TOPICS, value = PageReplicationEventListener.TOPIC)})
public class PageReplicationEventListener implements EventHandler,ClusterAware,JobConsumer {
    private static final Logger log = LoggerFactory
            .getLogger(PageReplicationEventListener.class);
    public static final String TOPIC = "com/adobe/acs/commons/sitemaps/flushjob";
    
    @Property(name =SiteMapConstants.SITE_ROOT_PATH, label="Root Path", description="Site Root path for sitemap", propertyPrivate = false, value = "/content/geometrixx/en")
    public static final String SITE_ROOT_PATH = SiteMapConstants.SITE_ROOT_PATH;
    
    private boolean isMaster = Boolean.FALSE;
    
    @Reference
    private DispatcherFlusher dispatcherFlusher;
    
    @Reference
    private ResourceResolverFactory resolverFactory;
    
    @Reference(name = "siteMapGenerators", referenceInterface = SiteMapGenerator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<String, SiteMapGenerator> siteMapGenerators = new TreeMap<String, SiteMapGenerator>();
    
    @Reference
    private JobManager jobManager;
    
    
    protected void bindSiteMapGenerators(
            final SiteMapGenerator siteMapGenerator,
            final Map<String, Object> props) {
        synchronized (this.siteMapGenerators) {
            this.siteMapGenerators.put(PropertiesUtil.toString(
                    props.get(SiteMapConstants.SITE_ROOT_PATH), "/content/geometrixx/en"),
                    siteMapGenerator);
        }
    }

    protected void unbindSiteMapGenerators(
            final SiteMapGenerator siteMapGenerator,
            final Map<String, Object> props) {
        synchronized (this.siteMapGenerators) {
            this.siteMapGenerators.remove(PropertiesUtil.toString(
                    props.get(SiteMapConstants.SITE_ROOT_PATH),"/content/geometrixx/en"));
        }
    }
    
    
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
    public JobResult process(Job job) {
        JobResult result = JobResult.FAILED;
        try {
        ReplicationAction action = getReplicationActionFromJob(job);
        String[] paths = action.getPaths();
        //in case same replication action has multiple sites but since dispatcher.flush never cares about multiple domain, we dont need this code.
        //shall remove once reviewed.
        Set<String> tobeFlushedDomains = getListOfDomains(paths, action);
        if(tobeFlushedDomains.size()>0){
            flushSiteMapXml(action);
       }
        result = JobResult.ASYNC;
        } catch (LoginException e) {
           log.error(e.getMessage(),e);
           result =  cancelJob();
        } catch (ReplicationException e) {
            log.error(e.getMessage(),e);
            result =  cancelJob();
        }
        return result;
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
    private ReplicationAction getReplicationActionFromJob(Job job){
        ReplicationAction action = (ReplicationAction) job.getProperty(SiteMapConstants.EVENT_ACTION);
        return action;
    }
    private Set<String> getListOfDomains(String[] paths , ReplicationAction action){
        Set<String> tobeFlushedDomains =  new HashSet<String>();
        for(String path :paths){
            for(String key : siteMapGenerators.keySet()){
                if(path.startsWith(key) &&!tobeFlushedDomains.contains(key)){
                   tobeFlushedDomains.add(key);
                }
            }
        }
        return tobeFlushedDomains;
    }
    
    private void flushSiteMapXml(ReplicationAction action) throws LoginException, ReplicationException{
        ResourceResolver resolver = getResolver(action.getUserId());
        dispatcherFlusher.flush(resolver,ReplicationActionType.ACTIVATE, false, DispatcherFlushFilter.RESOURCE_ONLY, SiteMapConstants.SITEMAP_REQUEST_URL);
        
    }
    private ResourceResolver getResolver(String userId) throws LoginException{
               
        HashMap<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(ResourceResolverFactory.USER_IMPERSONATION, userId);
        return resolverFactory.getAdministrativeResourceResolver(authInfo);
 
    }
    private JobResult cancelJob(){
        return JobResult.CANCEL;
    }
}
