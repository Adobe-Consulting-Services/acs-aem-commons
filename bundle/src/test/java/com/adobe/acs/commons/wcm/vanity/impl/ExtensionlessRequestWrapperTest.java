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
package com.adobe.acs.commons.wcm.vanity.impl;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionlessRequestWrapperTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    public MockSlingHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext());

        context.build().resource("/content")
                .resource("null-extension","sling:status", "302");

        context.build().resource("/content")
                .resource("has-extension");
    }

    @Test
    public void getRequestPathInfo_NullExtension() throws Exception {
        request.setResource(context.resourceResolver().getResource("/content/null-extension"));

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo)request.getRequestPathInfo();
        requestPathInfo.setExtension("xyz");

        ExtensionlessRequestWrapper wrapper = new ExtensionlessRequestWrapper(request);
        assertNull(wrapper.getRequestPathInfo().getExtension());
    }


    @Test
    public void getRequestPathInfo_HasExtension() throws Exception {
        request.setResource(context.resourceResolver().getResource("/content/has-extension"));

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo)request.getRequestPathInfo();
        requestPathInfo.setExtension("xyz");

        ExtensionlessRequestWrapper wrapper = new ExtensionlessRequestWrapper(request);
        assertEquals("xyz", wrapper.getRequestPathInfo().getExtension());
    }

    @Test
    public void passThroughMethods() throws Exception {
        context.requestPathInfo().setResourcePath("/content");
        context.requestPathInfo().setSelectorString("bar");
        context.requestPathInfo().setExtension("html");
        context.requestPathInfo().setSuffix("/xyz");

        context.request().setResource(context.resourceResolver().getResource("/content"));
        ExtensionlessRequestWrapper wrapper = new ExtensionlessRequestWrapper(context.request());

        assertEquals("/content", wrapper.getRequestPathInfo().getResourcePath());
        assertEquals("bar", wrapper.getRequestPathInfo().getSelectorString());
        assertEquals("html", wrapper.getRequestPathInfo().getExtension());
        assertEquals("/xyz", wrapper.getRequestPathInfo().getSuffix());
    }
}