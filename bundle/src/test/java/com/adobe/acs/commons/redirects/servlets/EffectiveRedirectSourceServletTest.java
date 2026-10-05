/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.http.entity.ContentType;
import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.acs.commons.redirects.models.Configurations;

public class EffectiveRedirectSourceServletTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private EffectiveRedirectSourceServlet servlet;

    @Before
    public void setUp() {
        context.build().resource("/conf/global/settings/redirects",
                "sling:resourceType", Configurations.REDIRECTS_RESOURCE_TYPE);
        context.request().setResource(context.resourceResolver().getResource("/conf/global/settings/redirects"));
        servlet = new EffectiveRedirectSourceServlet(new ModelAdapterFactory());
    }

    @Test
    public void resolvesRelativePathsWithContextPrefix() throws ServletException, IOException {
        context.request().addRequestParameter("source", "/content/site/en");
        context.request().addRequestParameter("target", "/content/site/fr");
        context.request().addRequestParameter("contextPrefix", "/content/site");

        servlet.doGet(context.request(), context.response());

        assertEquals(ContentType.TEXT_PLAIN.getMimeType(), context.response().getContentType());
        assertEquals("effectiveSource=/content/site/en\neffectiveTarget=/content/site/fr\n",
                context.response().getOutputAsString());
    }

    @Test
    public void leavesAbsoluteAndPrefixedPathsUnchanged() throws ServletException, IOException {
        context.request().addRequestParameter("source", "https://example.com/en");
        context.request().addRequestParameter("target", "/content/site/fr");
        context.request().addRequestParameter("contextPrefix", "/content/site");

        servlet.doGet(context.request(), context.response());

        assertEquals(ContentType.TEXT_PLAIN.getMimeType(), context.response().getContentType());
        assertEquals("effectiveSource=https://example.com/en\neffectiveTarget=/content/site/fr\n",
                context.response().getOutputAsString());
    }
}
