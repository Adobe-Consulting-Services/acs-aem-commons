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
import static org.mockito.Mockito.when;

import javax.annotation.Nonnull;

import com.day.cq.search.Query;
import com.day.cq.search.result.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueryIWrapTest {

    @Mock
    Query query;

    @Mock
    SearchResult searchResult;

    long start = 10L;

    @Before
    public void setUp() throws Exception {
        when(query.getResult()).thenReturn(searchResult);
        when(query.getStart()).then((invocation) -> start);
    }

    @Test
    public void testQueryWrapper() {
        WrapQuery wrapQuery = new WrapQuery(query);
        assertEquals("getStart() should return 10L", 10L, wrapQuery.getStart());
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
