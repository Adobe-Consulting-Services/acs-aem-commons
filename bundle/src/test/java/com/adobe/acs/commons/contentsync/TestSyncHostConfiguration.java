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
package com.adobe.acs.commons.contentsync;

import com.adobe.granite.crypto.CryptoSupport;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.adobe.acs.commons.contentsync.ConfigurationUtils.HOSTS_PATH;
import static junitx.framework.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestSyncHostConfiguration {
    @Rule
    public AemContext context = new AemContext();

    private CryptoSupport crypto;

    private String configPath = HOSTS_PATH + "/host1";

    @Before
    public void setUp() throws Exception {
        crypto = MockCryptoSupport.getInstance();
        context.registerService(CryptoSupport.class, crypto);
    }

    private SyncHostConfiguration getConfiguration(Object... properties) {
        Resource resource = context.create().resource(configPath, properties);
        return context.getService(ModelFactory.class).createModel(resource, SyncHostConfiguration.class);
    }

    @Test
    public void testUnecryptedPassword() {
        SyncHostConfiguration configuration = getConfiguration(
                "host", "http://localhost:4502", "username", "admin", "password", "admin");
        assertEquals("admin", configuration.getPassword());
    }

    @Test
    public void testEcryptedPassword() throws Exception {
        SyncHostConfiguration configuration = getConfiguration(
                "host", "http://localhost:4502", "username", "admin", "password", "admin-encrypted");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        assertEquals("admin", configuration.getPassword());
        verify(crypto, times(1)).isProtected(captor.capture());
        verify(crypto, times(1)).unprotect(captor.capture());
    }

    @Test
    public void testIsAuthEnabled() {
        configPath = HOSTS_PATH + "/host2";
        SyncHostConfiguration oauthConfiguration = getConfiguration("host", "http://localhost:4502", "authType", "oauth");
        assertEquals(true, oauthConfiguration.isOAuthEnabled());

        configPath = HOSTS_PATH + "/host3";
        SyncHostConfiguration basicConfiguration = getConfiguration("host", "http://localhost:4502", "authType", "basic");
        assertEquals(false, basicConfiguration.isOAuthEnabled());
    }
}
