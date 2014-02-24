package com.adobe.acs.commons.twitter.impl;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junitx.util.PrivateAccessor;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.adobe.acs.commons.twitter.TwitterConfiguration;
import com.adobe.acs.commons.twitter.TwitterFeedService;
import com.adobe.acs.commons.twitter.TwitterOAuthCommunicator;
import com.adobe.acs.commons.util.MockNode;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.webservicesupport.Configuration;
import com.day.cq.wcm.webservicesupport.ConfigurationManager;

@RunWith(MockitoJUnitRunner.class)
public class TwitterFeedServiceImplTest {

	private TwitterFeedService twitterFeedService;
	
	@Mock
	private Logger LOGGER;

	@Mock
	private TwitterOAuthCommunicator twitterOAuthCommunicator;

	@Mock
	private ConfigurationManager configurationManager;

	@Mock
	private Configuration configuration;

	@Mock
	private ResourceResolver resourceResolver;

	@Mock
	private QueryBuilder queryBuilder;

	@Mock
	private Session session;

	@Mock
	private Query query;

	@Mock
	private SearchResult searchResult;

	@Mock
	private Resource twitterResource;

	@Mock
	private Resource contentResource;

	@Mock
	private PageManager pageManager;

	@Mock
	private Page page;

	private String[] twitterComponentPaths = { "acs-commons//components//content//twitter-feed" };

	private List<Hit> hits = new ArrayList<Hit>();

	private Node twitterConfigNode;

	private Node twitterResourceNode;

	@Before
	public void setUp() throws Exception {
		twitterFeedService = new TwitterFeedServiceImpl();

		populate();

		PrivateAccessor.setField(twitterFeedService, "twitterOAuthCommunicator", twitterOAuthCommunicator);
		PrivateAccessor.setField(twitterFeedService, "configurationManager", configurationManager);
		PrivateAccessor.setField(twitterFeedService, "LOGGER", LOGGER);

		when(configurationManager.getConfiguration(anyString(), any(String[].class))).thenReturn(configuration);
		when(configuration.getContentResource()).thenReturn(contentResource);
		when(contentResource.adaptTo(Node.class)).thenReturn(twitterConfigNode);

		when(resourceResolver.adaptTo(QueryBuilder.class)).thenReturn(queryBuilder);
		when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
		when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);

		when(queryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).thenReturn(query);
		when(query.getResult()).thenReturn(searchResult);
		when(searchResult.getHits()).thenReturn(hits);
		when(pageManager.getContainingPage(twitterResource)).thenReturn(page);

		when(twitterResource.adaptTo(Node.class)).thenReturn(twitterResourceNode);

	}

	private void populate() throws RepositoryException {

		populateTwitterConfigNode();

		populateTwitterResourceNode();

		hits.add(new HitStub(twitterResource));

	}

	private void populateTwitterConfigNode() throws RepositoryException {

		Node[] nodes = new Node[1];

		MockNode firstNode = new MockNode("");
		firstNode.setProperty("oauth.client.id", "123");
		firstNode.setProperty("oauth.client.secret", "xyz");
		nodes[0] = firstNode;

		twitterConfigNode = new MockNode(nodes);
	}

	private void populateTwitterResourceNode() throws RepositoryException {
		twitterResourceNode = new MockNode("");
		twitterResourceNode.setProperty("username", "sachinmali");

	}

	@Test
	public void test_GivenThereAreNoTwitterResources_WhenRefreshTwitterFeedInvoked_ThenGetTweetsAsListIsNotCalled()
			throws RepositoryException {

		when(searchResult.getHits()).thenReturn(null);

		twitterFeedService.refreshTwitterFeed(resourceResolver, twitterComponentPaths);

		verify(twitterOAuthCommunicator, never()).getTweetsAsList(any(TwitterConfiguration.class));
	}

	@Test
	public void test_GivenThereIsNoTwitterCloudConfiguration_WhenRefreshTwitterFeedInvoked_ThenGetTweetsAsListIsNotCalled()
			throws RepositoryException {

		when(configurationManager.getConfiguration(anyString(), any(String[].class))).thenReturn(null);

		twitterFeedService.refreshTwitterFeed(resourceResolver, twitterComponentPaths);

		verify(twitterOAuthCommunicator, never()).getTweetsAsList(any(TwitterConfiguration.class));
	}

	@Test
	public void test_GivenThereIsInvalidTwitterCloudConfiguration_WhenRefreshTwitterFeedInvoked_ThenGetTweetsAsListIsNotCalled()
			throws RepositoryException {

		twitterResourceNode = new MockNode("");
		when(twitterResource.adaptTo(Node.class)).thenReturn(twitterResourceNode);

		twitterFeedService.refreshTwitterFeed(resourceResolver, twitterComponentPaths);

		verify(twitterOAuthCommunicator, never()).getTweetsAsList(any(TwitterConfiguration.class));
	}

	@Test
	public void test_GivenThereAreSameUserNameTwitterResources_WhenRefreshTwitterFeedInvoked_ThenGetTweetsAsListIsCalledOnce()
			throws RepositoryException {

		Resource twitterResource1 = mock(Resource.class);

		Node twitterResourceNode1 = new MockNode("");
		twitterResourceNode1.setProperty("username", "sachinmali");

		hits.add(new HitStub(twitterResource1));
		when(searchResult.getHits()).thenReturn(hits);
		when(pageManager.getContainingPage(twitterResource1)).thenReturn(page);
		when(twitterResource1.adaptTo(Node.class)).thenReturn(twitterResourceNode1);

		twitterFeedService.refreshTwitterFeed(resourceResolver, twitterComponentPaths);

		verify(twitterOAuthCommunicator, times(1)).getTweetsAsList(any(TwitterConfiguration.class));
	}
	
	@Test
	public void test_GivenThereAreDifferentUserNameTwitterResources_WhenRefreshTwitterFeedInvoked_ThenGetTweetsAsListIsCalledSeparately()
			throws RepositoryException {

		Resource twitterResource1 = mock(Resource.class);

		Node twitterResourceNode1 = new MockNode("");
		twitterResourceNode1.setProperty("username", "justinedelson");

		hits.add(new HitStub(twitterResource1));
		when(searchResult.getHits()).thenReturn(hits);
		when(pageManager.getContainingPage(twitterResource1)).thenReturn(page);
		when(twitterResource1.adaptTo(Node.class)).thenReturn(twitterResourceNode1);

		twitterFeedService.refreshTwitterFeed(resourceResolver, twitterComponentPaths);

		verify(twitterOAuthCommunicator, times(2)).getTweetsAsList(any(TwitterConfiguration.class));
	}

	@Test
	public void test_GivenHappyScenario_WhenRefreshTwitterFeedInvoked_ThenGetTweetsAsListIsCalled() throws RepositoryException {

		twitterFeedService.refreshTwitterFeed(resourceResolver, twitterComponentPaths);

		verify(twitterOAuthCommunicator).getTweetsAsList(any(TwitterConfiguration.class));
	}

	class HitStub implements Hit {

		private Resource resource;

		public HitStub(Resource resource) {
			this.resource = resource;
		}

		@Override
		public String getExcerpt() throws RepositoryException {
			return null;
		}

		@Override
		public Map<String, String> getExcerpts() throws RepositoryException {
			return null;
		}

		@Override
		public long getIndex() {
			return 0;
		}

		@Override
		public Node getNode() throws RepositoryException {
			return null;
		}

		@Override
		public String getPath() throws RepositoryException {
			return null;
		}

		@Override
		public ValueMap getProperties() throws RepositoryException {
			return null;
		}

		@Override
		public Resource getResource() throws RepositoryException {
			return resource;
		}

		@Override
		public String getTitle() throws RepositoryException {
			return null;
		}

	}
}
