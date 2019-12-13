/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.oak.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.oak.EnsureOakIndexManager;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class EnsureOakIndexServletTest {

    private static final String INDEX_PATH ="/apps/indexes/myindex";
    
    @Rule
    public SlingContext context = new SlingContext();
    
    @Mock
    EnsureOakIndexManager ensureOakIndexManager;
    
    EnsureOakIndexServlet servlet;
    
    @Before
    public void setup() {
        servlet = new EnsureOakIndexServlet();
        context.registerService(EnsureOakIndexManager.class, ensureOakIndexManager);
//        context.registerService(ensureOakIndexManager);
        context.registerInjectActivateService(servlet);
    }
    
    @Test
    public void testGet() {
        servlet.doGet(context.request(),context.response());
        assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED,context.response().getStatus());
    }
    
    @Test
    public void testPostWithForce() throws IOException {
        context.request().setParameterMap(ImmutableMap.of("force","true"));
        servlet.doPost(context.request(), context.response());
        verify(ensureOakIndexManager).ensureAll(true);
        assertEquals(HttpServletResponse.SC_OK,context.response().getStatus());
    }
    
    @Test
    public void testPostWithPathAndNonForce() throws IOException {
        context.request().setParameterMap(ImmutableMap.of("force","blub","path",INDEX_PATH));
        servlet.doPost(context.request(), context.response());
        verify(ensureOakIndexManager).ensure(false, INDEX_PATH);
        assertEquals(HttpServletResponse.SC_OK,context.response().getStatus());
    }
    
}
