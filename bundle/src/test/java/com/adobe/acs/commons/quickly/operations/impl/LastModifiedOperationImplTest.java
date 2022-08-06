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
package com.adobe.acs.commons.quickly.operations.impl;

import com.adobe.acs.commons.quickly.Command;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class LastModifiedOperationImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query query;

    @Mock
    SearchResult result;

    @Mock
    Hit hit;

    @Mock
    SlingHttpServletRequest baseRequest;

    @Mock
    SlingHttpServletResponse baseResponse;

    LastModifiedOperationImpl operation;

    private void setupQueryBuilder() throws Exception {
        when(result.getHits()).thenReturn(Collections.singletonList(hit));
        when(query.getResult()).thenReturn(result);
        doReturn(query).when(queryBuilder).createQuery(any(PredicateGroup.class), any(Session.class));
        context.registerService(QueryBuilder.class, queryBuilder);
    }

    private void setupBaseRequest() throws Exception {
        when(baseRequest.getResourceResolver()).thenReturn(context.resourceResolver());
    }

    @Before
    public void setUp() throws Exception {
        context.load().json(getClass().getResourceAsStream("LastModifiedOperationImplTest.json"), "/content/geometrixx");
        setupQueryBuilder();
        setupBaseRequest();
        operation = new LastModifiedOperationImpl();
        context.registerInjectActivateService(operation);
    }

    private Command getRawCommand() {
        return new Command(LastModifiedOperationImpl.CMD);
    }

    @Test
    public void testAccepts() {
        assertTrue(operation.accepts(null, getRawCommand()));
    }

    @Test
    public void testGetCmd() {
        assertEquals(LastModifiedOperationImpl.CMD, operation.getCmd());
    }

    @Test
    public void testWithoutParams() {
        operation.withoutParams(baseRequest, baseResponse, getRawCommand());
    }

    @Test
    public void testWithParamsSuccessHit() throws Exception {
        when(hit.getPath()).thenReturn("/content/geometrixx/en");
        operation.withParams(baseRequest, baseResponse, getRawCommand());
    }

    @Test
    public void testWithParamsExceptionHit() throws Exception {
        when(hit.getPath()).thenThrow(RepositoryException.class);
        operation.withParams(baseRequest, baseResponse, getRawCommand());
    }
}