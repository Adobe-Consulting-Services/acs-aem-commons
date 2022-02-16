/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateRedirectConfigurationServletTest {

    CreateRedirectConfigurationServlet servlet;

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Before
    public void setUp() {
        servlet = new CreateRedirectConfigurationServlet();
        RedirectFilterMBean redirectFilter = mock(RedirectFilterMBean.class);
        when(redirectFilter.getBucket()).thenReturn("settings");
        when(redirectFilter.getConfigName()).thenReturn("redirects");
        servlet.redirectFilter = redirectFilter;
    }

    @Test
    public void createConfig() throws ServletException, IOException   {
        context.build().resource("/conf/global");
        context.request().addRequestParameter("path", "/conf/global");
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        Resource cfg = context.resourceResolver().getResource("/conf/global/settings/redirects");
        assertNotNull(cfg);

        // return 409 if already exists
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_CONFLICT, context.response().getStatus());
    }
}
