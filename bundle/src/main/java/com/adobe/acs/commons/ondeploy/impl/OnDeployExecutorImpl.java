/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.ondeploy.impl;

import com.adobe.acs.commons.ondeploy.OnDeployExecutor;
import com.adobe.acs.commons.ondeploy.OnDeployScriptProvider;
import com.adobe.acs.commons.ondeploy.scripts.OnDeployScript;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A service that triggers scripts on deployment to an AEM server.
 * <p>
 * This class manages scripts so that they only run once (unless the script
 * fails).  Script execution statuses are stored in the JCR @
 * /var/acs-commons/on-deploy-scripts-status.
 * <p>
 * Scripts are specified via OSGi config, are are run in the order specified.
 * <p>
 * NOTE: Since it's always a possibility that
 * /var/acs-commons/on-deploy-scripts-status will be deleted in the JCR,
 * scripts should be written defensively in case they are actually run more
 * than once.  This also covers the scenario where a script is run a second
 * time after failing the first time.
 */
@Component(
        label = "ACS AEM Commons - On-Deploy Scripts Executor",
        description = "Developer tool that triggers scripts (specified via an implementation of OnDeployScriptProvider) to execute on deployment.",
        immediate = true,
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Service
public class OnDeployExecutorImpl implements OnDeployExecutor {
    private static final String SCRIPT_DATE_END = "endDate";
    private static final String SCRIPT_DATE_START = "startDate";
    private static final String SCRIPT_STATUS = "status";
    private static final String SCRIPT_STATUS_JCR_FOLDER = "/var/acs-commons/on-deploy-scripts-status";
    private static final String SCRIPT_STATUS_FAIL = "fail";
    private static final String SCRIPT_STATUS_RUNNING = "running";
    private static final String SCRIPT_STATUS_SUCCESS = "success";

    private static final String SERVICE_NAME = "on-deploy-scripts";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;
    @Reference(name = "scriptProvider", referenceInterface = OnDeployScriptProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private List<OnDeployScriptProvider> scriptProviders;

    @Activate
    protected void activate() {
        // noop
    }

    protected void bindDynamicClassLoaderManager(DynamicClassLoaderManager dynamicClassLoaderManager) {
        this.dynamicClassLoaderManager = dynamicClassLoaderManager;
    }

    protected void bindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = resourceResolverFactory;
    }

    /**
     * Executes all on-deploy scripts on bind of a script provider.
     */
    protected void bindScriptProvider(OnDeployScriptProvider scriptProvider) {
        logger.info("Executing on-deploy scripts from scriptProvider: {}", scriptProvider.getClass().getName());
        List<OnDeployScript> scripts = scriptProvider.getScripts();
        if (scripts.size() == 0) {
            logger.debug("No on-deploy scripts found.");
            return;
        }
        ResourceResolver resourceResolver = null;
        Session session = null;
        try {
            resourceResolver = logIn();
            session = resourceResolver.adaptTo(Session.class);
            runScripts(resourceResolver, session, scripts);
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception e) {
                    logger.warn("Failed session.logout()", e);
                }
            }
            if (resourceResolver != null) {
                try {
                    resourceResolver.close();
                } catch (Exception e) {
                    logger.warn("Failed resourceResolver.close()", e);
                }
            }
        }
    }

    protected void unbindScriptProvider(OnDeployScriptProvider scriptProvider) {
        // noop
    }

    protected Node getOrCreateStatusTrackingNode(Session session, String statusNodePath) {
        try {
            return JcrUtil.createPath(statusNodePath, JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, session, false);
        } catch (RepositoryException re) {
            logger.error("On-deploy script cannot be run because the system could not find or create the script status node: {}", statusNodePath);
            throw new OnDeployEarlyTerminationException(re);
        }
    }

    protected String getScriptStatus(ResourceResolver resourceResolver, Node statusNode, String statusNodePath) {
        try {
            Resource resource = resourceResolver.getResource(statusNode.getPath());
            return resource.getValueMap().get(SCRIPT_STATUS, (String) null);
        } catch (RepositoryException re) {
            logger.error("On-deploy script cannot be run because the system read the script status node: {}", statusNodePath);
            throw new OnDeployEarlyTerminationException(re);
        }
    }

    protected ResourceResolver logIn() {
        try {
            Map<String, Object> userParams = new HashMap<>();
            userParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
            return resourceResolverFactory.getServiceResourceResolver(userParams);
        } catch (LoginException le2) {
            logger.error("On-deploy scripts cannot be run because the system cannot log in with the appropriate service user");
            throw new OnDeployEarlyTerminationException(le2);
        }
    }

    protected void runScript(ResourceResolver resourceResolver, Session session, OnDeployScript script) throws RepositoryException {
        String statusNodePath = SCRIPT_STATUS_JCR_FOLDER + "/" + script.getClass().getName();
        Node statusNode = getOrCreateStatusTrackingNode(session, statusNodePath);
        String status = getScriptStatus(resourceResolver, statusNode, statusNodePath);
        if (status == null || status.equals(SCRIPT_STATUS_FAIL)) {
            trackScriptStart(session, statusNode);
            try {
                script.execute(resourceResolver);
                logger.info("On-deploy script completed successfully: {}", statusNodePath);
                trackScriptEnd(session, statusNode, SCRIPT_STATUS_SUCCESS);
            } catch (Exception e) {
                String errMsg = "On-deploy script failed: " + statusNodePath;
                logger.error(errMsg, e);
                trackScriptEnd(session, statusNode, SCRIPT_STATUS_FAIL);
                throw new OnDeployEarlyTerminationException(new RuntimeException(errMsg));
            }
        } else if (!status.equals(SCRIPT_STATUS_SUCCESS)) {
            String errMsg = "On-deploy script is already running or in an otherwise unknown state: " + statusNodePath + " - status: " + status;
            logger.error(errMsg);
            throw new OnDeployEarlyTerminationException(new RuntimeException(errMsg));
        } else {
            logger.debug("Skipping on-deploy script, as it is already complete: {}", statusNodePath);
        }
    }

    protected void runScripts(ResourceResolver resourceResolver, Session session, List<OnDeployScript> scripts) {
        for (OnDeployScript script : scripts) {
            try {
                runScript(resourceResolver, session, script);
            } catch (Exception e) {
                throw new OnDeployEarlyTerminationException(e);
            }
        }
    }

    protected void trackScriptEnd(Session session, Node statusNode, String status) throws RepositoryException {
        try {
            statusNode.setProperty(SCRIPT_STATUS, status);
            statusNode.setProperty(SCRIPT_DATE_END, Calendar.getInstance());
            session.save();
        } catch (RepositoryException e) {
            logger.error("On-deploy script status node could not be updated: {} - status: {}", statusNode.getPath(), status);
            throw new OnDeployEarlyTerminationException(e);
        }
    }

    protected void trackScriptStart(Session session, Node statusNode) throws RepositoryException {
        logger.info("Starting on-deploy script: {}", statusNode.getPath());
        try {
            statusNode.setProperty(SCRIPT_STATUS, SCRIPT_STATUS_RUNNING);
            statusNode.setProperty(SCRIPT_DATE_START, Calendar.getInstance());
            statusNode.setProperty(SCRIPT_DATE_END, (Value) null);
            session.save();
        } catch (RepositoryException e) {
            logger.error("On-deploy script cannot be run because the system could not write to the script status node: {}", statusNode.getPath());
            throw new OnDeployEarlyTerminationException(e);
        }
    }
}
