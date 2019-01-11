package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.adobe.acs.commons.testutil.LogTester;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.serviceusermapping.impl.MappingConfigAmendment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RemoteAssetsConfigImplTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Before
    public final void setup() {
        setupRemoteAssetsServiceUser(context);
    }

    @Test
    public void testBadConfigServerInvalidURL() {
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

        RemoteAssetsConfig config = context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);

        ResourceResolver resourceResolver = config.getResourceResolver();
        assertNotNull(resourceResolver);
        assertEquals("acs-commons-remote-assets-service", resourceResolver.getUserID());
    }

    @Test
    public void testValidConfigs() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();

        RemoteAssetsConfig config = context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);

        assertEquals(TEST_SERVER_URL, config.getServer());
        assertEquals(TEST_SERVER_USERNAME, config.getUsername());
        assertEquals(TEST_SERVER_PASSWORD, config.getPassword());
        assertEquals(Arrays.asList(TEST_TAGS_PATH_A, TEST_TAGS_PATH_B), config.getTagSyncPaths());
        assertEquals(Arrays.asList(TEST_DAM_PATH_A, TEST_DAM_PATH_B), config.getDamSyncPaths());
        assertEquals(new Integer(TEST_RETRY_DELAY), config.getRetryDelay());
        assertEquals(new Integer(TEST_SAVE_INTERVAL), config.getSaveInterval());
        assertEquals(new HashSet<String>(Arrays.asList(TEST_WHITELISTED_SVC_USER_A, TEST_WHITELISTED_SVC_USER_B)), config.getWhitelistedServiceUsers());

        assertNotNull(config.getRemoteAssetsHttpExecutor());
    }

    @Test
    public void testValidConfigsOverrideHttps() {
        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.put("server.url", TEST_SERVER_URL.replace("https://", "http://"));
        remoteAssetsConfigs.put("server.insecure", true);

        RemoteAssetsConfig config = context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);

        LogTester.assertLogText("Remote Assets connection is not HTTPS - authentication username and password will be"
                + " communicated in CLEAR TEXT.  This configuration is NOT recommended, as it may allow"
                + " credentials to be compromised!");
    }
}
