/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.search.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.jcr.Session;

import com.adobe.acs.commons.search.CloseableQuery;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloseableQueryBuilderImplTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private ResourceResolver contextResolver;

    @Mock
    QueryBuilder mockQueryBuilder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context.registerService(QueryBuilder.class, mockQueryBuilder);
        contextResolver = context.create().resource("/test").getResourceResolver();
        contextResolver.commit();
    }

    @Test
    public void testTryWithResources() throws Exception {
        ResourceResolver hitResolver = context.resourceResolver().clone(null);
        Resource hitResource = hitResolver.getResource("/test");
        assertNotNull("hitResource getResourceResolver() should not be null",
                hitResource.getResourceResolver());
        assertNotSame("hitResource getResourceResolver() should not be same as context.resourceResolver()",
                contextResolver, hitResource.getResourceResolver());

        SearchResult mockSearchResult = mock(SearchResult.class);
        Session mockSession = hitResolver.adaptTo(Session.class);
        Query mockQuery = mock(Query.class);

        when(mockSearchResult.getResources()).then((invocation) -> Collections.singletonList(hitResource).iterator());
        when(mockQuery.getResult()).thenReturn(mockSearchResult);
        when(mockQueryBuilder.createQuery(any(Session.class))).thenReturn(mockQuery);

        CloseableQueryBuilderImpl cqb = new CloseableQueryBuilderImpl();
        context.registerInjectActivateService(cqb);

        assertTrue("hitResolver should be live", hitResolver.isLive());

        try (CloseableQuery closeableQuery = cqb.createQuery(mockSession)) {
            /* should not throw exceptions */

            assertTrue("resource resolver from first hit should be live",
                    closeableQuery.getResult().getResources().next().getResourceResolver().isLive());
        }

        assertFalse("hitResolver should not be live", hitResolver.isLive());
    }
}
