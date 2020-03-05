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
package com.adobe.acs.commons.reports.internal.datasources;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.query.Query;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.wcm.datasources.DataSourceBuilder;
import com.adobe.acs.commons.wcm.datasources.DataSourceOption;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class DynamicSelectDataSourceTest {


    @Rule
    public SlingContext context = new SlingContext();

    @Mock
    private DataSourceBuilder dataSourceBuilder;

    @Mock
    private QueryHelper queryHelper;
    
    @Captor
    ArgumentCaptor<List<DataSourceOption>> dsCaptor;
    
    private DynamicSelectDataSource servlet;

    String queryStatement = "select * FROM [cq:Component] as parent  where ISDESCENDANTNODE(parent, '/apps/'));";
    List<Resource> resourceList;

    @Before
    public void setup() throws Exception {
        
        resourceList = new ArrayList<>();
        
        // prepare services
        context.registerService(QueryHelper.class,queryHelper);
        when(queryHelper.findResources(any(), anyString(), anyString(), eq(StringUtils.EMPTY)))
                .thenReturn(resourceList);
        context.registerService(DataSourceBuilder.class,dataSourceBuilder);
        
        servlet = new DynamicSelectDataSource();
        context.registerInjectActivateService(servlet);
    }

    @Test
    public void testsWithSingleResource() throws Exception {
        
        // Prepare resources
        context.build().resource("/querynode", ImmutableMap.of(DynamicSelectDataSource.PN_DROP_DOWN_QUERY_LANGUAGE,Query.JCR_SQL2,
                DynamicSelectDataSource.PN_DROP_DOWN_QUERY,queryStatement,
                DynamicSelectDataSource.PN_ALLOW_PROPERTY_NAMES,"jcr:title")).commit();
        context.request().setResource(context.resourceResolver().getResource("/querynode"));
        
        // test data
        context.build().resource("/result1", ImmutableMap.of("prop1","value1","jcr:title","someTitle")).commit();
        resourceList.add(context.resourceResolver().getResource("/result1"));

        servlet.doGet(context.request(),context.response());
        verify(dataSourceBuilder,times(1)).addDataSource(eq(context.request()),dsCaptor.capture());
        assertEquals(1,dsCaptor.getValue().size());
    }
    
    @Test
    public void testWithMultipleResources() throws Exception {
        
        // Prepare resources
        context.build().resource("/querynode", ImmutableMap.of(DynamicSelectDataSource.PN_DROP_DOWN_QUERY_LANGUAGE,Query.JCR_SQL2,
                DynamicSelectDataSource.PN_DROP_DOWN_QUERY,queryStatement,
                DynamicSelectDataSource.PN_ALLOW_PROPERTY_NAMES,"jcr:title")).commit();
        context.request().setResource(context.resourceResolver().getResource("/querynode"));
        
        // test data
        context.build().resource("/result1", ImmutableMap.of("prop1","value1","jcr:title","someTitle")).commit();
        context.build().resource("/result2", ImmutableMap.of("prop1","value1","jcr:title","someTitle")).commit();
        resourceList.add(context.resourceResolver().getResource("/result1"));
        resourceList.add(context.resourceResolver().getResource("/result2"));

        servlet.doGet(context.request(),context.response());
        verify(dataSourceBuilder,times(1)).addDataSource(eq(context.request()),dsCaptor.capture());
        assertEquals(1,dsCaptor.getValue().size());
    }
    
    @Test
    public void testWithNoQueryStringSpecified() throws Exception {
        
        // Prepare resources
        context.build().resource("/querynode", ImmutableMap.of(DynamicSelectDataSource.PN_DROP_DOWN_QUERY_LANGUAGE,Query.JCR_SQL2,
                DynamicSelectDataSource.PN_DROP_DOWN_QUERY,"", // no query string
                DynamicSelectDataSource.PN_ALLOW_PROPERTY_NAMES,"jcr:title")).commit();
        context.request().setResource(context.resourceResolver().getResource("/querynode"));
        
        servlet.doGet(context.request(),context.response());
        verify(dataSourceBuilder,times(1)).addDataSource(eq(context.request()),dsCaptor.capture());
        assertEquals(0,dsCaptor.getValue().size());
    }
    
}
