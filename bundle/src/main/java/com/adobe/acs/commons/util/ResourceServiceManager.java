/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.util;

import com.adobe.acs.commons.util.mbeans.ResourceServiceManagerMBean;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.day.cq.commons.jcr.JcrConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.management.NotCompliantMBeanException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for services to extend which want to manage other services based
 * on repository configurations.
 */
public abstract class ResourceServiceManager extends AnnotatedStandardMBean
        implements ResourceServiceManagerMBean, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceManager.class);

    public static final String SERVICE_OWNER_KEY = "service.owner";

    public static final String CONFIGURATION_ID_KEY = "configuration.id";

    private BundleContext bctx;

    private Map<String, ServiceRegistration> registeredServices = new HashMap<String, ServiceRegistration>();

    protected ResourceServiceManager(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);
    }

    @Activate
    public synchronized void activate(ComponentContext context) throws LoginException {
        log.trace("activate");
        bctx = context.getBundleContext();
        refreshCache();
        log.trace("Activation successful!");
    }

    @Deactivate
    public synchronized void deactivate(ComponentContext context) throws LoginException {
        log.trace("deactivate");
        for (String id : registeredServices.keySet()) {
            unregisterService(id);
        }
        log.trace("Deactivation successful!");
    }

    public BundleContext getBundleContext() {
        return bctx;
    }

    @Override
    public List<String> getRegisteredConfigurations() {
        List<String> registeredConfigurations = new ArrayList<String>();
        registeredConfigurations.addAll(this.registeredServices.keySet());
        return registeredConfigurations;
    }

    public Map<String, ServiceRegistration> getRegisteredServices() {
        return registeredServices;
    }

    /**
     * Get a resource resolver to access the Sling Repository.
     *
     * @return the resource resolver
     */
    protected abstract ResourceResolver getResourceResolver();

    /**
     * Get the root path for the instance of the ResourceServiceManager, the
     * configuration should be cq:Page children of the resource at this path.
     *
     * @return the configuration root
     */
    public abstract String getRootPath();

    public void handleEvent(Event event) {
        log.trace("handleEvent");
        refreshCache();
    }

    /**
     * Checks whether or not the specified ServiceReference is up to date with
     * the configuration resource.
     *
     * @param config
     *            the configuration resource
     * @param reference
     *            the service reference to check
     * @return true if the ServiceReference is up to date with the resource,
     *         false otherwise
     */
    protected abstract boolean isServiceUpdated(Resource config, ServiceReference reference);

    @Override
    @SuppressWarnings({"squid:S3776", "squid:S1141"})
    public synchronized void refreshCache() {
        log.trace("refreshCache");

        try ( ResourceResolver resolver = getResourceResolver()) {

            Resource aprRoot = resolver.getResource(getRootPath());
            if (aprRoot == null) {
                log.error("Root path for service resource not found: {}", getRootPath());
                return;
            }
            List<String> configuredIds = new ArrayList<String>();
            for (Resource child : aprRoot.getChildren()) {
                if (!JcrConstants.JCR_CONTENT.equals(child.getName())) {
                    log.debug("Updating service for configuration {}", child.getPath());
                    updateJobService(child.getPath(), child.getChild(JcrConstants.JCR_CONTENT));
                    configuredIds.add(child.getPath());
                }
            }

            String filter = "(" + SERVICE_OWNER_KEY + "=" + getClass().getCanonicalName() + ")";
            ServiceReference[] serviceReferences = (ServiceReference[]) ArrayUtils.addAll(
                    bctx.getServiceReferences(Runnable.class.getCanonicalName(), filter),
                    bctx.getServiceReferences(EventHandler.class.getCanonicalName(), filter));

            if (serviceReferences != null && serviceReferences.length > 0) {
                log.debug("Found {} registered services", serviceReferences.length);
                for (ServiceReference reference : serviceReferences) {
                    try {
                        String configurationId = (String) reference.getProperty(CONFIGURATION_ID_KEY);
                        if (!configuredIds.contains(configurationId)) {
                            log.debug("Unregistering service for configuration {}", configurationId);
                            this.unregisterService(configurationId);
                        }
                    } catch (Exception e) {
                        log.warn("Exception unregistering reference " + reference, e);
                    }
                }
            } else {
                log.debug("Did not find any registered services.");
            }

        } catch (InvalidSyntaxException e) {
            log.warn("Unable to search for invalid references due to invalid filter format", e);
        }
    }

    @SuppressWarnings("squid:S1149")
    private ServiceRegistration registerService(String id, Resource config) {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(SERVICE_OWNER_KEY, getClass().getCanonicalName());
        props.put(CONFIGURATION_ID_KEY, id);
        ServiceRegistration serviceRegistration = registerServiceObject(config, props);

        registeredServices.put(id, serviceRegistration);
        log.debug("Automatic Package Replication job {} successfully updated with service {}",
                id, serviceRegistration.getReference().getProperty(Constants.SERVICE_ID));

        return serviceRegistration;
    }

    /**
     * Register the service with the OSGi Container based on the configuration.
     *
     * @param config
     *            the configuration resource
     * @param props
     *            the default properties
     * @return the ServiceRegistration from registering the service
     */
    @SuppressWarnings("squid:S1149")
    protected abstract ServiceRegistration registerServiceObject(Resource config, Hashtable<String, Object> props);

    private void unregisterService(String id) {
        log.debug("Unregistering job: {}", id);
        try {
            ServiceRegistration registration = registeredServices.remove(id);
            if (registration != null) {
                registration.unregister();
            }
            log.debug("Job {} registered successfully!", id);
        } catch (Exception e) {
            log.warn("Exception unregistering job " + id, e);
        }
    }

    private void updateJobService(String id, Resource resource) {

        log.debug("Registering job: {}", id);
        try {

            String filter = "(&(" + SERVICE_OWNER_KEY + "=" + getClass().getCanonicalName() + ")" + "("
                    + CONFIGURATION_ID_KEY + "=" + id + "))";
            ServiceReference[] serviceReferences = (ServiceReference[]) ArrayUtils.addAll(
                    bctx.getServiceReferences(Runnable.class.getCanonicalName(), filter),
                    bctx.getServiceReferences(EventHandler.class.getCanonicalName(), filter));

            if (serviceReferences != null && serviceReferences.length > 0) {
                ServiceReference sr = serviceReferences[0];
                if (isServiceUpdated(resource, sr)) {
                    log.debug("Service for {} up to date, no changes necessary", id);
                } else {
                    log.warn("Unbinding ServiceReference for {}", id);
                    unregisterService(id);
                    registerService(id, resource);
                }
            } else {
                registerService(id, resource);
            }

        } catch (Exception e) {
            log.error("Failed to register job " + id, e);
        }
    }

}
