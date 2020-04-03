/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.redirectmaps.impl;

import static org.junit.Assert.assertEquals;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirectmaps.models.RedirectMapModelTest;

@RunWith(MockitoJUnitRunner.class)
public class FakeSlingHttpSerlvetRequestTest {

    private FakeSlingHttpServletRequest test;

    @Mock
    private ResourceResolver resolver;

    private static final Logger log = LoggerFactory.getLogger(RedirectMapModelTest.class);

    @Before
    public void init() {
        log.info("init");
        test = new FakeSlingHttpServletRequest(resolver, "http", "www.adobe.com", 80);
    }

    @Test
    public void getScheme() {
        assertEquals("http", test.getScheme());
    }

    @Test
    public void testPort() {
        assertEquals(80, test.getServerPort());
    }

    @Test
    public void testServerName() {
        assertEquals("www.adobe.com", test.getServerName());
    }
}
