/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2022 Adobe
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
package com.adobe.acs.commons.dam.impl;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.osgi.service.event.Event;

import javax.jcr.Session;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReviewTaskAssetMoverHandlerTest {
    private static final String PATH_FROM_ASSET = "/content/dam/asset.png";
    private static final String PATH_REJECTED_TO = "/content/dam/rejected";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    AssetManager assetManager;

    @Mock
    Asset asset;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query query;

    @Mock
    SearchResult result;

    @Mock
    Hit hit;

    @Mock
    Scheduler scheduler;

    @Mock
    ScheduleOptions scheduleOptions;

    ReviewTaskAssetMoverHandler handler;

    private void setupQueryBuilder() throws Exception {
        when(hit.getPath()).thenReturn(PATH_FROM_ASSET);
        when(hit.getResource()).then(call -> context.resourceResolver().getResource(PATH_FROM_ASSET));
        when(result.getResources()).then(call ->
                Collections.singleton(context.resourceResolver().getResource(PATH_FROM_ASSET))
                        .iterator());
        when(result.getHits()).thenReturn(Collections.singletonList(hit));
        when(query.getResult()).thenReturn(result);
        doReturn(query).when(queryBuilder).createQuery(any(PredicateGroup.class), any(Session.class));
        context.registerService(QueryBuilder.class, queryBuilder);
    }

    private void setupScheduler() {
        when(scheduler.NOW()).thenReturn(scheduleOptions);
        doAnswer(call -> {
            final Runnable job = call.getArgument(0);
            job.run();
            return true;
        }).when(scheduler).schedule(any(Runnable.class), any(ScheduleOptions.class));
        context.registerService(Scheduler.class, scheduler);
    }

    private void setupAssetManager() {
        when(asset.getValueMap()).then(call -> context.resourceResolver().getResource(PATH_FROM_ASSET).getValueMap());
        doReturn(asset).when(assetManager).getAsset(PATH_FROM_ASSET);
        context.registerAdapter(ResourceResolver.class, AssetManager.class, assetManager);
    }

    @Before
    public void setUp() throws Exception {
        context.load().json("/com/adobe/acs/commons/dam/impl/ReviewTaskAssetMoverHandlerTest.json", "/content/dam");
        setupAssetManager();
        setupQueryBuilder();
        setupScheduler();
        handler = new ReviewTaskAssetMoverHandler();
        context.registerInjectActivateService(handler, Collections.emptyMap());

        final Resource root = context.currentResource("/");
        final Resource task = context.resourceResolver().create(root, "task",
                Stream.of(
                                "jcr:primaryType=sling:Folder",
                                "contentPath=" + PATH_FROM_ASSET,
                                "onRejectMoveTo=" + PATH_REJECTED_TO)
                        .map(pair -> pair.split("="))
                        .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1])));
        context.resourceResolver().commit();
        context.currentResource(task);
    }

    @Test
    public void testUnbind() {
        handler.unbindQueryBuilder(queryBuilder);
        handler.unbindScheduler(scheduler);
        handler.unbindResourceResolverFactory(context.getService(ResourceResolverFactory.class));
    }

    @Test
    public void testHandleEventFailLogin() throws LoginException {
        handler.unbindResourceResolverFactory(context.getService(ResourceResolverFactory.class));
        final ResourceResolverFactory failingResolverFactory = mock(ResourceResolverFactory.class);
        doThrow(LoginException.class).when(failingResolverFactory).getServiceResourceResolver(anyMap());
        handler.bindResourceResolverFactory(failingResolverFactory);
        handler.handleEvent(new Event(ReviewTaskAssetMoverHandler.DEFAULT_TOPIC, Collections.singletonMap("TaskId", "/task")));
    }

    @Test
    public void testHandleEvent() {
        handler.handleEvent(new Event(ReviewTaskAssetMoverHandler.DEFAULT_TOPIC, Collections.singletonMap("TaskId", "/task")));
    }

}