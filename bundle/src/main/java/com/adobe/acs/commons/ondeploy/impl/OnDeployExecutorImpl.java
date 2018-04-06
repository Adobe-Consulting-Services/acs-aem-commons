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

import com.adobe.acs.commons.ondeploy.OnDeployScriptProvider;
import com.adobe.acs.commons.ondeploy.scripts.OnDeployScript;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Collections;
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
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE
)
public class OnDeployExecutorImpl {
    static final String SCRIPT_STATUS_JCR_FOLDER = "/var/acs-commons/on-deploy-scripts-status";

    private static final String SCRIPT_DATE_END = "endDate";
    private static final String SCRIPT_DATE_START = "startDate";
    private static final String SCRIPT_STATUS = "status";
    private static final String SCRIPT_STATUS_FAIL = "fail";
    private static final String SCRIPT_STATUS_RUNNING = "running";
    private static final String SCRIPT_STATUS_SUCCESS = "success";

    private static final String SERVICE_NAME = "on-deploy-scripts";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference(name = "scriptProvider", referenceInterface = OnDeployScriptProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private List<OnDeployScriptProvider> scriptProviders;

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
        try {
            resourceResolver = logIn();
            runScripts(resourceResolver, scripts);
        } finally {
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

    protected Resource getOrCreateStatusTrackingResource(ResourceResolver resourceResolver, Class<?> scriptClass) {
        String scriptClassName = scriptClass.getName();
        Resource resource = resourceResolver.getResource(SCRIPT_STATUS_JCR_FOLDER + "/" + scriptClassName);
        if (resource == null) {
            Resource folder = resourceResolver.getResource(SCRIPT_STATUS_JCR_FOLDER);
            try {
                resource = resourceResolver.create(folder, scriptClassName, Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));
            } catch (PersistenceException re) {
                logger.error("On-deploy script cannot be run because the system could not find or create the script status node: {}/{}", SCRIPT_STATUS_JCR_FOLDER, scriptClassName);
                throw new OnDeployEarlyTerminationException(re);
            }
        }
        return resource;

    }

    protected String getScriptStatus(Resource statusResource) {
        return statusResource.getValueMap().get(SCRIPT_STATUS, (String) null);
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

    protected void runScript(ResourceResolver resourceResolver, OnDeployScript script) {
        Resource statusResource = getOrCreateStatusTrackingResource(resourceResolver, script.getClass());
        String status = getScriptStatus(statusResource);
        if (status == null || status.equals(SCRIPT_STATUS_FAIL)) {
            trackScriptStart(statusResource);
            try {
                script.execute(resourceResolver);
                logger.info("On-deploy script completed successfully: {}", statusResource.getPath());
                trackScriptEnd(statusResource, SCRIPT_STATUS_SUCCESS);
            } catch (Exception e) {
                String errMsg = "On-deploy script failed: " + statusResource.getPath();
                logger.error(errMsg, e);
                trackScriptEnd(statusResource, SCRIPT_STATUS_FAIL);
                throw new OnDeployEarlyTerminationException(new RuntimeException(errMsg));
            }
        } else if (!status.equals(SCRIPT_STATUS_SUCCESS)) {
            String errMsg = "On-deploy script is already running or in an otherwise unknown state: " + statusResource.getPath() + " - status: " + status;
            logger.error(errMsg);
            throw new OnDeployEarlyTerminationException(new RuntimeException(errMsg));
        } else {
            logger.debug("Skipping on-deploy script, as it is already complete: {}", statusResource.getPath());
        }
    }

    protected void runScripts(ResourceResolver resourceResolver, List<OnDeployScript> scripts) {
        for (OnDeployScript script : scripts) {
            try {
                runScript(resourceResolver, script);
            } catch (Exception e) {
                throw new OnDeployEarlyTerminationException(e);
            }
        }
    }

    protected void trackScriptEnd(Resource statusResource, String status) {
        try {
            ModifiableValueMap properties = statusResource.adaptTo(ModifiableValueMap.class);
            properties.put(SCRIPT_STATUS, status);
            properties.put(SCRIPT_DATE_END, Calendar.getInstance());
            statusResource.getResourceResolver().commit();
        } catch (PersistenceException e) {
            logger.error("On-deploy script status node could not be updated: {} - status: {}", statusResource.getPath(), status);
            throw new OnDeployEarlyTerminationException(e);
        }
    }

    protected void trackScriptStart(Resource statusResource) {
        logger.info("Starting on-deploy script: {}", statusResource.getPath());
        try {
            ModifiableValueMap properties = statusResource.adaptTo(ModifiableValueMap.class);
            properties.put(SCRIPT_STATUS, SCRIPT_STATUS_RUNNING);
            properties.put(SCRIPT_DATE_START, Calendar.getInstance());
            properties.remove(SCRIPT_DATE_END);
            statusResource.getResourceResolver().commit();
        } catch (PersistenceException e) {
            logger.error("On-deploy script cannot be run because the system could not write to the script status node: {}", statusResource.getPath());
            throw new OnDeployEarlyTerminationException(e);
        }
    }
}
