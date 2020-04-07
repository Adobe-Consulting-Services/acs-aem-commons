/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;

import com.adobe.acs.commons.search.CloseableQuery;
import com.adobe.acs.commons.search.CloseableQueryBuilder;
import com.day.cq.commons.predicate.PredicateProvider;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.core.contentfinder.ViewQuery;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueryBuilderViewHandlerTest {
    private static final Logger LOG = LoggerFactory.getLogger(QueryBuilderViewHandlerTest.class);
    
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public SlingContext slingContext = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Mock
    CloseableQueryBuilder queryBuilder;

    @Mock
    CloseableQuery query;

    @Mock
    SearchResult searchResult;

    @Mock
    PredicateProvider predicateProvider;

    @Before
    public void setUp() throws Exception {
        slingContext.registerService(PredicateProvider.class, predicateProvider);
        slingContext.registerService(CloseableQueryBuilder.class, queryBuilder);
        when(searchResult.getHits()).thenReturn(new ArrayList<>());
        when(query.getResult()).thenReturn(searchResult);
        when(queryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).then(invocation -> {
            PredicateGroup predicates = invocation.getArgument(0);
            LOG.info("predicates: {}", predicates);
            return query;
        });
    }

    @Test
    public void testActivate() throws Exception {
        QueryBuilderViewHandler viewHandler = slingContext.registerInjectActivateService(new QueryBuilderViewHandler());
        MockSlingHttpServletRequest request = slingContext.request();
        Map<String, Object> params = new HashMap<>();
        params.put(ContentFinderConstants.CF_TYPE, "cq:Page");
        request.setParameterMap(params);
        ViewQuery viewQuery = viewHandler.createQuery(request, slingContext.resourceResolver().adaptTo(Session.class), "");

        viewQuery.execute();
        assertNotNull("", viewQuery.execute());
    }
}
