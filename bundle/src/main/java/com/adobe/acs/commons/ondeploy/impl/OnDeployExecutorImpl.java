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
import com.adobe.acs.commons.ondeploy.OnDeployScript;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.search.QueryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
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
        policy = ConfigurationPolicy.REQUIRE,
        configurationFactory = true
)
@Service
public class OnDeployExecutorImpl implements OnDeployExecutor {
    @Property(
            name = "scripts",
            cardinality = Integer.MAX_VALUE,
            label = "Scripts",
            description = "Classes that repersent on-deploy scripts (implement OnDeployScript), in the order to run them."
    )
    private static final String PROP_SCRIPTS = "scripts";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private QueryBuilder queryBuilder;
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    private List<OnDeployScript> scripts = null;

    /**
     * Executes all on-deploy scripts on activation of this service.
     *
     * @param properties OSGi properties for this service (unused).
     */
    @Activate
    protected final void activate(final Map<String, String> properties) {
        logger.info("Checking for on-deploy scripts");
        configure(properties);
        if (scripts.size() == 0) {
            logger.debug("No on-deploy scripts found.");
            return;
        }
        ResourceResolver resourceResolver = null;
        Session session = null;
        try {
            try {
                Map<String, Object> userParams = new HashMap<>();
                userParams.put(ResourceResolverFactory.SUBSERVICE, "onDeployScripts");
                resourceResolver = resourceResolverFactory.getServiceResourceResolver(userParams);
            } catch (LoginException le2) {
                logger.error("On-deploy scripts cannot be run because the system cannot log in with the appropriate service user");
                throw new OnDeployEarlyTerminationException(le2);
            }
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

    /**
     * Convert the conifgured list of on-deploy script classes to object instances.
     *
     * @param properties OSGi configs.
     */
    private void configure(final Map<String, String> properties) {
        scripts = new ArrayList<>();

        ClassLoader cl = dynamicClassLoaderManager.getDynamicClassLoader();
        String[] scriptClasses = PropertiesUtil.toStringArray(properties.get(PROP_SCRIPTS), new String[0]);
        for (String scriptClassName : scriptClasses) {
            if (StringUtils.isNotBlank(scriptClassName)) {
                try {
                    Class scriptClass = cl.loadClass(scriptClassName);
                    if (OnDeployScript.class.isAssignableFrom(scriptClass)) {
                        try {
                            scripts.add((OnDeployScript) scriptClass.newInstance());
                        } catch (Exception e) {
                            logger.error("Could not instatiate on-deploy script class: {}", scriptClassName);
                            throw new OnDeployEarlyTerminationException(e);
                        }
                    } else {
                        String errMsg = "On-deploy script class does not implement the OnDeployScript interface: " + scriptClassName;
                        logger.error(errMsg);
                        throw new OnDeployEarlyTerminationException(new RuntimeException(errMsg));
                    }
                } catch (ClassNotFoundException cnfe) {
                    logger.error("Could not find on-deploy script class: {}", scriptClassName);
                    throw new OnDeployEarlyTerminationException(cnfe);
                }
            }
        }
    }

    protected Node getOrCreateStatusTrackingNode(Session session, String statusNodePath) {
        try {
            return JcrUtil.createPath(statusNodePath, "nt:unstructured", "nt:unstructured", session, false);
        } catch (RepositoryException re) {
            logger.error("On-deploy script cannot be run because the system could not find or create the script status node: {}", statusNodePath);
            throw new OnDeployEarlyTerminationException(re);
        }
    }

    protected String getScriptStatus(ResourceResolver resourceResolver, Node statusNode, String statusNodePath) {
        try {
            Resource resource = resourceResolver.getResource(statusNode.getPath());
            return resource.getValueMap().get("status", (String) null);
        } catch (RepositoryException re) {
            logger.error("On-deploy script cannot be run because the system read the script status node: {}", statusNodePath);
            throw new OnDeployEarlyTerminationException(re);
        }
    }

    protected void runScript(ResourceResolver resourceResolver, Session session, OnDeployScript script) {
        String statusNodePath = "/var/acs-commons/on-deploy-scripts-status/" + script.getClass().getName();
        Node statusNode = getOrCreateStatusTrackingNode(session, statusNodePath);
        String status = getScriptStatus(resourceResolver, statusNode, statusNodePath);
        if (status == null || status.equals("fail")) {
            trackScriptStart(session, statusNode, statusNodePath);
            try {
                script.execute(resourceResolver, queryBuilder);
                logger.info("On-deploy script completed successfully: {}", statusNodePath);
                trackScriptEnd(session, statusNode, statusNodePath, "success");
            } catch (Exception e) {
                String errMsg = "On-deploy script failed: " + script.getClass().getName();
                logger.error(errMsg, e);
                trackScriptEnd(session, statusNode, statusNodePath, "fail");
                throw new OnDeployEarlyTerminationException(new RuntimeException(errMsg));
            }
        } else if (!status.equals("success")) {
            String errMsg = "On-deploy script is already running or in an otherwise unknown state: " + script.getClass().getName() + " - status: " + status;
            logger.error(errMsg);
            throw new OnDeployEarlyTerminationException(new RuntimeException(errMsg));
        } else {
            logger.debug("Skipping on-deploy script, as it is already complete: {}", statusNodePath);
        }
    }

    protected void runScripts(ResourceResolver resourceResolver, Session session, List<OnDeployScript> scripts) {
        for (OnDeployScript script : scripts) {
            runScript(resourceResolver, session, script);
        }
    }

    protected void trackScriptEnd(Session session, Node statusNode, String statusNodePath, String status) {
        try {
            statusNode.setProperty("status", status);
            statusNode.setProperty("endDate", Calendar.getInstance());
            session.save();
        } catch (RepositoryException e) {
            logger.error("On-deploy script status node could not be updated: {} - status: {}", statusNodePath, status);
            throw new OnDeployEarlyTerminationException(e);
        }
    }

    protected void trackScriptStart(Session session, Node statusNode, String statusNodePath) {
        logger.info("Starting on-deploy script: {}", statusNodePath);
        try {
            statusNode.setProperty("status", "running");
            statusNode.setProperty("startDate", Calendar.getInstance());
            statusNode.setProperty("endDate", (Calendar) null);
            session.save();
        } catch (RepositoryException e) {
            logger.error("On-deploy script cannot be run because the system could not write to the script status node: {}", statusNodePath);
            throw new OnDeployEarlyTerminationException(e);
        }
    }
}
