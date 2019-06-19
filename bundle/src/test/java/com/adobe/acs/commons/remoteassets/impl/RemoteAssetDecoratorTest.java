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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.remoteassets.RemoteAssetsBinarySync;
import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;

import junitx.util.PrivateAccessor;

@RunWith(MockitoJUnitRunner.class)
public final class RemoteAssetDecoratorTest {

    private static String TEST_MOCK_SYNC = "mocksync";
    private static String TEST_REMOTE_ASSET_CONTENT_PATH = "/content/dam/b/test_asset.png/jcr:content";
    private static final String TESTUSER = "testuser";

    private final RemoteAssetDecorator decorator = spy(new RemoteAssetDecorator());
    private final RemoteAssetsBinarySync assetSync = mock(RemoteAssetsBinarySync.class);
    private final RemoteAssetsConfig config = mock(RemoteAssetsConfig.class);

    private final Resource resource = mock(Resource.class);
    private final Map<String, Object> properties = new HashMap<>();
    private final ValueMap valueMap = new ValueMapDecorator(properties);
    private final ResourceResolver resourceResolver = mock(ResourceResolver.class);
    private final Set<String> whitelistedServiceUsers = new HashSet<>();
    private final List<String> damSyncPaths = new LinkedList<>();
    private final Session session = mock(Session.class);

    @Before
    public void setup() throws NoSuchFieldException {
        PrivateAccessor.setField(decorator, "assetSync", assetSync);
        PrivateAccessor.setField(decorator, "config", config);

        when(resource.getValueMap()).thenReturn(valueMap);
        when(resource.getPath()).thenReturn(TEST_REMOTE_ASSET_CONTENT_PATH);
        when(resource.getResourceResolver()).thenReturn(resourceResolver);

        when(resourceResolver.getUserID()).thenReturn(TESTUSER);

        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        when(config.getWhitelistedServiceUsers()).thenReturn(whitelistedServiceUsers);
        when(config.getDamSyncPaths()).thenReturn(damSyncPaths);
        when(config.getRetryDelay()).thenReturn(0);

        whitelistedServiceUsers.add(TESTUSER);
        damSyncPaths.add("/just/some/other/path");
        damSyncPaths.add(TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @SuppressWarnings("deprecation")
    private Resource decorate() {
        return decorator.decorate(resource, null);
    }

    private void verifyDoesNotAccept() {
        assertEquals(resource, decorate());
        verify(decorator, times(0)).isAlreadySyncing(anyString());
    }

    @Test
    public void doesNotAccept_wrongPrimaryType() {
        verifyDoesNotAccept();
    }

    @Test
    public void doesNotAccept_nonRemoteAsset() {
        properties.put(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSETCONTENT);
        verifyDoesNotAccept();
    }

    private void allowRetry() {
        doesNotAccept_nonRemoteAsset();
        properties.put(RemoteAssets.IS_REMOTE_ASSET, true);
        final Calendar lastFailure = Calendar.getInstance();
        final long currentTimeMillis = System.currentTimeMillis();
        lastFailure.setTimeInMillis(currentTimeMillis);
        properties.put(RemoteAssets.REMOTE_SYNC_FAILED, lastFailure);
    }

    @Test
    public void doesNotAccept_doNotRetryYet() {
        allowRetry();
        when(config.getRetryDelay()).thenReturn(100);
        verifyDoesNotAccept();
    }

    @Test
    public void doesNotAccept_loginFailureNull() {
        doesNotAccept_doNotRetryYet();
        when(resourceResolver.getUserID()).thenReturn(UserConstants.DEFAULT_ADMIN_ID);
        properties.put(RemoteAssets.REMOTE_SYNC_FAILED, null);
        verifyDoesNotAccept();
    }

    @Test
    public void doesNotAccept_retryAllowedAlready() {
        doesNotAccept_doNotRetryYet();
        when(resourceResolver.getUserID()).thenReturn(UserConstants.DEFAULT_ADMIN_ID);
        verifyDoesNotAccept();
    }

    @Test
    public void doesNotAccept_notInDamSyncPaths() {
        allowRetry();
        damSyncPaths.clear();
        verifyDoesNotAccept();
    }

    @Test
    public void doesNotAccept_catchRepositoryException() throws RepositoryException {
        allowRetry();
        whitelistedServiceUsers.clear();
        doThrow(RepositoryException.class).when(decorator).getUserManager(session);
        verifyDoesNotAccept();
    }
/*
        setupRemoteAssetsServiceUser(context);

        ResourceResolver resourceResolver = context.resourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);

        Node nodeContent = session.getRootNode().addNode("content", DamConstants.NT_SLING_ORDEREDFOLDER);
        Node nodeDam = nodeContent.addNode("dam", DamConstants.NT_SLING_ORDEREDFOLDER);
        setupCreateRemoteAsset(nodeDam, "a", false);
        setupCreateRemoteAsset(nodeDam, "b", true);
        setupCreateRemoteAsset(nodeDam, "z", true);
    }
    private void setupCreateRemoteAsset(Node nodeDam, String damFolder, boolean isRemoteAsset) throws RepositoryException {
        ValueFactory valueFactory = nodeDam.getSession().getValueFactory();

        Node nodeDamFolder = nodeDam.addNode(damFolder, DamConstants.NT_SLING_ORDEREDFOLDER);
        Node nodeAsset = nodeDamFolder.addNode("test_asset.png", DamConstants.NT_DAM_ASSET);
        Node nodeAssetContent = nodeAsset.addNode(JcrConstants.JCR_CONTENT, DamConstants.NT_DAM_ASSETCONTENT);
        nodeAssetContent.setProperty(IS_REMOTE_ASSET, isRemoteAsset);
        Node nodeAssetRenditions = nodeAssetContent.addNode(DamConstants.RENDITIONS_FOLDER, JcrConstants.NT_FOLDER);
        Node nodeRenditionOrig = nodeAssetRenditions.addNode(DamConstants.ORIGINAL_FILE, JcrConstants.NT_FILE);
        Node nodeRenditionOrigContent = nodeRenditionOrig.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        nodeRenditionOrigContent.setProperty(JcrConstants.JCR_DATA, valueFactory.createBinary(ClassLoader.getSystemResourceAsStream("remoteassets/remote_asset.png")));
    }

    private void setupFinish() {
        // Initialize Remote Assets
        when(remoteAssetsBinarySync.syncAsset(any(Resource.class))).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                Resource resource = (Resource)invocationOnMock.getArguments()[0];
                ModifiableValueMap resourceProps = resource.adaptTo(ModifiableValueMap.class);
                resourceProps.remove(IS_REMOTE_ASSET);
                resourceProps.put(TEST_MOCK_SYNC, true);
                return true;
            }
        });

        context.registerInjectActivateService(new RemoteAssetsConfigImpl(), getRemoteAssetsConfigs());
        context.registerInjectActivateService(remoteAssetsBinarySync);
        context.registerInjectActivateService(remoteAssetDecorator);

        LogTester.reset();
    }

    private ResourceResolver getUserResourceResolver() {
        return getUserResourceResolver("testuser", false);
    }

    private ResourceResolver getUserResourceResolver(String username, boolean isServiceUser) {
        try {
            User mockUser = mock(User.class);
            when(mockUser.isSystemUser()).thenReturn(isServiceUser);
            UserManager mockUserManager = mock(UserManager.class);
            when(mockUserManager.getAuthorizable(username)).thenReturn(mockUser);

            Map<String, Object> creds = new HashMap<>();
            creds.put("user.name", username);
            ResourceResolver resourceResolver = context.getService(ResourceResolverFactory.class).getResourceResolver(creds);

            when(remoteAssetDecorator.getUserManager(resourceResolver.adaptTo(Session.class))).thenReturn(mockUserManager);

            return resourceResolver;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertResourceSyncs(ResourceResolver resourceResolver, String path) {
        // Fetch the resource, triggering the sync
        Resource resource = resourceResolver.getResource(path);

        assertTrue(resource.getValueMap().get(TEST_MOCK_SYNC, false));
        LogTester.assertLogText("Sync'ing remote asset binaries: " + path);
    }

    private void assertResourceDoesNotSync(ResourceResolver resourceResolver, String path) {
        // Fetch the resource, triggering the sync
        Resource resource = resourceResolver.getResource(path);

        assertFalse(resource.getValueMap().get(TEST_MOCK_SYNC, false));
        LogTester.assertNotLogText("Sync'ing remote asset binaries: " + path);
    }

    @Test
    public void testGetResourceSyncsRemoteAsset() {
        setupFinish();
        assertResourceSyncs(getUserResourceResolver(), TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @Test
    public void testGetResourceSyncsAssetIfUserIsWhitelistedServiceUser() {
        setupFinish();
        ResourceResolver serviceResourceResolver = getUserResourceResolver(TEST_WHITELISTED_SVC_USER_A, true);
        assertResourceSyncs(serviceResourceResolver, TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @Test
    public void testGetResourceSyncsRemoteAssetThatFailedLongEnoughAgo() {
        Resource assetContent = context.resourceResolver().getResource(TEST_REMOTE_ASSET_CONTENT_PATH);
        ModifiableValueMap valueMap = assetContent.adaptTo(ModifiableValueMap.class);
        Calendar lastFailed = Calendar.getInstance();
        lastFailed.add(Calendar.MINUTE, -TEST_RETRY_DELAY);
        lastFailed.add(Calendar.SECOND, -1);
        valueMap.put(RemoteAssets.REMOTE_SYNC_FAILED, lastFailed);

        setupFinish();
        assertResourceSyncs(getUserResourceResolver(), TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @Test
    public void testGetResourceDoesNotSyncAssetOutsideOfMappedDamPaths() {
        setupFinish();
        assertResourceDoesNotSync(getUserResourceResolver(), "/content/dam/z/test_asset.png/jcr:content");
    }

    @Test
    public void testGetResourceDoesNotSyncAssetIfFailedTooRecently() {
        Resource assetContent = context.resourceResolver().getResource(TEST_REMOTE_ASSET_CONTENT_PATH);
        ModifiableValueMap valueMap = assetContent.adaptTo(ModifiableValueMap.class);
        Calendar lastFailed = Calendar.getInstance();
        lastFailed.add(Calendar.MINUTE, -TEST_RETRY_DELAY);
        lastFailed.add(Calendar.SECOND, 2);
        valueMap.put(RemoteAssets.REMOTE_SYNC_FAILED, lastFailed);

        setupFinish();
        assertResourceDoesNotSync(getUserResourceResolver(), TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @Test
    public void testGetResourceDoesNotSyncAssetIfUserIsAdminUser() {
        setupFinish();
        ResourceResolver adminResourceResolver = context.resourceResolver();
        assertEquals("admin", adminResourceResolver.getUserID());
        assertResourceDoesNotSync(adminResourceResolver, TEST_REMOTE_ASSET_CONTENT_PATH);
        LogTester.assertLogText("Avoiding binary sync for admin user");
    }

    @Test
    public void testGetResourceDoesNotSyncAssetIfUserIsServiceUser() {
        setupFinish();
        ResourceResolver serviceResourceResolver = getUserResourceResolver("serviceuser", true);
        assertResourceDoesNotSync(serviceResourceResolver, TEST_REMOTE_ASSET_CONTENT_PATH);
        LogTester.assertLogText("Avoiding binary sync b/c this is a non-whitelisted service user: serviceuser");
    }

    @Test
    public void testGetResourceDoesNotSyncAssetNotFlaggedAsRemote() {
        Resource assetContent = context.resourceResolver().getResource(TEST_REMOTE_ASSET_CONTENT_PATH);
        ModifiableValueMap valueMap = assetContent.adaptTo(ModifiableValueMap.class);
        valueMap.remove(IS_REMOTE_ASSET);

        setupFinish();
        assertResourceDoesNotSync(getUserResourceResolver(), TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @Test
    public void testGetResourceDoesNotSyncIfNotAssetContent() {
        setupFinish();

        ResourceResolver resourceResolver = getUserResourceResolver();
        Resource asset = resourceResolver.getResource("/content/dam/b/test_asset.png");
        Resource folder = resourceResolver.getResource("/content/dam/b");

        assertEquals(DamConstants.NT_DAM_ASSET, asset.getResourceType());
        assertEquals(DamConstants.NT_SLING_ORDEREDFOLDER, folder.getResourceType());
    }

    @Test
    public void testGetResourceHandlesExceptionCheckingIfResourceIsRemoteAsset() throws RepositoryException {
        setupFinish();

        doThrow(new RuntimeException("test exception")).when(remoteAssetDecorator).accepts(any(Resource.class));

        assertResourceDoesNotSync(getUserResourceResolver(), TEST_REMOTE_ASSET_CONTENT_PATH);
        LogTester.assertLogText("Failed binary sync check for remote asset: " + TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @Test
    public void testGetResourceHandlesSyncException() {
        setupFinish();
        doThrow(new RuntimeException("test sync exception")).when(remoteAssetsBinarySync).syncAsset(any(Resource.class));

        // Fetch the resource, triggering the sync
        Resource resource = getUserResourceResolver().getResource(TEST_REMOTE_ASSET_CONTENT_PATH);

        // Validate that the sync is attempted
        LogTester.assertLogText("Sync'ing remote asset binaries: " + TEST_REMOTE_ASSET_CONTENT_PATH);
        // But the sync does not succeed
        assertFalse(resource.getValueMap().get(TEST_MOCK_SYNC, false));
        LogTester.assertLogText("Failed to sync binaries for remote asset: " + TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @Test
    public void testGetResourceHandlesSyncFailure() {
        setupFinish();
        doReturn(false).when(remoteAssetsBinarySync).syncAsset(any(Resource.class));

        // Fetch the resource, triggering the sync
        Resource resource = getUserResourceResolver().getResource(TEST_REMOTE_ASSET_CONTENT_PATH);

        // Validate that the sync is attempted
        LogTester.assertLogText("Sync'ing remote asset binaries: " + TEST_REMOTE_ASSET_CONTENT_PATH);
        // But the sync does not succeed
        assertFalse(resource.getValueMap().get(TEST_MOCK_SYNC, false));
        LogTester.assertLogText("Failed to sync binaries for remote asset: " + TEST_REMOTE_ASSET_CONTENT_PATH);
    }

    @Test
    public void testGetResourceHandlesExceptionWaitingForSyncInProgress() {
        when(remoteAssetDecorator.isAlreadySyncing(TEST_REMOTE_ASSET_CONTENT_PATH)).thenReturn(true).thenReturn(true).thenThrow(new RuntimeException("test failed waiting"));

        setupFinish();
        assertResourceDoesNotSync(getUserResourceResolver(), TEST_REMOTE_ASSET_CONTENT_PATH);

        LogTester.assertLogText("Already sync'ing " + TEST_REMOTE_ASSET_CONTENT_PATH + " - waiting for parallel sync to complete");
        LogTester.assertLogText("Failed to wait for parallel binary sync for remote asset: " + TEST_REMOTE_ASSET_CONTENT_PATH);
        LogTester.assertNotLogText("Parallel sync of " + TEST_REMOTE_ASSET_CONTENT_PATH + " complete");
    }

    @Test
    public void testGetResourceWaitsForSyncInProgress() {
        Resource assetContent = context.resourceResolver().getResource(TEST_REMOTE_ASSET_CONTENT_PATH);
        when(remoteAssetDecorator.isAlreadySyncing(TEST_REMOTE_ASSET_CONTENT_PATH)).thenReturn(true).thenReturn(true).thenReturn(true)
                .then(new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                        ModifiableValueMap resourceProps = assetContent.adaptTo(ModifiableValueMap.class);
                        resourceProps.remove(IS_REMOTE_ASSET);
                        resourceProps.put(TEST_MOCK_SYNC, true);
                        return false;
                    }
                }).thenReturn(false);

        setupFinish();

        // Fetch the resource, triggering the sync
        Resource resource = getUserResourceResolver().getResource(TEST_REMOTE_ASSET_CONTENT_PATH);

        // Validate that the sync is not attempted (it was already in progress)
        LogTester.assertNotLogText("Sync'ing remote asset binaries: " + TEST_REMOTE_ASSET_CONTENT_PATH);
        // But the sync does succeed because it waited for the sync in progress
        assertTrue(resource.getValueMap().get(TEST_MOCK_SYNC, false));

        LogTester.assertLogText("Already sync'ing " + TEST_REMOTE_ASSET_CONTENT_PATH + " - waiting for parallel sync to complete");
        LogTester.assertLogText("Parallel sync of " + TEST_REMOTE_ASSET_CONTENT_PATH + " complete");
    }*/
}
