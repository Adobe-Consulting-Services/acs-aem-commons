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


import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.*;
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

    private static final String PN_REQUIRED_PROPERTIES = "acs.requiredProperties";
    private static final String PN_CONFIGURATION_SRC = "acs.configurationSrc";
    private static final String PN_CONFIGURATION_TYPE = "acs.configurationType";
    private static final String PN_TARGET_CONFIG = "acs.targetConfig";
    private static final String PN_PID = "acs.pid";

    // EventAdmin is used to manually trigger other events
    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void handleEvent(Event event) {
        JobUtil.processJob(event, this);
        return;
    }

    @Override
    public boolean process(final Event event) {
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

            final String targetPath = page.getProperties().get(PN_TARGET_CONFIG, String.class);

            /* Page is not configured to support Osgi Configuring */
            if(StringUtils.isBlank(targetPath)) { return true; }


            if(StringUtils.equals(event.getTopic(),
                    org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_REMOVED)) {

                /* Resource Deleted */

                final Resource targetResource = resourceResolver.getResource(targetPath);

                for(final Resource child : targetResource.getChildren()) {
                    final ValueMap properties = child.adaptTo(ValueMap.class);
                    final String src = properties.get(PN_CONFIGURATION_SRC, String.class);

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
                final String osgiConfigPid = properties.get(PN_PID, String.class);

                final String configurationType = properties.get(PN_CONFIGURATION_TYPE, "single");

                if(StringUtils.isBlank(osgiConfigPid)) {
                    log.debug("Ignoring. {} property not set for {}", PN_PID, path);
                    return true;
                } else if(!this.hasRequiredProperties(resource,
                        properties.get(PN_REQUIRED_PROPERTIES, new String[]{}))) {
                    log.debug("Missing required properties {}", path);
                    return true;
                }

                log.debug("Creating/updating OSGi Config at [ {} ] from [ {} ]",
                        targetPath + "/" + osgiConfigPid,
                        path + this.getPostFixPath(path, configurationType));

                final Node node = JcrUtil.copy(session.getNode(path),
                        session.getNode(targetPath),
                        osgiConfigPid + this.getPostFixPath(path, configurationType));

                JcrUtil.setProperty(node, PN_CONFIGURATION_SRC, path);

                // Remove unnecessary properties from the "real" sling:OsgiConfig node
                JcrUtil.setProperty(node, "sling:resourceType", null);
                JcrUtil.setProperty(node, PN_REQUIRED_PROPERTIES, null);
                JcrUtil.setProperty(node, PN_CONFIGURATION_TYPE, null);
                JcrUtil.setProperty(node, PN_PID, null);

                session.save();
            }

        } catch (LoginException e) {
            log.error(e.getMessage());
            e.printStackTrace();
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

    private String getPostFixPath(final String path, final String configurationType) {
        if(StringUtils.equalsIgnoreCase("factory", configurationType)) {
            return "-" + DigestUtils.md5Hex(path);
        } else {
            return "";
        }
    }

    private boolean hasRequiredProperties(final Resource resource,
                                          final String[] requiredProperties) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        for(final String requiredProperty : requiredProperties) {
            final String tmp = properties.get(requiredProperty, String.class);
            if(StringUtils.isBlank(tmp)) {
                return false;
            }
        }

        return true;
    }
}