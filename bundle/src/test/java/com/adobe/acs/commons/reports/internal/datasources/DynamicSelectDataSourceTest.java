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

import com.adobe.acs.commons.mcp.util.StringUtil;
import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.wcm.datasources.DataSourceBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynamicSelectDataSourceTest {

    private static final Logger log = LoggerFactory.getLogger(DynamicSelectDataSourceTest.class);

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @InjectMocks
    private DynamicSelectDataSource servlet = new DynamicSelectDataSource();

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Resource resource;

    @Mock
    private ValueMap valueMap;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private DataSourceBuilder dataSourceBuilder;

    @Mock
    private QueryHelper queryHelper;

    String queryStatement = "select * FROM [cq:Component] as parent  where ISDESCENDANTNODE(parent, '/apps/'));";
    List<Resource> resourceList;

    @Before
    public void setup() throws Exception {

        log.info("setup method");
        resourceList = new ArrayList();
        resourceList.add(resource);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(request.getResource()).thenReturn(resource);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(servlet.PN_DROP_DOWN_QUERY_LANGUAGE,javax.jcr.query.Query.JCR_SQL2)).thenReturn(javax.jcr.query.Query.JCR_SQL2);
        when(valueMap.get(servlet.PN_DROP_DOWN_QUERY,String.class)).thenReturn(queryStatement);
        when(valueMap.get(servlet.PN_ALLOW_PROPERTY_NAMES,new String[0])).thenReturn(new String[]{"jcr:title"});
        when(queryHelper.findResources(resourceResolver,javax.jcr.query.Query.JCR_SQL2,queryStatement, StringUtils.EMPTY)).thenReturn(resourceList);
    }

    @Test
    public void doGetTest() throws Exception {

        log.info("doGetTest method");
        servlet.doGet(request,response);
    }
}
