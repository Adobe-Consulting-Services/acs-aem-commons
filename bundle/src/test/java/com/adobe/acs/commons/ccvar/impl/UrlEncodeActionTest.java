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
package com.adobe.acs.commons.ccvar.impl;

import com.adobe.acs.commons.ccvar.TransformAction;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class UrlEncodeActionTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Test
    public void testSuccessfulUrlEncodeAction() {
        TransformAction urlEncodeAction = context.registerService(TransformAction.class, new UrlEncodeAction());
        assertNotNull(urlEncodeAction);
        assertEquals("url", urlEncodeAction.getName());
        assertEquals("test+space", urlEncodeAction.execute("test space"));
        assertEquals("%2Fcontent%2Fpath", urlEncodeAction.execute("/content/path"));
    }
}
