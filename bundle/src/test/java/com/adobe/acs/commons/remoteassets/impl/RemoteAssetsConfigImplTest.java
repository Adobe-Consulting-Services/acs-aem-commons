/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.testutil.LogTester;
import com.adobe.acs.commons.util.RequireAem;
import com.adobe.acs.commons.util.RequireAem.Distribution;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.serviceusermapping.impl.MappingConfigAmendment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RemoteAssetsConfigImplTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Before
    public final void setup() throws RepositoryException {
        setupRemoteAssetsServiceUser(context);
        
        // does not work with a Mock here
        RequireAem requireAem = new RequireAem() {
          
          @Override
          public Distribution getDistribution() {
            return null;
          }
        };
        context.registerService(RequireAem.class, requireAem, "distribution","classic");
    }

    @Test
    public void testBadConfigServerInvalidUrl() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.put("server.url", "somethingbogus");

        try {
            context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);
            fail("Expected exception");
        } catch (Exception e) {
            assertNotNull(e.getCause());
            assertEquals("Remote server address is malformed", e.getCause().getMessage());
        }
    }

    @Test
    public void testBadConfigServerNotSpecified() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.remove("server.url");

        try {
            context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);
            fail("Expected exception");
        } catch (Exception e) {
            assertNotNull(e.getCause());
            assertEquals("Remote server must be specified", e.getCause().getMessage());
        }
    }

    @Test
    public void testBadConfigServerProtocolNotSecure() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.put("server.url", TEST_SERVER_URL.replace("https://", "http://"));

        try {
            context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);
            fail("Expected exception");
        } catch (Exception e) {
            assertNotNull(e.getCause());
            assertEquals("Remote server address must be HTTPS", e.getCause().getMessage().substring(0, 35));
        }
    }

    @Test
    public void testBadConfigUsernameNotSpecified() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.remove("server.user");

        try {
            context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);
            fail("Expected exception");
        } catch (Exception e) {
            assertNotNull(e.getCause());
            assertEquals("Remote server username must be specified", e.getCause().getMessage());
        }
    }

    @Test
    public void testBadConfigPasswordNotSpecified() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.remove("server.pass");

        try {
            context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);
            fail("Expected exception");
        } catch (Exception e) {
            assertNotNull(e.getCause());
            assertEquals("Remote server password must be specified", e.getCause().getMessage());
        }
    }

    @Test
    public void testGetResourceResolver() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();

        RemoteAssetsConfigImpl config = context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);

        ResourceResolver resourceResolver = config.getResourceResolver();
        assertNotNull(resourceResolver);
        // the following does not work due to https://issues.apache.org/jira/browse/SLING-10995
        //assertEquals("acs-commons-remote-assets-service", resourceResolver.getUserID());
    }

    @Test
    public void testValidConfigs() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();

        RemoteAssetsConfigImpl config = context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);

        assertEquals(TEST_SERVER_URL, config.getServer());
        assertEquals(TEST_SERVER_USERNAME, config.getUsername());
        assertEquals(TEST_SERVER_PASSWORD, config.getPassword());
        assertEquals(Arrays.asList(TEST_TAGS_PATH_A, TEST_TAGS_PATH_B), config.getTagSyncPaths());
        assertEquals(Arrays.asList(TEST_DAM_PATH_A, TEST_DAM_PATH_B), config.getDamSyncPaths());
        assertEquals(Integer.valueOf(TEST_RETRY_DELAY), config.getRetryDelay());
        assertEquals(Integer.valueOf(TEST_SAVE_INTERVAL), config.getSaveInterval());
        assertEquals(new HashSet<String>(Arrays.asList(TEST_WHITELISTED_SVC_USER_A, TEST_WHITELISTED_SVC_USER_B)), config.getWhitelistedServiceUsers());

        assertNotNull(config.getRemoteAssetsHttpExecutor());
    }

    @Test
    public void testValidConfigsOverrideHttps() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.put("server.url", TEST_SERVER_URL.replace("https://", "http://"));
        remoteAssetsConfigs.put("server.insecure", true);

        RemoteAssetsConfigImpl config = context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);

        LogTester.assertLogText("Remote Assets connection is not HTTPS - authentication username and password will be"
                + " communicated in CLEAR TEXT.  This configuration is NOT recommended, as it may allow"
                + " credentials to be compromised!");
    }
}
