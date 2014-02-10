/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.configuration.osgi.impl;

import com.adobe.acs.commons.configuration.osgi.OsgiConfigConstants;
import com.adobe.acs.commons.configuration.osgi.OsgiConfigHelper;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.jcrclustersupport.ClusterAware;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
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
import java.util.HashMap;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Sling OSGi Config Event Handler",
        description = "Sling event handler used to transform Web UI-editable OSGi configs into sling:OsgiConfig nodes"
                + " that are picked up and installed via the Sling JCR Installer tooling.",
        policy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        metatype = true)
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
public class OsgiConfigEventHandler implements JobProcessor, EventHandler, ClusterAware {
    private static final Logger log = LoggerFactory.getLogger(OsgiConfigEventHandler.class);

    @Reference
    private OsgiConfigHelper osgiConfigHelper;

    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private QueryBuilder queryBuilder;


    private static final boolean DEFAULT_PROCESS_ON_ACTIVATE = false;
    private boolean processOnActivate = DEFAULT_PROCESS_ON_ACTIVATE;
    @Property(label = "Process on activate",
            description = "Search and reprocess all authorable sling:OsgiConfig sources upon activation of this OSGi "
                    + "Component",
            boolValue = DEFAULT_PROCESS_ON_ACTIVATE)
    public static final String PROP_PROCESS_ON_ACTIVATE = "process.on-activate";

    private boolean isMaster = false;

    @Override
    public final void handleEvent(Event event) {
        if(this.isMaster) {
            // Only process the job on master instances
            JobUtil.processJob(event, this);
        }
        return;
    }

    @Override
    public final boolean process(final Event event) {
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

        if(this.manageOsgiConfigs(path, event.getTopic())) {
            log.error("Unable to process OSGi Configuration for: {}", path);
        }

        return true;
    }

    private boolean manageOsgiConfigs(final String path, final String topic) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            if (!StringUtils.equals(topic, SlingConstants.TOPIC_RESOURCE_REMOVED)
                    && !StringUtils.equals(resourceResolver.getResource(path).adaptTo(ValueMap.class)
                    .get(JcrConstants.JCR_PRIMARYTYPE, String.class), OsgiConfigConstants.NT_SLING_OSGICONFIG)) {
                // Add or config event, but resource type is no sling:OsgiConfig
                return false;
            }

            final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            final Page page = pageManager.getContainingPage(path);

            /* resource is not under a page so ignore */
            if (page == null) {
                return false;
            }

            final Session session = resourceResolver.adaptTo(Session.class);

            final String targetPath = page.getProperties().get(OsgiConfigConstants.PN_TARGET_CONFIG, String.class);

            /* Page is not configured to support Osgi Configuring */
            if (StringUtils.isBlank(targetPath)) {
                return false;
            }

            if (StringUtils.equals(topic,
                    SlingConstants.TOPIC_RESOURCE_REMOVED)) {

                /* Resource Deleted */

                final Resource targetResource = resourceResolver.getResource(targetPath);

                for (final Resource child : targetResource.getChildren()) {
                    final ValueMap properties = child.adaptTo(ValueMap.class);
                    final String src = properties.get(OsgiConfigConstants.PN_CONFIGURATION_SRC, String.class);

                    if (StringUtils.equals(src, path)) {
                        log.debug("Deleting OSGi Config at: {}", child.getPath());
                        child.adaptTo(Node.class).remove();
                    }
                }

                session.save();
            } else {

                /* Resource created or modified */

                final Resource resource = resourceResolver.getResource(path);
                final ValueMap properties = resource.adaptTo(ValueMap.class);

                if (StringUtils.isBlank(properties.get(OsgiConfigConstants.PN_PID, String.class))) {
                    log.debug("Ignoring. {} property not set for {}", OsgiConfigConstants.PN_PID, path);
                    return false;
                } else if (!osgiConfigHelper.hasRequiredProperties(resource,
                        properties.get(OsgiConfigConstants.PN_REQUIRED_PROPERTIES, new String[]{}))) {
                    log.debug("Missing required properties {}", path);
                    return false;
                }

                log.debug("Creating/updating OSGi Config at [ {} ] from [ {} ]",
                        targetPath + "/" + osgiConfigHelper.getPID(resource),
                        path);

                final Node node = JcrUtil.copy(session.getNode(path),
                        session.getNode(targetPath),
                        osgiConfigHelper.getPID(resource));

                JcrUtil.setProperty(node, OsgiConfigConstants.PN_CONFIGURATION_SRC, path);

                // Remove unnecessary properties from the "real" sling:OsgiConfig node
                JcrUtil.setProperty(node, JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, null);
                JcrUtil.setProperty(node, OsgiConfigConstants.PN_CONFIGURATION_TYPE, null);
                JcrUtil.setProperty(node, OsgiConfigConstants.PN_PID, null);

                // Going directly to node as JCRUtil does not handle removal of String[] properties as expected
                node.setProperty(OsgiConfigConstants.PN_REQUIRED_PROPERTIES, (String[]) null);

                session.save();
            }
        } catch (LoginException e) {
            log.error(e.getMessage());
            return false;
        } catch (PathNotFoundException e) {
            log.error(e.getMessage());
            return false;
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return false;
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

        return true;
    }

    @Activate
    protected void activate(final Map<String, Object> config) {
        log.debug("Activating OSGi Config Event Handler");

        final String searchPath = "/etc";

        processOnActivate = PropertiesUtil.toBoolean(config.get(PROP_PROCESS_ON_ACTIVATE),
                DEFAULT_PROCESS_ON_ACTIVATE);

        if(!this.isMaster) {
            log.info("Non-master instance. Do not run process on activate routine.");

            return;
        } else if(processOnActivate) {

            log.info("Processing any existing author-able OSGi Configurations on activation under: {}", searchPath);

            final Map<String, String> map = new HashMap<String, String>();
            map.put("path", searchPath);
            map.put("type", OsgiConfigConstants.NT_SLING_OSGICONFIG);
            map.put("property", OsgiConfigConstants.PN_PID);
            map.put("property.operation", "exists");

            map.put("p.offset", "0");
            map.put("p.limit", "-1");

            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                final Session adminSession = resourceResolver.adaptTo(Session.class);

                final Query query = queryBuilder.createQuery(PredicateGroup.create(map), adminSession);

                final SearchResult result = query.getResult();

                for (final Hit hit : result.getHits()) {
                    // Use topic "Added" as it is unknown if this was a modification or add.
                    // Add and modified are handled the same; so selection of add over modified is arbitrary.
                    // It is known this is not a Delete event since the resource exists.
                    // Delete events are not handled by the activate process.
                    if(this.manageOsgiConfigs(hit.getPath(),
                            org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_ADDED)) {
                        log.info("(Re)Registered configuration for {}", hit.getPath());
                    }
                }
            } catch (LoginException e) {
                log.error(e.getMessage());
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }
    }

    @Override
    public void bindRepository(final String respositoryID, final String clusterID, final boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public void unbindRepository() {
        this.isMaster = false;
    }
}