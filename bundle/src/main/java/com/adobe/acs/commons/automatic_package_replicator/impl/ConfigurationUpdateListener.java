/*
 * #%L
 * ACS AEM Tools Bundle - Automatic Package Replicator
 * %%
 * Copyright (C) 2017 - Dan Klco
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
package com.adobe.acs.commons.automatic_package_replicator.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.automatic_package_replicator.AutomaticPackageReplicatorMBean;
import com.adobe.acs.commons.automatic_package_replicator.model.AutomaticPackageReplicatorModel;
import com.adobe.acs.commons.automatic_package_replicator.model.AutomaticPackageReplicatorModel.TRIGGER;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.Replicator;

/**
 * Listens to changes under /etc/acs-commons/automatic-package-replication and
 * manages the Automatic Package Replicator jobs based on the updates.
 */
@Component(immediate = true)
@Service(value = { EventHandler.class, AutomaticPackageReplicatorMBean.class })
@Properties({
		@Property(name = EventConstants.EVENT_TOPIC, value = { SlingConstants.TOPIC_RESOURCE_ADDED,
				SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED }),
		@Property(name = "jmx.objectname", value = "com.adobe.acs.commons:type=Automatic Package Replicator"),
		@Property(name = EventConstants.EVENT_FILTER, value = "(path=/etc/acs-commons/automatic-package-replication/*/jcr:content)") })
public class ConfigurationUpdateListener extends AnnotatedStandardMBean
		implements EventHandler, AutomaticPackageReplicatorMBean {

	private static final Logger log = LoggerFactory.getLogger(ConfigurationUpdateListener.class);

	private static final String ROOT_PATH = "/etc/acs-commons/automatic-package-replication";

	public static final String SERVICE_OWNER_KEY = "service.owner";

	public static final String CONFIGURATION_ID_KEY = "configuration.id";

	private static final String TRIGGER_KEY = "trigger.name";

	/**
	 * Creating this as a separate method to make migrating to service users
	 * easier. Callers of this method must ensure the resource resolver is
	 * closed.
	 * 
	 * @param factory
	 *            the resource resolver factory to use for getting the resource
	 *            resolver
	 * @return the resource resolver or null if there is an exception allocating
	 *         the resource resolver
	 */
	@SuppressWarnings("deprecation")
	final static ResourceResolver getResourceResolver(ResourceResolverFactory factory) {
		ResourceResolver resolver = null;
		try {
			resolver = factory.getAdministrativeResourceResolver(null);

		} catch (LoginException e) {
			log.error("Exception allocating resource resolver", e);
		}
		return resolver;
	}

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private Scheduler scheduler;

	@Reference
	private Replicator replicator;

	@Reference
	private EventAdmin eventAdmin;

	private BundleContext bctx;

	private Map<String, ServiceRegistration> automaticPackageReplicatorJobs = new HashMap<String, ServiceRegistration>();

	public ConfigurationUpdateListener() throws NotCompliantMBeanException {
		super(AutomaticPackageReplicatorMBean.class);
	}

	protected ConfigurationUpdateListener(Class<?> mbeanInterface) throws NotCompliantMBeanException {
		super(mbeanInterface);
	}

	@Activate
	public void activate(ComponentContext context) throws LoginException {
		log.info("activate");
		bctx = context.getBundleContext();
		refreshCache();
		log.info("Activation successful!");
	}

	@Deactivate
	public void deactivate(ComponentContext context) throws LoginException {
		log.info("deactivate");
		for (String id : automaticPackageReplicatorJobs.keySet()) {
			unregisterJobConfiguration(id);
		}
		log.info("Deactivation successful!");
	}

	@Override
	public void execute(String id) {
		AutomaticPackageReplicatorJob job = (AutomaticPackageReplicatorJob) bctx
				.getService(automaticPackageReplicatorJobs.get(id).getReference());
		job.run();
	}

	@Override
	public List<String> getRegisteredConfigurations() {
		List<String> configurations = new ArrayList<String>();
		configurations.addAll(automaticPackageReplicatorJobs.keySet());
		return configurations;
	}

	public void handleEvent(Event event) {
		log.trace("handleEvent");
		refreshCache();
	}

	@Override
	public synchronized void refreshCache() {
		log.debug("refreshCache");

		ResourceResolver resolver = null;

		try {

			resolver = getResourceResolver(resourceResolverFactory);
			Resource aprRoot = resolver.getResource(ROOT_PATH);
			List<String> configuredIds = new ArrayList<String>();
			for (Resource child : aprRoot.getChildren()) {
				if (!JcrConstants.JCR_CONTENT.equals(child.getName())) {
					updateJobService(child.getPath(), child.getChild(JcrConstants.JCR_CONTENT));
					configuredIds.add(child.getPath());
				}
			}

			String filter = "(" + SERVICE_OWNER_KEY + "=" + getClass().getCanonicalName() + ")";
			ServiceReference[] serviceReferences = (ServiceReference[]) ArrayUtils.addAll(
					bctx.getServiceReferences(Runnable.class.getCanonicalName(), filter),
					bctx.getServiceReferences(EventHandler.class.getCanonicalName(), filter));

			log.warn("Found {} registered services", serviceReferences.length);
			for (ServiceReference reference : serviceReferences) {
				try {
					String configurationId = (String) reference.getProperty(CONFIGURATION_ID_KEY);
					if (!configuredIds.contains(configurationId)) {
						log.debug("Unregistering service for configuration {}", configurationId);
						this.unregisterJobConfiguration(configurationId);
					}
				} catch (Exception e) {
					log.warn("Exception unregistering reference " + reference, e);
				}
			}

		} catch (InvalidSyntaxException e) {
			log.warn("Unable to search for invalid references due to invalid filter format", e);
		} finally {
			if (resolver != null) {
				resolver.close();
			}
		}
	}

	private void updateJobService(String id, Resource resource) {
		AutomaticPackageReplicatorModel model = new AutomaticPackageReplicatorModel(resource);

		log.debug("Registering job: {}", id);
		try {

			String filter = "(&(" + SERVICE_OWNER_KEY + "=" + getClass().getCanonicalName() + ")" + "("
					+ CONFIGURATION_ID_KEY + "=" + id + "))";
			ServiceReference[] serviceReferences = (ServiceReference[]) ArrayUtils.addAll(
					bctx.getServiceReferences(Runnable.class.getCanonicalName(), filter),
					bctx.getServiceReferences(EventHandler.class.getCanonicalName(), filter));
			
			if (serviceReferences != null && serviceReferences.length > 0) {
				ServiceReference sr = serviceReferences[0];
				String triggerStr = (String) sr.getProperty(TRIGGER_KEY);
				if (model.getTrigger() == TRIGGER.cron && model.getTrigger() == TRIGGER.valueOf(triggerStr)
						&& ObjectUtils.equals(sr.getProperty(Scheduler.PROPERTY_SCHEDULER_EXPRESSION),
								model.getCronTrigger())) {
					log.debug("Cron job registered correctly, no changes required");
				} else if (model.getTrigger() == TRIGGER.event && model.getTrigger() == TRIGGER.valueOf(triggerStr)
						&& ObjectUtils.equals(sr.getProperty(EventConstants.EVENT_TOPIC), model.getEventTopic())
						&& ObjectUtils.equals(sr.getProperty(EventConstants.EVENT_FILTER), model.getEventFilter())) {
					log.debug("Event handler registered correctly, no changes required");

				} else {
					log.warn("Unbinding ServiceReference for {}", id);
					unregisterJobConfiguration(id);
					registerJobService(id, model);
				}
			} else {
				registerJobService(id, model);
			}

		} catch (Exception e) {
			log.error("Failed to register job " + id, e);
		}
	}

	private ServiceRegistration registerJobService(String id, AutomaticPackageReplicatorModel model) {
		AutomaticPackageReplicatorJob job = new AutomaticPackageReplicatorJob(resourceResolverFactory, replicator,
				eventAdmin, model.getPackagePath());
		ServiceRegistration serviceRegistration = null;
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(SERVICE_OWNER_KEY, getClass().getCanonicalName());
		props.put(CONFIGURATION_ID_KEY, id);
		props.put(TRIGGER_KEY, model.getTrigger().name());
		if (AutomaticPackageReplicatorModel.TRIGGER.cron == model.getTrigger()) {
			props.put(Scheduler.PROPERTY_SCHEDULER_EXPRESSION, model.getCronTrigger());
			log.debug("Registering cron runner with: {}", props);
			serviceRegistration = bctx.registerService(Runnable.class.getCanonicalName(), job, props);
		} else {
			props.put(EventConstants.EVENT_TOPIC, new String[] { model.getEventTopic() });
			if (StringUtils.isNotEmpty(model.getEventFilter())) {
				props.put(EventConstants.EVENT_FILTER, model.getEventFilter());
			}
			log.debug("Registering event handler runner with: {}", props);
			serviceRegistration = bctx.registerService(EventHandler.class.getCanonicalName(), job, props);
		}

		automaticPackageReplicatorJobs.put(id, serviceRegistration);
		log.debug("Automatic Package Replication job {} successfully updated with service {}",
				new Object[] { id, serviceRegistration.getReference().getProperty(Constants.SERVICE_ID) });

		return serviceRegistration;
	}

	private void unregisterJobConfiguration(String id) {
		log.debug("Unregistering job: {}", id);
		try {
			ServiceRegistration registration = automaticPackageReplicatorJobs.remove(id);
			if (registration != null) {
				registration.unregister();
			}
			log.debug("Job {} registered successfully!", id);
		} catch (Exception e) {
			log.warn("Exception unregistering job " + id, e);
		}
	}
}
