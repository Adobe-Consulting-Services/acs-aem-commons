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
package com.adobe.acs.commons.synth.impl;

import com.amazonaws.HttpMethod;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class SynthesizedSlingHttpServletRequestTest {
    AemContext context;

    ResourceResolver resourceResolver;

    SynthesizedSlingHttpServletRequest synthesizedSlingHttpServletRequest;

    @Before
    public void setUp() throws Exception {
        context = new AemContext(ResourceResolverType.JCR_MOCK);
        resourceResolver = context.resourceResolver();
        synthesizedSlingHttpServletRequest = new SynthesizedSlingHttpServletRequest(context.request());
    }

    @Test
    public void test_getAndSetMethod() throws Exception {
        String newMethod = HttpMethod.POST.toString();
        Assert.assertEquals(synthesizedSlingHttpServletRequest.getMethod(), HttpMethod.GET.toString());
        synthesizedSlingHttpServletRequest.setMethod(newMethod);
        Assert.assertEquals(synthesizedSlingHttpServletRequest.getMethod(), newMethod);
    }

    @Test
    public void test_getAndSetResource() throws Exception {
        String testResourcePath = "/test/resource";
        Assert.assertNull(synthesizedSlingHttpServletRequest.getResource());
        context.load().json("/com/adobe/acs/commons/cloudconfig/cloudconfig.json", testResourcePath);
        synthesizedSlingHttpServletRequest.setResource(context.resourceResolver().getResource(testResourcePath));
        Assert.assertEquals(synthesizedSlingHttpServletRequest.getResource().getPath(), testResourcePath);
        String newTestResourcePath = "/new/test/resource";
        synthesizedSlingHttpServletRequest.setResourcePath(newTestResourcePath);
        Assert.assertEquals(synthesizedSlingHttpServletRequest.getRequestPathInfo().getResourcePath(), newTestResourcePath);
    }

    @Test
    public void test_getRequestPathInfo() throws Exception {
        Assert.assertNotNull(synthesizedSlingHttpServletRequest.getRequestPathInfo());
    }

    @Test
    public void test_setAndGetExtension() throws Exception {
        String newExtension = "xml";
        String originalExtension = synthesizedSlingHttpServletRequest.getRequestPathInfo().getExtension();
        synthesizedSlingHttpServletRequest.setExtension(newExtension);
        Assert.assertNotEquals(synthesizedSlingHttpServletRequest.getRequestPathInfo().getExtension(), originalExtension);
        Assert.assertEquals(synthesizedSlingHttpServletRequest.getRequestPathInfo().getExtension(), newExtension);
    }

    @Test
    public void test_setAndGetSuffix() throws Exception {
        String originalSuffix = synthesizedSlingHttpServletRequest.getRequestPathInfo().getSuffix();
        String newSuffix = "test";
        synthesizedSlingHttpServletRequest.setSuffix(newSuffix);
        Assert.assertNotEquals(synthesizedSlingHttpServletRequest.getRequestPathInfo().getSuffix(), originalSuffix);
        Assert.assertEquals(synthesizedSlingHttpServletRequest.getRequestPathInfo().getSuffix(), newSuffix);
    }

    @Test
    public void test_setAndClearSelectors() throws Exception {
        String[] newSelectors = new String[]{"test","test2"};
        synthesizedSlingHttpServletRequest.setSelectors(newSelectors);
        Assert.assertTrue(Arrays.equals(synthesizedSlingHttpServletRequest.getRequestPathInfo().getSelectors(), newSelectors));
        synthesizedSlingHttpServletRequest.clearSelectors();
        Assert.assertEquals(synthesizedSlingHttpServletRequest.getRequestPathInfo().getSelectors().length, 0);
    }

}
