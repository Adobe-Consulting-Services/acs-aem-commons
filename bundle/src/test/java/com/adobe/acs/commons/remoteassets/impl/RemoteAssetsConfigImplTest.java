package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.adobe.acs.commons.testutil.LogTester;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.http.client.fluent.Executor;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.serviceusermapping.impl.MappingConfigAmendment;
import org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import javax.jcr.Session;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RemoteAssetsConfigImplTest {
    private static final String TEST_SERVER_URL = "https://remote-aem-server:4502/";
    private static final String TEST_SERVER_USERNAME = "admin";
    private static final String TEST_SERVER_PASSWORD = "passwd";
    private static final String TEST_TAGS_PATH_A = "/content/cq:tags/a";
    private static final String TEST_TAGS_PATH_B = "/content/cq:tags/b";
    private static final String TEST_DAM_PATH_A = "/content/dam/a";
    private static final String TEST_DAM_PATH_B = "/content/dam/b";
    private static final int TEST_RETRY_DELAY = 30;
    private static final int TEST_SAVE_INTERVAL = 500;
    private static final String TEST_WHITELISTED_SVC_USER_A = "user_a";
    private static final String TEST_WHITELISTED_SVC_USER_B = "user_b";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Before
    public final void setup() {
        Map<String, Object> serviceUserMapperConfig = new HashMap<>();
        serviceUserMapperConfig.put("user.mapping", context.bundleContext().getBundle().getSymbolicName() + ":remote-assets=acs-commons-remote-assets-service");
        context.registerInjectActivateService(new MappingConfigAmendment(), serviceUserMapperConfig);
    }

    private Map<String, Object> getValidRemoteAssetsConfigs() {
        Map<String, Object> remoteAssetsConfigs = new HashMap<>();
        remoteAssetsConfigs.put("server.url", TEST_SERVER_URL);
        remoteAssetsConfigs.put("server.user", TEST_SERVER_USERNAME);
        remoteAssetsConfigs.put("server.pass", TEST_SERVER_PASSWORD);
        remoteAssetsConfigs.put("server.insecure", false);
        remoteAssetsConfigs.put("tag.paths", new String[]{TEST_TAGS_PATH_A, "", TEST_TAGS_PATH_B});
        remoteAssetsConfigs.put("dam.paths", new String[]{TEST_DAM_PATH_A, "", TEST_DAM_PATH_B});
        remoteAssetsConfigs.put("retry.delay", TEST_RETRY_DELAY);
        remoteAssetsConfigs.put("save.interval", TEST_SAVE_INTERVAL);
        remoteAssetsConfigs.put("whitelisted.service.users", new String[]{TEST_WHITELISTED_SVC_USER_A, "", TEST_WHITELISTED_SVC_USER_B});

        return remoteAssetsConfigs;
    }

    @Test
    public void testBadConfigServerInvalidURL() {
        Map<String, Object> remoteAssetsConfigs = getValidRemoteAssetsConfigs();
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
        Map<String, Object> remoteAssetsConfigs = getValidRemoteAssetsConfigs();
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
        Map<String, Object> remoteAssetsConfigs = getValidRemoteAssetsConfigs();
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
        Map<String, Object> remoteAssetsConfigs = getValidRemoteAssetsConfigs();
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
        Map<String, Object> remoteAssetsConfigs = getValidRemoteAssetsConfigs();
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
        Map<String, Object> remoteAssetsConfigs = getValidRemoteAssetsConfigs();

        RemoteAssetsConfig config = context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);

        ResourceResolver resourceResolver = config.getResourceResolver();
        assertNotNull(resourceResolver);
        assertEquals("acs-commons-remote-assets-service", resourceResolver.getUserID());
    }

    @Test
    public void testValidConfigs() {
        Map<String, Object> remoteAssetsConfigs = getValidRemoteAssetsConfigs();

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
        Map<String, Object> remoteAssetsConfigs = getValidRemoteAssetsConfigs();
        remoteAssetsConfigs.put("server.url", TEST_SERVER_URL.replace("https://", "http://"));
        remoteAssetsConfigs.put("server.insecure", true);

        RemoteAssetsConfig config = context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);

        LogTester.assertLogText("Remote Assets connection is not HTTPS - authentication username and password will be"
                + " communicated in CLEAR TEXT.  This configuration is NOT recommended, as it may allow"
                + " credentials to be compromised!");
    }
}
