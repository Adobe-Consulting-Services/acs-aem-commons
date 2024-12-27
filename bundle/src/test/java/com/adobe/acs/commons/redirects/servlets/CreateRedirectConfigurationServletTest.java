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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import com.adobe.acs.commons.redirects.models.Configurations;
import com.adobe.acs.commons.redirects.models.RedirectConfiguration;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateRedirectConfigurationServletTest {

    CreateRedirectConfigurationServlet servlet;

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Before
    public void setUp() {
        context.addModelsForClasses(Configurations.class);
        servlet = new CreateRedirectConfigurationServlet();
        RedirectFilterMBean redirectFilter = mock(RedirectFilterMBean.class);
        when(redirectFilter.getBucket()).thenReturn("settings");
        when(redirectFilter.getConfigName()).thenReturn("redirects");
        context.registerService(RedirectFilterMBean.class, redirectFilter);
        servlet.redirectFilter = redirectFilter;
    }

    @Test
    public void createConfig() throws ServletException, IOException {
        context.build().resource("/conf/global");
        context.request().addRequestParameter("path", "/conf/global");
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        // Read configurations via Model
        Configurations confModel = context.request().adaptTo(Configurations.class);
        Collection<RedirectConfiguration> configurations = confModel.getConfigurations();
        RedirectConfiguration cfg = configurations.iterator().next();
        assertEquals("/conf/global/settings/redirects", cfg.getPath());

        // return 409 if already exists
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_CONFLICT, context.response().getStatus());
    }

    @Test
    public void createDeepHierarchies() throws ServletException, IOException {
        context.build().resource("/conf/level0/level1/level2");

        context.request().setParameterMap(Collections.singletonMap("path", "/conf/level0"));
        servlet.doPost(context.request(), context.response());

        context.request().setParameterMap(Collections.singletonMap("path", "/conf/level0/level1"));
        servlet.doPost(context.request(), context.response());

        context.request().setParameterMap(Collections.singletonMap("path", "/conf/level0/level1/level2"));
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        // Read configurations via Model
        Configurations confModel = context.request().adaptTo(Configurations.class);
        Iterator<RedirectConfiguration> configurations = confModel.getConfigurations().iterator();
        assertEquals("/conf/level0/settings/redirects", configurations.next().getPath());
        assertEquals("/conf/level0/level1/settings/redirects", configurations.next().getPath());
        assertEquals("/conf/level0/level1/level2/settings/redirects", configurations.next().getPath());
    }

    @Test
    public void createConfigWithContextPrefix() throws ServletException, IOException {
        context.build().resource("/conf/global");
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("path", "/conf/global");
        request.addRequestParameter("contextPrefix", "/content/mysite");
        servlet.doPost(request, context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        // Read configurations via Model
        Configurations confModel = request.adaptTo(Configurations.class);
        Collection<RedirectConfiguration> configurations = confModel.getConfigurations();
        RedirectConfiguration cfg = configurations.iterator().next();
        assertEquals("/conf/global/settings/redirects", cfg.getPath());
        assertEquals("/content/mysite", request.getResourceResolver().getResource(cfg.getPath()).getValueMap().get("contextPrefix"));

        // return 409 if already exists
        servlet.doPost(request, context.response());
        assertEquals(HttpServletResponse.SC_CONFLICT, context.response().getStatus());
    }
}
