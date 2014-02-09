/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.configuration.impl;

import com.adobe.acs.commons.configuration.OsgiConfigConstants;
import com.adobe.acs.commons.configuration.OsgiConfigHelper;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(
        label = "ACS AEM Commons - Sling OSGi Config Event Listener",
        description = "",
        immediate = true,
        metatype = false)
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
        )
})
@Service
public class OsgiConfigEventHandler implements JobProcessor, EventHandler {
    private static final Logger log = LoggerFactory.getLogger(OsgiConfigEventHandler.class);

    @Reference
    private OsgiConfigHelper osgiConfigiHelper;

    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public final void handleEvent(Event event) {
        JobUtil.processJob(event, this);
        return;
    }

    @Override
    public final boolean process(final Event event) {
        // Resource path "undergoing" the event
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            if(!StringUtils.equals(event.getTopic(), SlingConstants.TOPIC_RESOURCE_REMOVED)
                    && !StringUtils.equals(resourceResolver.getResource(path).adaptTo(ValueMap.class).get("jcr:primaryType", ""), "sling:OsgiConfig")) {
                // Add or config event, but resource type is no sling:OsgiConfig
                return true;
            }

            final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            final Page page = pageManager.getContainingPage(path);

            /* resource is not under a page so ignore */
            if(page == null) { return true; }

            final Session session = resourceResolver.adaptTo(Session.class);

            final String targetPath = page.getProperties().get(OsgiConfigConstants.PN_TARGET_CONFIG, String.class);

            /* Page is not configured to support Osgi Configuring */
            if(StringUtils.isBlank(targetPath)) { return true; }


            if(StringUtils.equals(event.getTopic(),
                    org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_REMOVED)) {

                /* Resource Deleted */

                final Resource targetResource = resourceResolver.getResource(targetPath);

                for(final Resource child : targetResource.getChildren()) {
                    final ValueMap properties = child.adaptTo(ValueMap.class);
                    final String src = properties.get(OsgiConfigConstants.PN_CONFIGURATION_SRC, String.class);

                    if(StringUtils.equals(src, path)) {
                        log.debug("Deleting OSGi Config at: {}", child.getPath());
                        child.adaptTo(Node.class).remove();
                    }
                }

                session.save();

            } else {

                /* Resource created or modified */

                final Resource resource = resourceResolver.getResource(path);
                final ValueMap properties = resource.adaptTo(ValueMap.class);

                if(StringUtils.isBlank(properties.get(OsgiConfigConstants.PN_PID, String.class))) {
                    log.debug("Ignoring. {} property not set for {}", OsgiConfigConstants.PN_PID, path);
                    return true;
                } else if(!osgiConfigiHelper.hasRequiredProperties(resource,
                        properties.get(OsgiConfigConstants.PN_REQUIRED_PROPERTIES, new String[]{ }))) {
                    log.debug("Missing required properties {}", path);
                    return true;
                }

                log.debug("Creating/updating OSGi Config at [ {} ] from [ {} ]",
                        targetPath + "/" + osgiConfigiHelper.getPID(resource),
                        path);

                final Node node = JcrUtil.copy(session.getNode(path),
                        session.getNode(targetPath),
                        osgiConfigiHelper.getPID(resource));

                JcrUtil.setProperty(node, OsgiConfigConstants.PN_CONFIGURATION_SRC, path);

                // Remove unnecessary properties from the "real" sling:OsgiConfig node
                JcrUtil.setProperty(node, "sling:resourceType", null);
                JcrUtil.setProperty(node, OsgiConfigConstants.PN_CONFIGURATION_TYPE, null);
                JcrUtil.setProperty(node, OsgiConfigConstants.PN_PID, null);

                // Going directly to node as JCRUtil does not handle removal of String[] properties as expected
                node.setProperty(OsgiConfigConstants.PN_REQUIRED_PROPERTIES, (String[])null);

                session.save();
            }
        } catch (LoginException e) {
            log.error(e.getMessage());
        } catch (PathNotFoundException e) {
            log.error(e.getMessage());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } finally {
            if(resourceResolver != null) {
                resourceResolver.close();
            }
        }

        // Only return false if job processing failed and the job should be rescheduled
        return true;
    }
}