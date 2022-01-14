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
package com.adobe.acs.commons.util.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryHelperImplTest {
    private static final String STATEMENT = "//element(*, nt:base)";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    com.day.cq.search.Query query;

    @Mock
    com.day.cq.search.result.SearchResult searchResult;

    QueryHelperImpl helper = new QueryHelperImpl();

    @Before
    public void setUp() throws Exception {
        context.registerService(QueryBuilder.class, queryBuilder);
        context.registerInjectActivateService(helper);
        when(searchResult.getQueryStatement()).thenReturn(STATEMENT);
        when(query.getResult()).thenReturn(searchResult);
        doReturn(query).when(queryBuilder).createQuery(any(PredicateGroup.class), any(Session.class));
    }

    @Test
    public void testIsTraversal() throws Exception {
        final Resource root = context.currentResource("/");
        assertNotNull("expect root not null", root);
        context.resourceResolver().create(root, "foo", singletonMap("jcr:primaryType", "sling:Folder"));
        context.resourceResolver().commit();
        assertTrue("expect is traversal", helper.isTraversal(context.resourceResolver(), Query.JCR_SQL2, "select * from [nt:base]"));
    }

    @Test
    public void testGetResourcesFromQuery() throws Exception {
        final Resource root = context.currentResource("/");
        assertNotNull("expect root not null", root);
        final Resource foo = context.resourceResolver().create(root, "foo", singletonMap("jcr:primaryType", "sling:Folder"));
        final Resource bar = context.resourceResolver().create(foo, "bar", singletonMap("jcr:primaryType", "sling:Folder"));
        context.resourceResolver().commit();
        List<Resource> results = helper.findResources(context.resourceResolver(), Query.JCR_SQL2, "select * from [sling:Folder] where name() = 'foo'", "bar");
        assertEquals("expect result", bar.getPath(), results.get(0).getPath());
    }

    @Test
    public void testGetResourcesFromQueryBuilder() throws Exception {
        final Resource root = context.currentResource("/");
        assertNotNull("expect root not null", root);
        final Resource foo = context.resourceResolver().create(root, "foo", singletonMap("jcr:primaryType", "sling:Folder"));
        final Resource bar = context.resourceResolver().create(foo, "bar", singletonMap("jcr:primaryType", "sling:Folder"));
        context.resourceResolver().commit();
        final Hit hit = mock(Hit.class);
        when(hit.getResource()).thenReturn(foo);
        when(hit.getPath()).thenReturn(foo.getPath());
        when(searchResult.getHits()).thenReturn(singletonList(hit));
        List<Resource> results = helper.findResources(context.resourceResolver(), QueryHelperImpl.QUERY_BUILDER, "type=sling:Folder\nnodename=foo", "bar");
        assertEquals("expect result", bar.getPath(), results.get(0).getPath());
    }

    @Test
    public void testIsTraversalQueryBuilder() throws Exception {
        final Resource root = context.currentResource("/");
        assertNotNull("expect root not null", root);
        context.resourceResolver().create(root, "foo", singletonMap("jcr:primaryType", "sling:Folder"));
        context.resourceResolver().commit();
        assertTrue("expect is traversal", helper.isTraversal(context.resourceResolver(), singletonMap("type", "nt:base")));
    }
}