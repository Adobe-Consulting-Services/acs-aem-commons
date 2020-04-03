/*
 * #%L
 * ACS AEM Commons Twitter Support Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.twitter.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import junitx.util.PrivateAccessor;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyView;

@RunWith(MockitoJUnitRunner.class)
public class TwitterFeedSchedulerTest {

    private TwitterFeedScheduler scheduler;

    @Mock
    private TwitterFeedUpdater twitterFeedService;

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Before
    public void setUp() throws Exception {

        scheduler = new TwitterFeedScheduler();

        PrivateAccessor.setField(scheduler, "resourceResolverFactory",
                resourceResolverFactory);
        PrivateAccessor.setField(scheduler, "twitterFeedService",
                twitterFeedService);

        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(
                resourceResolver);

    }

    public TopologyEvent createLeaderChangeEvent(boolean isLeader) {
        TopologyEvent te = mock(TopologyEvent.class);
        TopologyView view = mock(TopologyView.class);
        InstanceDescription instanceDescription = mock(InstanceDescription.class);
        when(te.getNewView()).thenReturn(view);
        when(view.getLocalInstance()).thenReturn(instanceDescription);
        when(instanceDescription.isLeader()).thenReturn(isLeader);
        return te;
    }
    
    @Test
    public void testDefaultInstanceBehaviour() throws NoSuchFieldException {
        boolean isLeader = (Boolean) PrivateAccessor.getField(scheduler,
                "isLeader");
        assertFalse(isLeader);
    }

    @Test
    public void test_GivenItsMasterInstance_WhenRunIsInvoked_ThenCallsService()
            throws Exception {
        scheduler.handleTopologyEvent(createLeaderChangeEvent(true));
        scheduler.run();
        verify(twitterFeedService).updateTwitterFeedComponents(resourceResolver);

    }

    @Test
    public void test_GivenItsMasterInstance_WhenRunIsInvoked_ThenFinallyResourceResolverGetsClosed()
            throws Exception {
        scheduler.handleTopologyEvent(createLeaderChangeEvent(true));
        scheduler.run();
        verify(resourceResolver).close();

    }

}
