/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.httpcache.invalidator.event;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.httpcache.invalidator.CacheInvalidationJobConstants;

/**
 * Sample http cache invalidation job creator. Creates invalidation job when there is a change in JCR repository. Cache
 * invalidation events could be sling event handlers, sling filter to trap replication events, workflow steps, etc.
 * Invalidation event creates and starts Invalidation jobs. <p> This is modeled as a sling observer which listens
 * to any change in paths (by default) -- /content, /etc and creates an invalidation job. The intention of this
 * invalidation job is that whenever there is any change in the above said paths, the configured cache needs to be
 * invalidated. </p>
 */
// @formatter:off
@Component(label = "ACS AEM Commons - HTTP Cache - JCR node change invalidator",
           description = "Watches for the configured JCR paths and triggers cache invalidation job.",
           metatype = true,
           policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(label = "JCR paths to watch for changes. (legacy)",
                  description = "Paths expressed in LDAP syntax. Example: (|(path=/content*)(path=/etc*))"
                          + " - Watches for changes under /content or /etc. This is deprecated - consider "
                          + "using " + ResourceChangeListener.PATHS + " instead for better performance.",
                  name = EventConstants.EVENT_FILTER),
        @Property(label = "JCR paths to watch for changes.",
                  value = {"/content", "/etc"},
                  description = "List of paths to watch. Entries with the 'glob:' prefix are interpreted as globs, "
                          + "i.e. the * and ** wildcards are supported.",
                  name = ResourceChangeListener.PATHS),
        @Property(label = "Type of change to listen to",
        value = {"ADDED", "REMOVED","CHANGED"},
        name = ResourceChangeListener.CHANGES),
        @Property(name = "webconsole.configurationFactory.nameHint",
                    value = "JCR paths to watch for changes: {" + EventConstants.EVENT_FILTER + "} "
                            + "{" + ResourceChangeListener.PATHS + "}",
                    propertyPrivate = true)
})
// We do _not_ register as a service. We register ourselves manually as either an EventHandler or
// a ResourceChangeListener based on what config we have.
// @formatter:on
public class JCRNodeChangeEventHandler implements EventHandler, ResourceChangeListener, ExternalResourceChangeListener {
    private static final Logger log = LoggerFactory.getLogger(JCRNodeChangeEventHandler.class);

    @Reference
    private JobManager jobManager;
    
    private ServiceRegistration<?> registration;
    
    @Activate
    @SuppressWarnings({"squid:S1149","deprecation"})
    protected void activate(BundleContext context, Map<String, Object> config) {
        String pathFilter = PropertiesUtil.toString(config.get(EventConstants.EVENT_FILTER), "");
        if (!pathFilter.isEmpty()) {
            log.warn("LDAP-style path filter detected, so a legacy event-based listener will be registered. "
                    + "Consider using a list of paths instead to improve performance.");
            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put(EventConstants.EVENT_TOPIC, new String[] { SlingConstants.TOPIC_RESOURCE_CHANGED,
                    SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants.TOPIC_RESOURCE_REMOVED });
            properties.put(EventConstants.EVENT_FILTER, pathFilter);
            registration = context.registerService(EventHandler.class, this, properties);
        } else {
            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put(ResourceChangeListener.PATHS, config.get(ResourceChangeListener.PATHS));
            properties.put(ResourceChangeListener.CHANGES, config.get(ResourceChangeListener.CHANGES));
            registration = context.registerService(ResourceChangeListener.class, this, properties);
        }
    }
    
    @Deactivate
    protected void deactivate() {
        registration.unregister();
    }

    @Override
    public void handleEvent(final Event event) {
        // Get the required information from the event.
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        handlePath(path);
    }
    
    @Override
    public void onChange(List<ResourceChange> changes) {
        for (ResourceChange change : changes) {
            handlePath(change.getPath());
        }
    }
    
    private void handlePath(String path) {
        final Map<String, Object> payload = Collections.singletonMap(CacheInvalidationJobConstants.PAYLOAD_KEY_DATA_CHANGE_PATH, path);
        jobManager.addJob(CacheInvalidationJobConstants.TOPIC_HTTP_CACHE_INVALIDATION_JOB, payload);

        log.debug("New invalidation job created with the payload path. - {}", path);
    }
}
