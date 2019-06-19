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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.apache.jackrabbit.api.security.user.UserManager;
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

    private static String TEST_REMOTE_ASSET_CONTENT_PATH = "/content/dam/b/test_asset.png/jcr:content";
    private static final String TESTUSER = "testuser";

    private final RemoteAssetDecorator decorator = spy(new RemoteAssetDecorator());
    private final RemoteAssetsBinarySync assetSync = mock(RemoteAssetsBinarySync.class);
    private final RemoteAssetsConfig config = mock(RemoteAssetsConfig.class);

    private final Resource resource = mock(Resource.class);
    private final Resource newResource = mock(Resource.class);
    private final Map<String, Object> properties = new HashMap<>();
    private final ValueMap valueMap = new ValueMapDecorator(properties);
    private final ResourceResolver resourceResolver = mock(ResourceResolver.class);
    private final Set<String> whitelistedServiceUsers = new HashSet<>();
    private final List<String> damSyncPaths = new LinkedList<>();
    private final Session session = mock(Session.class);
    private final UserManager userManager = mock(UserManager.class);
    private final User user = mock(User.class);

    private Set<String> remoteResourcesSyncing;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws NoSuchFieldException, RepositoryException {
        remoteResourcesSyncing = (Set<String>) PrivateAccessor.getField(RemoteAssetDecorator.class, "remoteResourcesSyncing");
        remoteResourcesSyncing.clear();

        PrivateAccessor.setField(decorator, "assetSync", assetSync);
        PrivateAccessor.setField(decorator, "config", config);
        doReturn(userManager).when(decorator).getUserManager(session);

        when(resource.getValueMap()).thenReturn(valueMap);
        when(resource.getPath()).thenReturn(TEST_REMOTE_ASSET_CONTENT_PATH);
        when(resource.getResourceResolver()).thenReturn(resourceResolver);

        when(resourceResolver.getUserID()).thenReturn(TESTUSER);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(resourceResolver.getResource(TEST_REMOTE_ASSET_CONTENT_PATH)).thenReturn(newResource);

        when(config.getWhitelistedServiceUsers()).thenReturn(whitelistedServiceUsers);
        when(config.getDamSyncPaths()).thenReturn(damSyncPaths);
        when(config.getRetryDelay()).thenReturn(0);

        whitelistedServiceUsers.add(TESTUSER);
        damSyncPaths.add("/just/some/other/path");
        damSyncPaths.add(TEST_REMOTE_ASSET_CONTENT_PATH);

        when(userManager.getAuthorizable(anyString())).thenReturn(user);
    }

    @SuppressWarnings("deprecation")
    private Resource decorate() {
        return decorator.decorate(resource, null);
    }

    private void assertSameResourceDecorated() {
        assertEquals(resource, decorate());
    }

    private void assertNewResourceDecorated() {
        assertEquals(newResource, decorate());
    }

    private void verifyIsAlreadySyncing(final int times) {
        verify(decorator, times(times)).isAlreadySyncing(anyString());
    }

    private void verifyDoesNotAccept() {
        assertSameResourceDecorated();
        verifyIsAlreadySyncing(0);
    }

    private void verifyAcceptedSameResource() {
        assertSameResourceDecorated();
        verifyIsAlreadySyncing(1);
    }

    private void verifyAcceptedNewResource() {
        assertNewResourceDecorated();
        verifyIsAlreadySyncing(1);
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

    @Test
    public void isAllowedUser_currentUserIsNull() throws RepositoryException {
        allowRetry();
        whitelistedServiceUsers.clear();
        when(userManager.getAuthorizable(anyString())).thenReturn(null);
        verifyDoesNotAccept();
    }

    @Test
    public void isAllowedUser_systemUser() {
        allowRetry();
        whitelistedServiceUsers.clear();
        when(user.isSystemUser()).thenReturn(true);
        verifyDoesNotAccept();
    }

    @Test
    public void isAllowedUser_notSystemUser() {
        allowRetry();
        whitelistedServiceUsers.clear();
        when(user.isSystemUser()).thenReturn(false);
        verifyAcceptedSameResource();
    }

    @Test
    public void syncAssetBinaries_syncAsset() {
        allowRetry();
        when(assetSync.syncAsset(resource)).thenReturn(true);
        verifyAcceptedNewResource();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void syncAssetBinaries_catchException() {
        allowRetry();
        when(assetSync.syncAsset(resource)).thenThrow(Exception.class);
        verifyAcceptedSameResource();
    }

    @Test
    public void waitForSyncInProgress_giveUpWaiting() throws NoSuchFieldException {
        allowRetry();
        PrivateAccessor.setField(RemoteAssetDecorator.class, "SYNC_WAIT_SECONDS", 1);
        remoteResourcesSyncing.add(TEST_REMOTE_ASSET_CONTENT_PATH);
        assertSameResourceDecorated();
    }

    @Test
    public void waitForSyncInProgress_waitTillReady() {
        allowRetry();
        remoteResourcesSyncing.add(TEST_REMOTE_ASSET_CONTENT_PATH);
        new java.util.Timer().schedule( 
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        remoteResourcesSyncing.clear();
                    }
                }, 500
        );
        assertNewResourceDecorated();
    }
}
