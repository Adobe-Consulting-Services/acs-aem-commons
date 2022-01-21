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
package com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler;

import com.adobe.acs.commons.util.impl.QueryHelperImpl;
import com.day.cq.search.QueryBuilder;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class QueryBuilderViewQueryTest {
    private static final String EXPECT_TITLE = "Foo";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    com.day.cq.search.Query query;

    @Mock
    com.day.cq.search.result.SearchResult searchResult;

    @Mock
    com.day.cq.search.result.Hit hit;

    QueryHelperImpl helper = new QueryHelperImpl();

    @Before
    public void setUp() throws Exception {
        context.registerService(QueryBuilder.class, queryBuilder);
        context.registerInjectActivateService(helper);
        when(query.getResult()).thenReturn(searchResult);
        final Resource root = context.currentResource("/");
        assertNotNull("expect root not null", root);
        final Resource foo = context.resourceResolver().create(root, "foo", Stream.of("jcr:primaryType=sling:Folder", "jcr:title=" + EXPECT_TITLE)
                .map(pair -> pair.split("="))
                .collect(Collectors.toMap(value -> value[0], value -> value[1])));
        context.resourceResolver().create(foo, "bar", singletonMap("jcr:primaryType", "sling:Folder"));
        context.resourceResolver().commit();
        when(hit.getResource()).thenReturn(foo);
        when(searchResult.getHits()).thenReturn(singletonList(hit));
    }

    @Test
    public void testExecute() {
        final QueryBuilderViewQuery viewQuery = new QueryBuilderViewQuery(query);
        Collection<com.day.cq.wcm.core.contentfinder.Hit> hits = viewQuery.execute();
        assertFalse("expect nonempty", hits.isEmpty());
        assertEquals("expect path", EXPECT_TITLE, hits.iterator().next().get("title"));
    }

    @Test
    public void testExecute_withException() throws Exception {
        doThrow(RepositoryException.class).when(hit).getExcerpt();
        final QueryBuilderViewQuery viewQuery = new QueryBuilderViewQuery(query);
        Collection<com.day.cq.wcm.core.contentfinder.Hit> hits = viewQuery.execute();
        assertTrue("expect empty", hits.isEmpty());
    }

}