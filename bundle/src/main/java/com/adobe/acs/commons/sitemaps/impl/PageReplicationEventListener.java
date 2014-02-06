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
package com.adobe.acs.commons.sitemaps.impl;

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
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.adobe.acs.commons.sitemaps.SiteMapGenerator;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationException;
@Component(label = "page replication event listener to clear sitemap cache", immediate=true,policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = "event.topics", value = ReplicationAction.EVENT_TOPIC , propertyPrivate = true)})
public class PageReplicationEventListener implements EventHandler {
    private static final Logger log = LoggerFactory
            .getLogger(PageReplicationEventListener.class);
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
    
    @Reference
    private DispatcherFlusher dispatcherFlusher;
    
    @Reference
    private ResourceResolverFactory resolverFactory;
    
    @Override
    public void handleEvent(Event event) {
        ReplicationAction action = ReplicationAction.fromEvent(event);
        String[] paths = action.getPaths();
        //in case same replication action has multiple sites
        Set<String> tobeFlushedDomains = getListOfDomains(paths, action);
        for(String key : tobeFlushedDomains){
            flushSiteMapXml(action);
        }
        
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
    
    private void flushSiteMapXml(ReplicationAction action){
        ResourceResolver resolver = getResolver(action.getUserId());
        try {
            dispatcherFlusher.flush(resolver, action.getType(), false, DispatcherFlushFilter.RESOURCE_ONLY, "/sitemap");
        } catch (ReplicationException e) {
           log.error(e.getMessage(),e);
        }
    }
    private ResourceResolver getResolver(String userId){
        try {        
        HashMap<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(ResourceResolverFactory.USER_IMPERSONATION, userId);
        return resolverFactory.getAdministrativeResourceResolver(authInfo);
        
    } catch (LoginException e) {
       log.error(e.getMessage(),e);
    } 
    return null;
    }

}
