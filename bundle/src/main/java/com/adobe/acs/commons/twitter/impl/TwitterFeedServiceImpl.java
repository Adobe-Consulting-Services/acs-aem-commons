package com.adobe.acs.commons.twitter.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.twitter.TwitterConfiguration;
import com.adobe.acs.commons.twitter.TwitterFeedService;
import com.adobe.acs.commons.twitter.TwitterOAuthCommunicator;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.webservicesupport.Configuration;
import com.day.cq.wcm.webservicesupport.ConfigurationManager;

@Component(immediate = true, label = "ACS AEM Commons - Twitter Feed Service")
@Service
public class TwitterFeedServiceImpl implements TwitterFeedService {

	private Logger LOGGER = LoggerFactory.getLogger(TwitterFeedServiceImpl.class);

	@Reference
	private Replicator replicator;

	@Reference
	private ConfigurationManager configurationManager;

	private TwitterOAuthCommunicator twitterOAuthCommunicator;

	protected void activate(ComponentContext ctx) {
		twitterOAuthCommunicator = new TwitterOAuthCommunicator();
	}

	@Override
	public void refreshTwitterFeed(ResourceResolver resourceResolver, String[] twitterComponentPaths) throws RepositoryException {

		List<Resource> twitterResources = findTwitterResources(resourceResolver, twitterComponentPaths);

		Map<String, List<String>> usernameTweetsMap = loadTwitterFeedsForUserName(resourceResolver, twitterResources);

		updateTwitterFeedOnResources(resourceResolver, twitterResources, usernameTweetsMap);

	}

	private List<Resource> findTwitterResources(ResourceResolver resourceResolver, String[] twitterComponentPaths)
			throws RepositoryException {

		List<Resource> twitterResources = new ArrayList<Resource>();

		Map<String, String> predicateMap = new HashMap<String, String>();
		predicateMap.put("path", "/content");
		predicateMap.put("property", "sling:resourceType");

		int i = 1;
		for (String path : twitterComponentPaths) {
			predicateMap.put("property." + (i++) + "_value", path.toString());

		}

		predicateMap.put("p.limit", "-1");

		List<Hit> hits = runQuery(resourceResolver, predicateMap);

		if (hits != null && hits.size() > 0) {

			for (Hit hit : hits) {
				twitterResources.add(hit.getResource());
			}
		}

		return twitterResources;
	}

	private List<Hit> runQuery(ResourceResolver resourceResolver, Map<String, String> queryParams) {
		List<Hit> hits = new ArrayList<Hit>();

		QueryBuilder queryBuilder = resourceResolver.adaptTo(QueryBuilder.class);
		Session session = resourceResolver.adaptTo(Session.class);
		Query query = queryBuilder.createQuery(PredicateGroup.create(queryParams), session);

		SearchResult result = query.getResult();
		hits = result.getHits();
		return hits;
	}

	private Map<String, List<String>> loadTwitterFeedsForUserName(ResourceResolver resourceResolver,
			List<Resource> twitterResources) throws RepositoryException {

		Map<String, List<String>> usernameTweetsMap = new HashMap<String, List<String>>();

		for (Resource twitterResource : twitterResources) {

			LOGGER.info("Loading Twitter configuration for resource {}", twitterResource);

			TwitterConfiguration twitterConfiguration = retrieveTwitterConfiguration(resourceResolver, twitterResource);

			if (twitterConfiguration.isValid()) {
				String userName = twitterConfiguration.getUsername();

				if (!usernameTweetsMap.containsKey(userName)) {
					List<String> tweets = twitterOAuthCommunicator.getTweetsAsList(twitterConfiguration);

					usernameTweetsMap.put(twitterConfiguration.getUsername(), tweets);
				}

			} else {
				LOGGER.info("Invalid Twitter configuration for resource {}, it won't be refreshed", twitterResource);
			}

		}

		return usernameTweetsMap;
	}

	private TwitterConfiguration retrieveTwitterConfiguration(ResourceResolver resourceResolver, Resource twitterResource)
			throws RepositoryException, ValueFormatException, PathNotFoundException {

		TwitterConfiguration twitterConfiguration = new TwitterConfiguration();

		String userName = findUsername(twitterResource);
		twitterConfiguration.setUsername(userName);

		String[] services = findCloudServiceConfigurations(resourceResolver, twitterResource);

		if (configurationManager != null) {

			Configuration twitterCloudconfiguration = configurationManager.getConfiguration("twitterconnect", services);

			populateTwitterConfiguration(twitterConfiguration, twitterCloudconfiguration);

		}

		return twitterConfiguration;
	}

	private String findUsername(Resource twitterResource) {
		Node twitterResourceNode = twitterResource.adaptTo(Node.class);

		ValueMap properties = new JcrPropertyMap(twitterResourceNode);

		String userName = properties.get("username", null);
		return userName;
	}

	private String[] findCloudServiceConfigurations(ResourceResolver resourceResolver, Resource twitterResource) {
		PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

		Page page = pageManager.getContainingPage(twitterResource);

		Resource resource = page.getContentResource();
		InheritanceValueMap valueMap = new HierarchyNodeInheritanceValueMap(resource);
		String[] services = valueMap.getInherited("cq:cloudserviceconfigs", new String[] {});
		return services;
	}

	private void populateTwitterConfiguration(TwitterConfiguration twitterConfiguration, Configuration twitterCloudconfiguration)
			throws RepositoryException, ValueFormatException, PathNotFoundException {
		if (twitterCloudconfiguration != null) {

			Node configNode = twitterCloudconfiguration.getContentResource().adaptTo(Node.class);

			NodeIterator nodeIterator = configNode.getNodes();

			while (nodeIterator.hasNext()) {
				Node childNode = nodeIterator.nextNode();

				String consumerKey = childNode.getProperty("oauth.client.id").getString();
				String consumerSecret = childNode.getProperty("oauth.client.secret").getString();

				twitterConfiguration.setConsumerKey(consumerKey);
				twitterConfiguration.setConsumerSecret(consumerSecret);
				break;

			}

		}
	}

	private void updateTwitterFeedOnResources(ResourceResolver resourceResolver, List<Resource> twitterResources,
			Map<String, List<String>> usernameTweetsMap) throws RepositoryException {

		for (Resource twitterResource : twitterResources) {

			updateAndReplicateTwitterFeedOnResource(twitterResource, usernameTweetsMap, resourceResolver);

		}

	}

	public void updateAndReplicateTwitterFeedOnResource(Resource twitterResource, Map<String, List<String>> usernameTweetsMap,
			ResourceResolver resourceResolver) {

		try {

			String username = findUsername(twitterResource);

			if (!StringUtils.isEmpty(username)) {

				List<String> tweets = usernameTweetsMap.get(username);

				if (!CollectionUtils.isEmpty(tweets)) {

					Node twitterNode = twitterResource.adaptTo(Node.class);
					removeProperty("tweets", twitterNode);
					twitterNode.setProperty("tweets", tweets.toArray(new String[tweets.size()]));

					Session session = resourceResolver.adaptTo(Session.class);
					session.save();

					handleReplication(twitterResource, resourceResolver, session);
				}
			}

		} catch (RepositoryException e) {
			LOGGER.error("Exception while updating twitter feed on resource:" + twitterResource.getPath(), e);
		}

	}

	private void handleReplication(Resource twitterResource, ResourceResolver resourceResolver, Session session)
			throws RepositoryException {

		Node twitterNode = twitterResource.adaptTo(Node.class);

		if (isReplicationOn(twitterNode)) {
			PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
			Page page = pageManager.getContainingPage(twitterResource);

			replicatePage(session, page.getPath());
		}

	}

	private boolean isReplicationOn(Node twitterNode) throws RepositoryException {

		boolean replicate = false;

		if (twitterNode.hasProperty("replicate")) {
			replicate = twitterNode.getProperty("replicate").getBoolean();

		}

		return replicate;
	}

	private static void removeProperty(String propertyName, Node node) throws RepositoryException {

		if (node.hasProperty(propertyName)) {
			if (node.getProperty(propertyName).isMultiple()) {
				Value[] nullVal = null;
				node.setProperty(propertyName, nullVal);
			} else {
				Value nullVal = null;
				node.setProperty(propertyName, nullVal);
			}

			node.getSession().save();
		}
	}

	private void replicatePage(Session session, String path) {
		try {

			replicator.replicate(session, ReplicationActionType.ACTIVATE, path);

		} catch (Exception e) {
			LOGGER.error("Exception while replicating page: " + path, e);
		}
	}

}
