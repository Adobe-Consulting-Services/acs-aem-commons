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

import com.day.cq.search.QueryBuilder;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.osgi.service.event.Event;

import java.util.Collections;

import static org.junit.Assert.*;

public class ReviewTaskAssetMoverHandlerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Scheduler scheduler;

    ReviewTaskAssetMoverHandler handler;

    @Before
    public void setUp() throws Exception {
        context.registerService(QueryBuilder.class, queryBuilder);
        context.registerService(Scheduler.class, scheduler);
        handler = new ReviewTaskAssetMoverHandler();
        context.registerInjectActivateService(handler, Collections.emptyMap());
    }

    @Test
    public void testHandleEvent() {
        handler.handleEvent(new Event(ReviewTaskAssetMoverHandler.DEFAULT_TOPIC, Collections.emptyMap()));
    }
}