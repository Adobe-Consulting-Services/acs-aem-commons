/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageInfoProvider;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PageInfoProvider which indicates that shared component properties
 * are enabled, and for which component types.  Note that this provider
 * requires Page Root Provider to be configured.
 * <p>
 * https://docs.adobe.com/docs/en/cq/5-6-1/developing/pageinfo.html#Creating a Page Information Provider
 */
@org.apache.felix.scr.annotations.Component
@Service(PageInfoProvider.class)
public class SharedComponentPropertiesPageInfoProvider implements PageInfoProvider, EventListener {
    private static final Logger log = LoggerFactory.getLogger(SharedComponentPropertiesPageInfoProvider.class);

    private static final String SERVICE_NAME = "shared-component-props";

    @Reference
    private PageRootProvider pageRootProvider;

    @Reference
    private SharedComponentProperties sharedComponentProperties;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SlingRepository repository;

    @SuppressWarnings("AEM Rules:AEM-3") // used for observation
    private Session respositorySession;

    private ObservationManager observationManager;

    private Map<String, List<Boolean>> componentsWithSharedProperties;

    private long scheduledSharedComponentsMapUpdate = -1L;

    /**
     * Add a "sharedComponentProperties" section to pageInfo so that JS libs in the authoring interface
     * can determine whether or not to enable shared/global properties for a component on a site.
     */
    @Override
    @SuppressWarnings( "deprecation" )
    public void updatePageInfo(SlingHttpServletRequest request, org.apache.sling.commons.json.JSONObject info, Resource resource)
            throws org.apache.sling.commons.json.JSONException {
        if (scheduledSharedComponentsMapUpdate > 0 && System.currentTimeMillis() > scheduledSharedComponentsMapUpdate) {
            scheduledSharedComponentsMapUpdate = -1L;
            updateSharedComponentsMap();
        }

        org.apache.sling.commons.json.JSONObject props = new org.apache.sling.commons.json.JSONObject();
        props.put("enabled", false);

        Page page = pageRootProvider.getRootPage(resource);
        if (page != null) {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            try {
                AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
                Privilege privilegeAddChild = accessControlManager.privilegeFromName("jcr:addChildNodes");
                Privilege privilegeModifyProps = accessControlManager.privilegeFromName("jcr:modifyProperties");
                Privilege[] requiredPrivs = new Privilege[]{privilegeAddChild, privilegeModifyProps};

                if (accessControlManager.hasPrivileges(page.getPath() + "/jcr:content", requiredPrivs)) {
                    props.put("enabled", true);
                    props.put("root", page.getPath());
                    props.put("components", Maps.transformValues(componentsWithSharedProperties, (Function<List<Boolean>, Object>) org.apache.sling.commons.json.JSONArray::new));
                } else {
                    log.debug("User does not have [ {} ] on [ {} ]", requiredPrivs, page.getPath() + "/jcr:content");
                }
            } catch (RepositoryException e) {
                log.error("Unexpected error checking permissions to modify shared component properties", e);
            }
        } else {
            log.debug("No Page Root could be found for [ {} ]", resource.getPath());
        }

        info.put("sharedComponentProperties", props);
    }

    /**
     * Listen for add/update/delete of shared dialog nodes, in order to trigger an update of the
     * map of components that have shared property dialogs.
     *
     * Technically a delete may not be caught if a node higher in the ancestry is deleted (thus
     * deleting its children) but having a stale entry in the map does not cause any problems.
     */
    @Override
    public void onEvent(EventIterator eventIterator) {
        while (eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            try {
                String[] pathPieces = event.getPath().split("/");
                String nodeName = pathPieces[pathPieces.length - 1];
                switch (nodeName) {
                    case "dialogglobal":
                    case "dialogshared":
                    case "dialog_global":
                    case "dialog_shared":
                        scheduleSharedComponentsMapUpdate();
                        break;

                    default:
                        break;
                }
            } catch (Exception e) {
                log.error("Error determining if event affects list of components with shared/global properties", e);
            }
        }
    }

    /**
     * Schedule an update of the map of components with shared/global properties to be updated
     * 5 seconds from now.
     *
     * This handles race conditions where the map calculation happens before all nodes are installed,
     * and also prevents stampedes from multiple JCR update events such as during a package installation.
     */
    private void scheduleSharedComponentsMapUpdate() {
        log.debug("Flagging for rebuild of the map of components with shared properties dialogs");
        scheduledSharedComponentsMapUpdate = System.currentTimeMillis() + 5000;
    }

    /**
     * Traverse the entire set of components in the /apps directory and create a map of all component types
     * that have shared/global config dialogs.
     *
     * This is used by the JS libs in the authoring interface to determine if a component should show the
     * options for editing shared/global configs.
     */
    private void updateSharedComponentsMap() {
        Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo)){
            log.debug("Calculating map of components with shared properties dialogs");

            resourceResolver.refresh();
            ComponentManager componentManager = resourceResolver.adaptTo(ComponentManager.class);
            Map<String, List<Boolean>> localComponentsWithSharedProperties = new HashMap<>();
            for (Component component : componentManager.getComponents()) {
                if (component.getPath().startsWith("/apps")) {
                    boolean hasSharedDialogForTouch = componentHasTouchDialog(component, "dialogshared");
                    boolean hasGlobalDialogForTouch = componentHasTouchDialog(component, "dialogglobal");
                    boolean hasSharedDialogForClassic = componentHasClassicDialog(component, "dialog_shared");
                    boolean hasGlobalDialogForClassic = componentHasClassicDialog(component, "dialog_global");
                    if (hasSharedDialogForTouch || hasGlobalDialogForTouch || hasSharedDialogForClassic || hasGlobalDialogForClassic) {
                        localComponentsWithSharedProperties.put(component.getResourceType(),
                                Arrays.asList(hasSharedDialogForTouch, hasGlobalDialogForTouch, hasSharedDialogForClassic, hasGlobalDialogForClassic));
                    }
                }
            }
            componentsWithSharedProperties = Collections.unmodifiableMap(localComponentsWithSharedProperties);

            log.debug("Calculated map of components with shared properties dialogs: {}", componentsWithSharedProperties);
        } catch (org.apache.sling.api.resource.LoginException e) {
            log.error("Unable to log into service user to determine list of components with shared properties dialogs", e);
        } catch (RepositoryException e) {
            log.error("Unexpected error attempting to determine list of components with shared properties dialogs", e);
        }
    }

    /**
     * Determine if a component has a Classic UI dialog for shared or global configs.
     */
    private boolean componentHasClassicDialog(Component component, String dialogName) throws RepositoryException {
        Resource dialog = component.getLocalResource(dialogName);
        return dialog != null && dialog.adaptTo(Node.class).isNodeType("cq:Dialog");
    }

    /**
     * Determine if a component has a Touch UI dialog for shared or global configs.
     */
    private boolean componentHasTouchDialog(Component component, String dialogName) {
        Resource dialog = component.getLocalResource(dialogName);
        return dialog != null && dialog.isResourceType("cq/gui/components/authoring/dialog");
    }

    @Activate
    public void activate(final Map<String, String> config) throws RepositoryException {
        componentsWithSharedProperties = new HashMap<>();

        // Schedule the initial calculation of components that have shared/global property dialogs.
        scheduleSharedComponentsMapUpdate();

        try {
            // Add an event listener on the /apps directory for component adds/removals to recalculate
            // the set of components that have shared/global property dialogs.
            respositorySession = repository.loginService(SERVICE_NAME, null);
            observationManager = respositorySession.getWorkspace().getObservationManager();

            // Need to listen for "nt:folder" else "nt:unstructured" nodes created/deleted from
            // CRD DE are not captured.
            String[] nodeTypes = {"nt:folder", "nt:unstructured"};
            int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED;
            observationManager.addEventListener(this, eventTypes, "/apps", true, null, nodeTypes, true);

            log.info("Activated JCR event listener for components with shared/global properties");
        } catch (LoginException le) {
            log.error("Could not get an admin resource resolver to listen for components with shared/global properties");
        } catch (Exception e) {
            log.error("Error activating JCR event listener for components with shared/global properties", e);
        }
    }

    @Deactivate
    public void deactivate(final Map<String, String> config) throws RepositoryException {
        scheduledSharedComponentsMapUpdate = -1L;
        try {
            if (observationManager != null) {
                observationManager.removeEventListener(this);
                log.info("Deactivated JCR event listener for components with shared/global properties");
            }
        } catch (RepositoryException re) {
            log.error("Error deactivating JCR event listener for components with shared/global properties", re);
        } finally {
            if (respositorySession != null) {
                respositorySession.logout();
                respositorySession = null;
            }
        }
    }
}