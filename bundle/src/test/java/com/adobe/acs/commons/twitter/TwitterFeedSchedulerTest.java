package com.adobe.acs.commons.twitter;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import junitx.util.PrivateAccessor;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class TwitterFeedSchedulerTest {

	private TwitterFeedScheduler scheduler;

	@Mock
	private TwitterFeedService twitterFeedService;

	@Mock
	private ResourceResolverFactory resourceResolverFactory;

	@Mock
	private ResourceResolver resourceResolver;

	@Mock
	private Logger LOGGER;

	private String[] twitterComponentPaths = { "acs-commons//components//content//twitter-feed" };

	@Before
	public void setUp() throws Exception {

		scheduler = new TwitterFeedScheduler();

		PrivateAccessor.setField(scheduler, "resourceResolverFactory", resourceResolverFactory);
		PrivateAccessor.setField(scheduler, "twitterFeedService", twitterFeedService);
		PrivateAccessor.setField(scheduler, "twitterComponentPaths", twitterComponentPaths);
		PrivateAccessor.setField(scheduler, "LOGGER", LOGGER);

		when(resourceResolverFactory.getAdministrativeResourceResolver(anyMapOf(String.class, Object.class))).thenReturn(
				resourceResolver);

	}

	@Test
	public void testDefaultInstanceBehaviour() {
		assertFalse(scheduler.getIsMasterInstance());
	}

	@Test
	public void test_GivenItsMasterInstance_WhenRunIsInvoked_ThenCallsService() throws Exception {
		PrivateAccessor.setField(scheduler, "isMasterInstance", true);
		scheduler.run();
		verify(twitterFeedService).refreshTwitterFeed(resourceResolver, twitterComponentPaths);

	}

	@Test
	public void test_GivenItsMasterInstance_WhenRunIsInvoked_ThenFinallyResourceResolverGetsClosed() throws Exception {
		PrivateAccessor.setField(scheduler, "isMasterInstance", true);
		scheduler.run();
		verify(resourceResolver).close();

	}

}
