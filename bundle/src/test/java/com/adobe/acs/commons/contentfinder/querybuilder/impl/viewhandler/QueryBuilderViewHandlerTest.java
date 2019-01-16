package com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;

import com.adobe.acs.commons.search.CloseableQuery;
import com.adobe.acs.commons.search.CloseableQueryBuilder;
import com.day.cq.search.Predicate;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class QueryBuilderViewHandlerTest {
    private static final Logger LOG = LoggerFactory.getLogger(QueryBuilderViewHandlerTest.class);

    @Rule
    public SlingContext slingContext = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Mock
    CloseableQueryBuilder queryBuilder;

    @Mock
    CloseableQuery query;

    @Mock
    SearchResult searchResult;

    @Before
    public void setUp() throws Exception {
        slingContext.registerService(CloseableQueryBuilder.class, queryBuilder);
        when(searchResult.getHits()).thenReturn(new ArrayList<>());
        when(query.getResult()).thenReturn(searchResult);
        when(queryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).then(invocation -> {
            PredicateGroup predicates = invocation.getArgumentAt(0, PredicateGroup.class);
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
