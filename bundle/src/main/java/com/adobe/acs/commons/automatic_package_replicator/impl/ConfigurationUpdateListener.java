package com.adobe.acs.commons.automatic_package_replicator.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.automatic_package_replicator.AutomaticPackageReplicatorMBean;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component
@Service(value = EventHandler.class)
@Properties({
		@Property(name = EventConstants.EVENT_TOPIC, value = { SlingConstants.TOPIC_RESOURCE_ADDED,
				SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED }),
		@Property(name = EventConstants.EVENT_FILTER, value = "(path=/etc/acs-tools/automatic-package-replicator/*/jcr:content)") })
public class ConfigurationUpdateListener extends AnnotatedStandardMBean
		implements EventHandler, AutomaticPackageReplicatorMBean {

	private static final Logger log = LoggerFactory.getLogger(ConfigurationUpdateListener.class);

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	private static final String ROOT_PATH = "/etc/acs-commons/automatic-package-replicator";

	private BundleContext bctx;

	private Map<String, ServiceReference> automaticPackageReplicatorJobs = new HashMap<String, ServiceReference>();

	protected ConfigurationUpdateListener(Class<?> mbeanInterface) throws NotCompliantMBeanException {
		super(mbeanInterface);
	}

	@Activate
	public void activate(ComponentContext context) throws LoginException {
		bctx = context.getBundleContext();
		refreshCache();
	}

	public void handleEvent(Event event) {
		String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

		ResourceResolver resolver = null;

		try {
			resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
			PageManager pm = resolver.adaptTo(PageManager.class);
			path = pm.getContainingPage(path).getPath();
			automaticPackageReplicatorJobs.remove(path);

			if (SlingConstants.TOPIC_RESOURCE_ADDED.equals(event.getTopic())
					|| SlingConstants.TOPIC_RESOURCE_CHANGED.equals(event.getTopic())) {
				loadJobConfiguration(resolver, path);
			}
		} finally {
			if (resolver != null) {
				resolver.close();
			}
		}
	}

	private void loadJobConfiguration(ResourceResolver resolver, String path) {
		// TODO Auto-generated method stub

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

	@Override
	public void refreshCache() throws LoginException {
		log.debug("refreshCache");
		automaticPackageReplicatorJobs.clear();

		ResourceResolver resolver = null;

		try {
			resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
			PageManager pm = resolver.adaptTo(PageManager.class);
			Page rootPage = pm.getPage(ROOT_PATH);

			Iterator<Page> children = rootPage.listChildren();
			while (children.hasNext()) {
				loadJobConfiguration(resolver, children.next().getPath());
			}

		} finally {
			if (resolver != null) {
				resolver.close();
			}
		}
	}
}
