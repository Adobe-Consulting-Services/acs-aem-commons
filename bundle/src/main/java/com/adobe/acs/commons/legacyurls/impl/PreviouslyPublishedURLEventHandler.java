package com.adobe.acs.commons.legacyurls.impl;


import com.adobe.acs.commons.legacyurls.PreviouslyPublishedURLManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Map;


@Component(
        label = "ACS AEM Commons - Previously Published URL Handler",
        description = "",
        immediate = true,
        metatype = false
)
@Properties({
        @Property(
                label = "Event Topics",
                value = {
                        org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_ADDED,
                        org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_CHANGED,
                        org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_REMOVED
                },
                description = "[Required] Sling Event Topics this event handler will to respond to.",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        ),
        @Property(
                label = "Event Filters",
                value =   "(&(" + "legacyURLs" + "=*))",
                description = "[Optional] Event Filters used to further restrict this event handler; Uses LDAP expression against event properties.",
                name = EventConstants.EVENT_FILTER,
                propertyPrivate = true
        )
})
@Service
public class PreviouslyPublishedURLEventHandler implements JobProcessor, EventHandler {
    private static final Logger log = LoggerFactory.getLogger(PreviouslyPublishedURLEventHandler.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ResourceResolver adminResourceResolver;


    @Reference
    private PreviouslyPublishedURLManager previouslyPublishedURLManager;

    @Override
    public void handleEvent(Event event) {
        JobUtil.processJob(event, this);
    }

    @Override
    public boolean process(Event event) {

        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        final Resource resource = adminResourceResolver.getResource(path);

        if(resource == null) {
            log.warn("Could process previously published URL for [ {} ]", path);
        }

        final ValueMap properties = resource.adaptTo(ValueMap.class);
        String[] urls = properties.get("urls", String[].class);

        try {

            if(urls == null) {
                return true;
            } else if(StringUtils.equals(org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_ADDED, event.getTopic())) {
                previouslyPublishedURLManager.create(adminResourceResolver, path, urls);
            } else if(StringUtils.equals(org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_CHANGED, event.getTopic())) {
                previouslyPublishedURLManager.update(adminResourceResolver, path, urls);
            } else if(StringUtils.equals(org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_REMOVED, event.getTopic())) {
                previouslyPublishedURLManager.delete(adminResourceResolver, path);
            }

        } catch (RepositoryException e) {
            return false;
        }

        return true;
    }

    protected void activate(Map<String, Object> config) {
        try {
            adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    protected void deactivate(Map<String, Object> config) {
        if(adminResourceResolver != null) {
            adminResourceResolver.close();
        }
    }
}