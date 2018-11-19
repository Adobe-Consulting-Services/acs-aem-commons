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

import com.adobe.acs.commons.twitter.impl.TwitterFeedScheduler;

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

        when(
                resourceResolverFactory
                        .getServiceResourceResolver(anyMapOf(
                                String.class, Object.class))).thenReturn(
                resourceResolver);

    }

    @Test
    public void testDefaultInstanceBehaviour() throws NoSuchFieldException {
        boolean isMaster = (Boolean) PrivateAccessor.getField(scheduler,
                "isMaster");
        assertFalse(isMaster);
    }

    @Test
    public void test_GivenItsMasterInstance_WhenRunIsInvoked_ThenCallsService()
            throws Exception {
        scheduler.bindRepository("", "", true);
        scheduler.run();
        verify(twitterFeedService).updateTwitterFeedComponents(resourceResolver);

    }

    @Test
    public void test_GivenItsMasterInstance_WhenRunIsInvoked_ThenFinallyResourceResolverGetsClosed()
            throws Exception {
        scheduler.bindRepository("", "", true);
        scheduler.run();
        verify(resourceResolver).close();

    }

}
