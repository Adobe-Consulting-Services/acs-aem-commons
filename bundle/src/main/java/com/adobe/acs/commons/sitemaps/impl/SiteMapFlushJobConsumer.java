package com.adobe.acs.commons.sitemaps.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.adobe.acs.commons.sitemaps.SiteMapGenerator;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
@Component(label = "ACS AEM Commons - Job consumer to clear sitemap cache", immediate=true)
@Service
@Properties({
    @Property(name = JobConsumer.PROPERTY_TOPICS, value = SiteMapFlushJobConsumer.TOPIC)
})
public class SiteMapFlushJobConsumer implements JobConsumer {
    public static final String TOPIC = "com/adobe/acs/commons/sitemaps/flushjob";
    private static final Logger log = LoggerFactory
            .getLogger(SiteMapFlushJobConsumer.class); 
    
    
    @Reference
    private DispatcherFlusher dispatcherFlusher;
    
    @Reference
    private ResourceResolverFactory resolverFactory;
    
    @Reference(name = "siteMapGenerators", referenceInterface = SiteMapGenerator.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<String, SiteMapGenerator> siteMapGenerators = new TreeMap<String, SiteMapGenerator>();
   
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
