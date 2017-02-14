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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

import org.apache.commons.lang.ArrayUtils;
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
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

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

	private Map<String, ServiceReference> automaticPackageReplicatorJobs = new HashMap<String, ServiceReference>();

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
				.getService(automaticPackageReplicatorJobs.get(id));
		job.run();
	}

	@Override
	public String[] getRegisteredConfigurations() {
		return automaticPackageReplicatorJobs.keySet().toArray(new String[0]);
	}

	public void handleEvent(Event event) {
		log.trace("handleEvent");
		String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

		ResourceResolver resolver = null;

		try {
			resolver = getResourceResolver(resourceResolverFactory);
			PageManager pm = resolver.adaptTo(PageManager.class);
			Page configPage = pm.getContainingPage(path);

			unregisterJobConfiguration(configPage.getPath());

			if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic())
					|| SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic())) {
				registerJobConfiguration(configPage.getPath(), configPage.getContentResource());
			}
		} finally {
			if (resolver != null) {
				resolver.close();
			}
		}
	}

	@Override
	public void refreshCache() {
		log.debug("refreshCache");
		for (String id : automaticPackageReplicatorJobs.keySet()) {
			this.unregisterJobConfiguration(id);
		}
		
		try {
			String filter = "(" + SERVICE_OWNER_KEY + "=" + getClass().getCanonicalName() + ")";
			ServiceReference[] serviceReferences = (ServiceReference[]) ArrayUtils.addAll(
					bctx.getServiceReferences(Runnable.class.getCanonicalName(), filter),
					bctx.getServiceReferences(EventHandler.class.getCanonicalName(), filter));
			if (serviceReferences != null && serviceReferences.length > 0) {
				log.warn("Found {} registered services after unregistering known jobs", serviceReferences.length);
				for (ServiceReference reference : serviceReferences) {
					try {
						if (reference != null) {
							bctx.ungetService(reference);
						}
					} catch (Exception e) {
						log.warn("Exception unregistering reference " + reference, e);
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			log.warn("Unable to search for invalid references due to invalid filter format", e);
		}

		ResourceResolver resolver = null;

		try {
			resolver = getResourceResolver(resourceResolverFactory);
			Resource aprRoot = resolver.getResource(ROOT_PATH);
			for (Resource child : aprRoot.getChildren()) {
				if (!JcrConstants.JCR_CONTENT.equals(child.getName())) {
					registerJobConfiguration(child.getPath(), child.getChild(JcrConstants.JCR_CONTENT));
				}
			}
		} finally {
			if (resolver != null) {
				resolver.close();
			}
		}
	}

	private void registerJobConfiguration(String id, Resource resource) {
		AutomaticPackageReplicatorModel model = new AutomaticPackageReplicatorModel(resource);

		log.debug("Registering job: {}", id);
		try {
			AutomaticPackageReplicatorJob job = new AutomaticPackageReplicatorJob(resourceResolverFactory, replicator,
					eventAdmin, model.getPackagePath());
			ServiceRegistration serviceRegistration = null;
			Hashtable<String, Object> props = new Hashtable<String, Object>();
			props.put(SERVICE_OWNER_KEY, getClass().getCanonicalName());
			if (AutomaticPackageReplicatorModel.TRIGGER.cron == model.getTrigger()) {
				props.put(Scheduler.PROPERTY_SCHEDULER_EXPRESSION, model.getCronTrigger());
				log.debug("Registering cron runner with: {}", props);
				serviceRegistration = bctx.registerService(Runnable.class.getName(), job, props);
			} else {
				props.put(EventConstants.EVENT_TOPIC, new String[] { model.getEventTopic() });
				if (StringUtils.isNotEmpty(model.getEventFilter())) {
					props.put(EventConstants.EVENT_FILTER, model.getEventFilter());
				}
				log.debug("Registering event handler runner with: {}", props);
				serviceRegistration = bctx.registerService(EventHandler.class.getName(), job, props);
			}

			automaticPackageReplicatorJobs.put(id, serviceRegistration.getReference());

			log.debug("Automatic Package Replication job {} registered successfully as service {}",
					new Object[] { id, serviceRegistration.getReference().getProperty(Constants.SERVICE_ID) });

		} catch (Exception e) {
			log.error("Failed to register job " + id, e);
		}
	}

	private void unregisterJobConfiguration(String id) {
		log.debug("Unregistering job: {}", id);
		try {
			ServiceReference reference = automaticPackageReplicatorJobs.remove(id);
			if (reference != null) {
				bctx.ungetService(reference);
			}
			log.debug("Job {} registered successfully!", id);
		} catch (Exception e) {
			log.warn("Exception unregistering job " + id, e);
		}
	}
}
