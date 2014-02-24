package com.adobe.acs.commons.twitter;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.jcrclustersupport.ClusterAware;

@Component(immediate = true, metatype = true, label = "ACS AEM Commons - Twitter Feed Refresh Scheduler", policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties(value = {
		@Property(name = "scheduler.expression", value = "0 0/15 * * * ?", label = "Twitter Feed Refresh interval (Quartz Cron Expression)"),
		@Property(name = "scheduler.concurrent", boolValue = false, propertyPrivate = true) })
public class TwitterFeedScheduler implements Runnable, ClusterAware {

	private Logger LOGGER = LoggerFactory.getLogger(TwitterFeedScheduler.class);

	private Boolean isMasterInstance = Boolean.FALSE;

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private TwitterFeedService twitterFeedService;

	@Property(value = "acs-commons/components/content/twitter-feed", label = "Twitter-feed component paths", unbounded = PropertyUnbounded.ARRAY)
	private static final String TWITTER_COMPONENT_PATHS = "twitter.component.paths";

	private String[] twitterComponentPaths = null;

	protected void activate(ComponentContext ctx) {
		final Dictionary<?, ?> props = ctx.getProperties();
		LOGGER.info("activate {}", props);

		twitterComponentPaths = PropertiesUtil.toStringArray(props.get(TWITTER_COMPONENT_PATHS));

	}

	// @Override
	// public void handleTopologyEvent(TopologyEvent event) {
	// if (event.getType() == TopologyEvent.Type.TOPOLOGY_CHANGED
	// || event.getType() == TopologyEvent.Type.TOPOLOGY_INIT) {
	//
	// isMasterInstance = event.getNewView().getLocalInstance().isLeader();
	// LOGGER.info("############### isMaster Instance:" + isMasterInstance);
	// }
	//
	// }

	@Override
	public void bindRepository(String repositoryId, String clusterId, boolean isMaster) {
		isMasterInstance = isMaster;
	}

	public void unbindRepository() {

	}

	public Boolean getIsMasterInstance() {
		return isMasterInstance;
	}

	@Override
	public void run() {

		ResourceResolver resourceResolver = null;

		if (isMasterInstance) {
			try {
				LOGGER.info("Master Instance, Running ACS AEM Commons Twitter Feed Scheduler");

				resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

				twitterFeedService.refreshTwitterFeed(resourceResolver, twitterComponentPaths);

			} catch (Exception e) {
				LOGGER.error("Exception while running TwitterFeedScheduler, details", e);
			} finally {
				if (resourceResolver != null) {
					resourceResolver.close();
					resourceResolver = null;
				}
			}

		}

	}

}
