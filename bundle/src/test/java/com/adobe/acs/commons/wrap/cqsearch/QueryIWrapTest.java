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
package com.adobe.acs.commons.wrap.cqsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.eval.PredicateEvaluator;
import com.day.cq.search.facets.Bucket;
import com.day.cq.search.result.SearchResult;

public class QueryIWrapTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Query query;

    @Mock
    Query refinedQuery;

    @Mock
    SearchResult searchResult;

    PredicateGroup predicates = new PredicateGroup();

    @Before
    public void setUp() throws Exception {
        when(query.getStart()).thenReturn(10L);
        when(query.getHitsPerPage()).thenReturn(42L);
        when(query.getExcerpt()).thenReturn(true);
        when(query.getPredicates()).thenReturn(predicates);
        when(query.refine(any(Bucket.class))).thenReturn(refinedQuery);
        when(query.getResult()).thenReturn(searchResult);
    }

    @Test
    public void testQueryWrapper() {
        WrapQuery wrapQuery = new WrapQuery(query);

        assertEquals("getStart() should return 10L", 10L, wrapQuery.getStart());
        assertEquals("getHitsPerPage() should return 42L", 42L, wrapQuery.getHitsPerPage());
        assertTrue("getExcerpt() should return true", wrapQuery.getExcerpt());
        assertSame("getPredicates() should return mocked predicates", predicates, wrapQuery.getPredicates());
        assertSame("getResult() should return mocked SearchResult", searchResult, wrapQuery.getResult());

        assertSame("wrapQuery.wrapQuery(refinedQuery) should return refinedQuery", refinedQuery,
                wrapQuery.wrapQuery(refinedQuery));

        wrapQuery.setStart(0L);
        verify(query, times(1)).setStart(0L);
        wrapQuery.setHitsPerPage(10L);
        verify(query, times(1)).setHitsPerPage(10L);
        wrapQuery.setExcerpt(false);
        verify(query, times(1)).setExcerpt(false);

        final String predType = "testPredicate";
        final PredicateEvaluator mockPredEval = mock(PredicateEvaluator.class);

        doAnswer((invocation) -> {
            String predTypeArg = invocation.getArgument(0);
            PredicateEvaluator predEvalArg = invocation.getArgument(1);
            assertEquals("expect same predicate type arg", predType, predTypeArg);
            assertSame("expect same PredicateEvaluator instance arg", mockPredEval, predEvalArg);
            return null;
        }).when(query).registerPredicateEvaluator(anyString(), any(PredicateEvaluator.class));

        wrapQuery.registerPredicateEvaluator(predType, mockPredEval);

        verify(query, times(1)).registerPredicateEvaluator(anyString(), any(PredicateEvaluator.class));
    }

    static class WrapQuery implements QueryIWrap {
        final Query wrapped;

        public WrapQuery(final Query wrapped) {
            this.wrapped = wrapped;
        }

        @Nonnull
        @Override
        public Query unwrapQuery() {
            return wrapped;
        }
    }
}
