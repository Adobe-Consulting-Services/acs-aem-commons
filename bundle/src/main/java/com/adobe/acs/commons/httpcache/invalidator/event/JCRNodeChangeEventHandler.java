/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.invalidator.event;

import com.adobe.acs.commons.httpcache.invalidator.CacheInvalidationJobConstants;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample http cache invalidation job creator. Creates invalidation job when there is a change in JCR repository. Cache
 * invalidation events could be sling event handlers, sling filter to trap replication events, workflow steps, etc.
 * Invalidation event creates and starts Invalidation jobs. <p> This is modelled as a sling event handler which listens
 * to any change in paths (by default) -- /content, /etc and creates an invalidation job. The intention of this
 * invalidation job is that whenever there is any change in the above said paths, the configured cache needs to be
 * invalidated. </p>
 */
// @formatter:off
@Component(label = "ACS AEM Commons - HTTP Cache - JCR node change invalidator.",
           description = "Watches for the configured JCR paths and triggers cache invalidation job.",
           metatype = true,
           immediate = true,
           policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(label = "Event Topics",
            // TODO: Register a Resource Change Listener instead as per the deprecation notes
            // https://sling.apache.org/apidocs/sling9/org/apache/sling/api/resource/observation/ResourceChangeListener.html
                   value = {SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_ADDED,
                           SlingConstants.TOPIC_RESOURCE_REMOVED},
                   description = "This handler responds to resource modification event.",
                   name = EventConstants.EVENT_TOPIC,
                   propertyPrivate = true),
        @Property(label = "JCR paths to watch for changes.",
                  value = "(|(" + SlingConstants.PROPERTY_PATH + "="
                          + "/content*)(" + SlingConstants.PROPERTY_PATH + "=" + "/etc*))",
                  description = "Paths expressed in LDAP syntax. Example: (|(path=/content*)(path=/etc*))"
                          + " - Watches for changes under /content or /etc. ",
                  name = EventConstants.EVENT_FILTER),
        @Property(name = "webconsole.configurationFactory.nameHint",
                    value = "JCR paths to watch for changes: {" + EventConstants.EVENT_FILTER + "}",
                    propertyPrivate = true)
})
@Service
// @formatter:on
public class JCRNodeChangeEventHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(JCRNodeChangeEventHandler.class);

    @Reference
    private JobManager jobManager;

    @Override
    public void handleEvent(final Event event) {

        // Get the required information from the event.
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        // Create the required payload.
        final Map<String, Object> payload = new HashMap<String, Object>();
        payload.put(CacheInvalidationJobConstants.PAYLOAD_KEY_DATA_CHANGE_PATH, path);
        // Start a job.
        jobManager.addJob(CacheInvalidationJobConstants.TOPIC_HTTP_CACHE_INVALIDATION_JOB, payload);

        log.debug("New invalidation job created with the payload path. - {}", path);
    }
}
