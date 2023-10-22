/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync.impl;

import com.adobe.acs.commons.contentsync.MockCryptoSupport;
import com.adobe.granite.crypto.CryptoSupport;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static com.adobe.acs.commons.contentsync.ConfigurationUtils.HOSTS_PATH;
import static junitx.framework.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestEncryptPasswordPostProcessor {
    @Rule
    public AemContext context = new AemContext();

    private CryptoSupport crypto;

    private EncryptPasswordPostProcessor postProcessor;

    @Before
    public void setUp() throws Exception {

        crypto = MockCryptoSupport.getInstance();

        context.registerService(CryptoSupport.class, crypto);

        postProcessor = context.registerInjectActivateService(new EncryptPasswordPostProcessor());
    }

    @Test
    public void testProtectPassword() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Resource resource = context.create().resource(HOSTS_PATH + "/host1",
                "host", "http://localhost:4502", "username", "admin", "password", "admin");
        context.request().setResource(resource);
        List<Modification> changes = new ArrayList<>();
        changes.add(new Modification(ModificationType.CREATE, resource.getPath(), resource.getPath()));
        postProcessor.process(context.request(), changes);

        assertEquals("admin-encrypted", resource.getValueMap().get("password"));
        verify(crypto, times(1)).isProtected(captor.capture());
        verify(crypto, times(1)).protect(captor.capture());
     }

    @Test
    public void testSkipProtectedPassword() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Resource resource = context.create().resource(HOSTS_PATH + "/host1",
                "host", "http://localhost:4502", "username", "admin", "password", "admin-encrypted");
        context.request().setResource(resource);
        List<Modification> changes = new ArrayList<>();
        changes.add(new Modification(ModificationType.MODIFY, resource.getPath(), resource.getPath()));

        postProcessor.process(context.request(), changes);

        assertEquals("admin-encrypted", resource.getValueMap().get("password"));
        verify(crypto, times(1)).isProtected(captor.capture());
        verify(crypto, times(0)).protect(captor.capture());
    }

    @Test
    public void testIgnoreNonContentSyncPaths() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Resource resource = context.create().resource( "/var/unknown/host1",
                "host", "http://localhost:4502", "username", "admin", "password", "admin");
        context.request().setResource(resource);
        List<Modification> changes = new ArrayList<>();
        changes.add(new Modification(ModificationType.CREATE, resource.getPath(), resource.getPath()));

        postProcessor.process(context.request(), changes);

        assertEquals("admin", resource.getValueMap().get("password"));
        verify(crypto, times(0)).isProtected(captor.capture());
        verify(crypto, times(0)).protect(captor.capture());
    }

    @Test
    public void testIgnoreNullPassword() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Resource resource = context.create().resource( HOSTS_PATH + "/host1",
                "host", "http://localhost:4502", "username", "admin");
        context.request().setResource(resource);
        List<Modification> changes = new ArrayList<>();
        changes.add(new Modification(ModificationType.CREATE, resource.getPath(), resource.getPath()));

        postProcessor.process(context.request(), changes);

        assertEquals(null, resource.getValueMap().get("password"));
        verify(crypto, times(0)).isProtected(captor.capture());
        verify(crypto, times(0)).protect(captor.capture());
    }
}
