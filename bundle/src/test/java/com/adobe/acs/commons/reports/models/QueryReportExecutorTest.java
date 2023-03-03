/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.reports.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;

import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(Parameterized.class)
public class QueryReportExecutorTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "SELECT * FROM [nt:unstructured] WHERE ISDESCENDANTNODE([/test])", "JCR-SQL2" },
                { "/jcr:root/test//* order by @jcr:score", "xpath" },
                { "path=/test", "queryBuilder" }
        });
    }

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    private final String statement;
    private final String language;

    private QueryBuilder queryBuilder;

    public QueryReportExecutorTest(String statement, String language) {
        this.statement = statement;
        this.language = language;
    }

    @Before
    public void before() {
        context.create().resource("/test",
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));
        context.create().resource("/test/item1",
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));
        context.create().resource("/test/item2",
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));

        queryBuilder = mock(QueryBuilder.class);

        List<Resource> resources = new ArrayList<>();
        resources.add(context.resourceResolver().getResource("/test/item1"));
        resources.add(context.resourceResolver().getResource("/test/item2"));
        SearchResult result = mock(SearchResult.class);
        when(result.getHits()).then(inv -> {
            return resources.stream().map(r -> {
                Hit hit = mock(Hit.class);
                try {
                    when(hit.getPath()).thenReturn(r.getPath());
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
                return hit;
            }).collect(Collectors.toList());
        });

        Query query = mock(Query.class);
        when(query.getResult()).thenReturn(result);
        when(queryBuilder.createQuery(any(), any())).thenReturn(query);

    }

    private Resource createConfig(int pageSize, String query, String queryLanguage) {

        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        properties.put("pageSize", pageSize);
        properties.put("query", query);
        properties.put("queryLanguage", queryLanguage);

        return context.create().resource("/conf/" + UUID.randomUUID().toString(), properties);
    }

    @Test
    public void supportsQueries() throws ReportException {
        QueryReportExecutor executor = new QueryReportExecutor(context.request(), queryBuilder);
        executor.setConfiguration(
                createConfig(10, statement, language));
        executor.setPage(0);

        ResultsPage results = executor.getResults();
        assertNotNull(results);
        assertEquals(2, results.getResultSize());
    }

    @Test
    public void supportsDetails() throws ReportException {
        QueryReportExecutor executor = new QueryReportExecutor(context.request(), queryBuilder);
        executor.setConfiguration(
                createConfig(10, statement, language));
        executor.setPage(0);

        String details = executor.getDetails();
        assertNotNull(details);
        assertTrue(details.contains(language));
    }

}
