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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.jcr.Session;

import com.adobe.acs.commons.search.CloseableQuery;
import com.adobe.acs.commons.search.CloseableQueryBuilder;
import com.adobe.acs.commons.wrap.cqsearch.QueryIWrap;
import com.adobe.acs.commons.wrap.jcr.BaseSessionIWrap;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.facets.Bucket;
import com.day.cq.search.result.SearchResult;
import org.apache.jackrabbit.api.JackrabbitSession;
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
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnitRunner;


public class CloseableQueryBuilderImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private ResourceResolver contextResolver;

    @Mock
    QueryBuilder mockQueryBuilder;

    CloseableQueryBuilderImpl closeableQueryBuilder = new CloseableQueryBuilderImpl();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context.registerService(QueryBuilder.class, mockQueryBuilder);
        contextResolver = context.create().resource("/test").getResourceResolver();
        contextResolver.commit();
        context.registerInjectActivateService(closeableQueryBuilder);
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

        assertTrue("hitResolver should be live", hitResolver.isLive());

        try (CloseableQuery closeableQuery = closeableQueryBuilder.createQuery(mockSession)) {
            /* should not throw exceptions */

            assertTrue("resource resolver from first hit should be live",
                    closeableQuery.getResult().getResources().next().getResourceResolver().isLive());
        }

        assertFalse("hitResolver should not be live", hitResolver.isLive());
    }

    @Test
    public void testCreateQuery() {
        final JackrabbitSession mockJackrabbitSession = mock(JackrabbitSession.class);
        final Session mockJcrSession = mock(Session.class);
        final Query mockQuery = mock(Query.class);

        when(mockQueryBuilder.createQuery(any(Session.class))).then((invocation) -> {
            Session sessionArg = invocation.getArgument(0);
            assertTrue("query session should be a wrapper: " + sessionArg.getClass().getName(),
                    sessionArg instanceof BaseSessionIWrap);
            return mockQuery;
        });

        assertNotNull("returned query should not be null with resourceResolver",
                closeableQueryBuilder.createQuery(contextResolver));

        assertNotNull("returned query should not be null with mocked JCR session",
                closeableQueryBuilder.createQuery(mockJcrSession));

        assertNotNull("returned query should not be null with mocked Jackrabbit session",
                closeableQueryBuilder.createQuery(mockJackrabbitSession));

        final PredicateGroup predicates = new PredicateGroup();
        when(mockQuery.getPredicates()).thenReturn(predicates);

        when(mockQueryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).then((invocation) -> {
            PredicateGroup predicatesArg = invocation.getArgument(0);
            assertSame("Same predicate group should be passed through", predicates, predicatesArg);
            Session sessionArg = invocation.getArgument(1);
            assertTrue("query session should be a wrapper: " + sessionArg.getClass().getName(),
                    sessionArg instanceof BaseSessionIWrap);
            return mockQuery;
        });

        assertNotNull("returned query w/resolver and preds should not be null",
                closeableQueryBuilder.createQuery(predicates, contextResolver));
        assertSame("returned query w/resolver should have same preds", predicates,
                closeableQueryBuilder.createQuery(predicates, contextResolver).getPredicates());
        assertNotNull("returned query w/jcr and preds should not be null",
                closeableQueryBuilder.createQuery(predicates, mockJcrSession));
        assertSame("returned query w/jcr should have same preds", predicates,
                closeableQueryBuilder.createQuery(predicates, mockJcrSession).getPredicates());
        assertNotNull("returned query w/jackrabbit and preds should not be null",
                closeableQueryBuilder.createQuery(predicates, mockJackrabbitSession));
        assertSame("returned query w/jackrabbit should have same preds", predicates,
                closeableQueryBuilder.createQuery(predicates, mockJackrabbitSession).getPredicates());

    }

    @Test
    public void testLoadQuery() throws Exception {
        final JackrabbitSession mockJackrabbitSession = mock(JackrabbitSession.class);
        final Session mockJcrSession = mock(Session.class);
        final Query mockQuery = mock(Query.class);

        when(mockQueryBuilder.loadQuery(any(String.class), any(Session.class))).then((invocation) -> {
            String pathArg = invocation.getArgument(0);
            Session sessionArg = invocation.getArgument(1);
            assertTrue("query session should be a wrapper: " + sessionArg.getClass().getName(),
                    sessionArg instanceof BaseSessionIWrap);
            if ("".equals(pathArg)) {
                return null;
            }
            return mockQuery;
        });

        assertNotNull("load query should not be null with resourceResolver and path",
                closeableQueryBuilder.loadQuery("/somepath", contextResolver));

        assertNotNull("load query should not be null with mocked JCR session and path",
                closeableQueryBuilder.loadQuery("/somepath", mockJcrSession));

        assertNotNull("load query should not be null with mocked Jackrabbit session and path",
                closeableQueryBuilder.loadQuery("/somepath", mockJackrabbitSession));

        assertNull("load query should be null with resourceResolver and empty path",
                closeableQueryBuilder.loadQuery("", contextResolver));

        assertNull("load query should be null with mocked JCR session and empty path",
                closeableQueryBuilder.loadQuery("", mockJcrSession));

        assertNull("load query should be null with mocked Jackrabbit session and empty path",
                closeableQueryBuilder.loadQuery("", mockJackrabbitSession));

    }

    @Test
    public void testStoreQuery() throws Exception {
        final JackrabbitSession mockJackrabbitSession = mock(JackrabbitSession.class);
        final Session mockJcrSession = mock(Session.class);
        final Query mockQuery = mock(Query.class);

        doAnswer((invocation) -> {
            Session sessionArg = invocation.getArgument(3);
            assertTrue("query session should NOT be a session wrapper: " + sessionArg.getClass().getName(),
                    !(sessionArg instanceof BaseSessionIWrap));
            return null;
        }).when(mockQueryBuilder).storeQuery(isA(Query.class), isA(String.class), anyBoolean(), any(Session.class));

        closeableQueryBuilder.storeQuery(mockQuery, "/somepath", true, contextResolver);
        closeableQueryBuilder.storeQuery(mockQuery, "/somepath", true, mockJcrSession);
        closeableQueryBuilder.storeQuery(mockQuery, "/somepath", true, mockJackrabbitSession);

        verify(mockQueryBuilder, times(3))
                .storeQuery(isA(Query.class), isA(String.class), anyBoolean(), any(Session.class));
    }

    @Test
    public void testClearFacetCache() {

        closeableQueryBuilder.clearFacetCache();
        verify(mockQueryBuilder, times(1)).clearFacetCache();

    }

    @Test
    public void testRefineQuery() {
        final Query mockQuery = mock(Query.class);
        final Bucket bucket = mock(Bucket.class);
        final Query mockRefinedQuery = mock(Query.class);

        when(mockQuery.refine(bucket)).thenReturn(mockRefinedQuery);
        when(mockQueryBuilder.createQuery(any(Session.class))).thenReturn(mockQuery);

        Query refinedQuery = closeableQueryBuilder.createQuery(contextResolver).refine(bucket);
        assertNotSame("refined query should not be same as mock", refinedQuery, mockRefinedQuery);
        assertTrue("refined query should also be wrapper: " + refinedQuery.getClass().getName(),
                refinedQuery instanceof QueryIWrap);
    }

    @Test
    public void testCloseableQueryImpl_wrapQuery() {
        final Query mockQuery = mock(Query.class);
        final CloseableQueryBuilderImpl.CloseableQueryImpl closeableQuery =
                new CloseableQueryBuilderImpl.CloseableQueryImpl(mockQuery);

        final Query mockOtherQuery = mock(Query.class);
        final CloseableQueryBuilderImpl.CloseableQueryImpl closeableOtherQuery =
                new CloseableQueryBuilderImpl.CloseableQueryImpl(mockOtherQuery);

        final Query wrappedOther = closeableQuery.wrapQuery(mockOtherQuery);
        assertNotSame("wrapped other should not be same as mock other", mockOtherQuery, wrappedOther);

        final Query wrappedCloseableOther = closeableQuery.wrapQuery(closeableOtherQuery);
        assertSame("wrapped closeable other should be same as closable other", closeableOtherQuery,
                wrappedCloseableOther);
    }

    @Test
    public void testGetAdapter() {
        assertSame("getAdapter should return same instance with ResourceResolver and correct adapterType.",
                closeableQueryBuilder, closeableQueryBuilder.getAdapter(contextResolver, CloseableQueryBuilder.class));
        assertNull("getAdapter should return null for non-ResourceResolver adapter",
                closeableQueryBuilder.getAdapter(context.request(), CloseableQueryBuilder.class));
        assertNull("getAdapter should return null for incorrect adapterType",
                closeableQueryBuilder.getAdapter(contextResolver, QueryBuilder.class));
    }
}
