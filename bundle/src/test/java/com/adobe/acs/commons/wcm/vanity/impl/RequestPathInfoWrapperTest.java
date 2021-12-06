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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestPathInfoWrapperTest {
    AemContext context;

    ResourceResolver resourceResolver;

    RequestPathInfoWrapper requestPathInfoWrapper;

    private static final String TEST_PATH = "/test/path";
    private static final String TEST_EXTENSION = "json";
    private static final String TEST_SELECTOR = "test";
    private static final String TEST_SUFFIX = "bean";

    @Before
    public void setUp() throws Exception {
        context = new AemContext(ResourceResolverType.JCR_OAK);
        context.load().json(getClass().getResourceAsStream("RequestPathInfoWrapperContent.json"), TEST_PATH);
        resourceResolver = context.resourceResolver();
        requestPathInfoWrapper = RequestPathInfoWrapper.createRequestPathInfoWrapper(context.requestPathInfo(), resourceResolver.getResource(TEST_PATH));
    }

    @Test
    public void test_getResourcePath() throws Exception {
        context.requestPathInfo().setResourcePath(TEST_PATH);
        Assert.assertEquals(requestPathInfoWrapper.getResourcePath(), TEST_PATH);
    }


    @Test
    public void test_getExtension() throws Exception {
        context.requestPathInfo().setExtension(TEST_EXTENSION);
        Assert.assertEquals(requestPathInfoWrapper.getExtension(), TEST_EXTENSION);
    }

    @Test
    public void test_getSelectorString() throws Exception {
        context.requestPathInfo().setSelectorString(TEST_SELECTOR);
        Assert.assertEquals(requestPathInfoWrapper.getSelectorString(), TEST_SELECTOR);
    }

    @Test
    public void test_getSelectors() throws Exception {
        Assert.assertEquals(requestPathInfoWrapper.getSelectors().getClass(), String[].class);
    }

    @Test
    public void test_getSuffix() throws Exception {
        context.requestPathInfo().setSuffix(TEST_SUFFIX);
        Assert.assertEquals(requestPathInfoWrapper.getSuffix(), TEST_SUFFIX);
    }

    @Test
    public void test_getSuffixResource() throws Exception {
        Assert.assertNull(requestPathInfoWrapper.getSuffixResource());
    }

}
