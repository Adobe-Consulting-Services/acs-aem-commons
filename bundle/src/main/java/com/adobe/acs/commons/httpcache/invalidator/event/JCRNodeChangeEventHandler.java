package com.adobe.acs.commons.httpcache.invalidator.event;

import com.adobe.acs.commons.httpcache.invalidator.CacheInvalidationJobConstants;
import org.apache.felix.scr.annotations.*;
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
 * to any change in paths -- /content, /etc and creates an invalidation job. The intention of this invalidation job is
 * that whenever there is any change in the above said paths the configured cache needs to be invalidated. </p>
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Invalidation job creator on JCR node change",
           description = "Creates http cache invalidation job on JCR node / property change.",
           metatype = true,
           immediate = true,
           policy = ConfigurationPolicy.REQUIRE)
@Properties({@Property(label = "Event Topics",
                       value = {SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_ADDED,
                               SlingConstants.TOPIC_RESOURCE_REMOVED},
                       description = "This handler responds to resource modification event.",
                       name = EventConstants.EVENT_TOPIC,
                       propertyPrivate = true),

                    @Property(label = "Event Filters",
                              value = "(|(" + SlingConstants.PROPERTY_PATH + "=" +
                                      "/content*)(" + SlingConstants.PROPERTY_PATH + "=" + "/etc*))",
                              description = "Event filter announcing that this is " +
                                      "interested only for the MPAS " + "products parent " +
                                      "node.",
                              name = EventConstants.EVENT_FILTER,
                              propertyPrivate = true)})
@Service
public class JCRNodeChangeEventHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(JCRNodeChangeEventHandler.class);

    @Reference
    private JobManager jobManager;

    @Override
    public void handleEvent(final Event event) {
        // Get the required information from the event.
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        // Create the required payload.
        final Map<String, Object> payload = new HashMap<>();
        payload.put(CacheInvalidationJobConstants.PAYLOAD_KEY_DATA_CHANGE_PATH, path);
        // Start a job.
        jobManager.addJob(CacheInvalidationJobConstants.TOPIC_HTTP_CACHE_INVALIDATION_JOB, payload);
        log.debug("New invalidation job created with the payload path. - {}", path);
    }
}
