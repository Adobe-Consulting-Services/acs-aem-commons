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

import com.adobe.acs.commons.remoteassets.RemoteAssetsBinarySync;
import com.adobe.acs.commons.testutil.LogTester;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.commons.handler.StandardImageHandler;
import com.day.cq.dam.commons.util.DamUtil;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.remoteassets.impl.RemoteAssets.IS_REMOTE_ASSET;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssets.REMOTE_SYNC_FAILED;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.getBytes;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.getRemoteAssetsConfigs;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.setupRemoteAssetsServiceUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class RemoteAssetsBinarySyncImplTest {
    private static String TEST_ASSET_CONTENT_PATH = "/content/dam/a/test_asset.png/jcr:content";
    private static String TEST_RENDITION_1280 = "cq5dam.web.1280.1280.png";
    private static String TEST_RENDITION_140 = "cq5dam.thumbnail.140.100.png";
    private static String TEST_RENDITION_48 = "cq5dam.thumbnail.48.48.png";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);
    private MockServerClient mockServerClient;

    private RemoteAssetsConfigImpl remoteAssetsConfig;
    private RemoteAssetsBinarySync remoteAssetsBinarySync;
    private Map<String, Integer> renditionResponseStatusMap;

    @Before
    public void setup() throws Exception {
        setupRemoteAssetsServiceUser(context);

        mockServerClient = new MockServerClient("localhost", mockServerRule.getPort());

        renditionResponseStatusMap = new HashMap<>();

        ResourceResolver resourceResolver = context.resourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);

        Node nodeContent = session.getRootNode().addNode("content", DamConstants.NT_SLING_ORDEREDFOLDER);
        Node nodeDam = nodeContent.addNode("dam", DamConstants.NT_SLING_ORDEREDFOLDER);
        Node nodeDamFolder = nodeDam.addNode("a", DamConstants.NT_SLING_ORDEREDFOLDER);
        Node nodeAsset = nodeDamFolder.addNode("test_asset.png", DamConstants.NT_DAM_ASSET);
        Node nodeAssetContent = nodeAsset.addNode(JcrConstants.JCR_CONTENT, DamConstants.NT_DAM_ASSETCONTENT);
        nodeAssetContent.setProperty(IS_REMOTE_ASSET, true);
        nodeAssetContent.setProperty(REMOTE_SYNC_FAILED, new GregorianCalendar(1900, 1, 1));
        Node nodeAssetRenditions = nodeAssetContent.addNode(DamConstants.RENDITIONS_FOLDER, JcrConstants.NT_FOLDER);
        setupAddRendition(nodeAssetRenditions, DamConstants.ORIGINAL_FILE);
        setupAddRendition(nodeAssetRenditions, TEST_RENDITION_1280);
        setupAddRendition(nodeAssetRenditions, TEST_RENDITION_140);
        setupAddRendition(nodeAssetRenditions, TEST_RENDITION_48);
        nodeAssetRenditions.addNode("nonrenditionnode", JcrConstants.NT_FOLDER);

        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.put("server.url", "http://localhost:" + mockServerRule.getPort());
        remoteAssetsConfigs.put("server.insecure", true);

        remoteAssetsConfig = spy(new RemoteAssetsConfigImpl());
        context.registerInjectActivateService(remoteAssetsConfig, remoteAssetsConfigs);
        remoteAssetsBinarySync = context.registerInjectActivateService(new RemoteAssetsBinarySyncImpl());

        ResourceResolver remoteAssetsResourceResolver = spy(remoteAssetsConfig.getResourceResolver());
        doNothing().when(remoteAssetsResourceResolver).revert();
        doReturn(remoteAssetsResourceResolver).when(remoteAssetsConfig).getResourceResolver();

        LogTester.reset();
    }

    private void setupAddRendition(Node nodeAssetRenditions, String renditionName) throws Exception {
        ValueFactory valueFactory = nodeAssetRenditions.getSession().getValueFactory();
        Node nodeRendition = nodeAssetRenditions.addNode(renditionName, JcrConstants.NT_FILE);
        Node nodeRenditionContent = nodeRendition.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        nodeRenditionContent.setProperty(JcrConstants.JCR_DATA, valueFactory.createBinary(ClassLoader.getSystemResourceAsStream("remoteassets/remote_asset.png")));
        nodeRenditionContent.setProperty(JcrConstants.JCR_MIMETYPE, StandardImageHandler.PNG1_MIMETYPE);

        renditionResponseStatusMap.put(renditionName, HttpServletResponse.SC_OK);
    }

    private void setupFinish() throws IOException {
        for (Map.Entry<String, Integer> renditionResponseStatusEntry : renditionResponseStatusMap.entrySet()) {
            String renditionName = renditionResponseStatusEntry.getKey();
            Integer renditionResponseStatus = renditionResponseStatusEntry.getValue();

            String renditionUrlPath = TEST_ASSET_CONTENT_PATH.replace(JcrConstants.JCR_CONTENT, "_jcr_content") + "/renditions/" + renditionName;
            String testImageName = renditionName.endsWith(".png") ? renditionName : (renditionName + ".png");
            byte[] testImageBytes = getBytes(ClassLoader.getSystemResourceAsStream("remoteassetstest/" + testImageName));

            HttpRequest request = request().withMethod("GET").withPath(renditionUrlPath);
            if (renditionResponseStatus.intValue() == HttpServletResponse.SC_OK) {
                mockServerClient.when(request).respond(
                        response().withStatusCode(renditionResponseStatus).withBody(testImageBytes)
                );
            } else {
                mockServerClient.when(request).respond(
                        response().withStatusCode(renditionResponseStatus).withBody("test error")
                );
            }
        }
    }

    private void assertRenditionSynced(Asset asset, String renditionName) throws IOException {
        Rendition rendition = asset.getRendition(renditionName);
        assertNotNull(rendition);

        String testImageName = renditionName.endsWith(".png") ? renditionName : (renditionName + ".png");
        byte[] testImageBytes = getBytes(ClassLoader.getSystemResourceAsStream("remoteassetstest/" + testImageName));
        assertEquals(testImageBytes.length, getBytes(rendition.getStream()).length);
    }

    private void assertRenditionNotSynced(Asset asset, String renditionName) throws IOException {
        Rendition rendition = asset.getRendition(renditionName);
        assertNotNull(rendition);

        byte[] testImageBytes = getBytes(ClassLoader.getSystemResourceAsStream("remoteassets/remote_asset.png"));
        assertEquals(testImageBytes.length, getBytes(rendition.getStream()).length);
    }

    @Test
    public void testSyncAssetSuccess() throws Exception {
        setupFinish();

        Resource resource = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertTrue(remoteAssetsBinarySync.syncAsset(resource));

        Resource resourceUpdated = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertFalse(resourceUpdated.getValueMap().get(IS_REMOTE_ASSET, false));
        assertNull(resourceUpdated.getValueMap().get(REMOTE_SYNC_FAILED, Calendar.class));

        Asset asset = DamUtil.resolveToAsset(resourceUpdated);
        assertRenditionSynced(asset, DamConstants.ORIGINAL_FILE);
        assertRenditionSynced(asset, TEST_RENDITION_1280);
        assertRenditionSynced(asset, TEST_RENDITION_140);
        assertRenditionSynced(asset, TEST_RENDITION_48);
    }

    @Test
    public void testSycnAssetSuccessWithRemovalOfNotFoundRendition() throws Exception {
        renditionResponseStatusMap.put(TEST_RENDITION_140, HttpServletResponse.SC_NOT_FOUND);
        setupFinish();

        Resource resource = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertTrue(remoteAssetsBinarySync.syncAsset(resource));

        Resource resourceUpdated = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertFalse(resourceUpdated.getValueMap().get(IS_REMOTE_ASSET, false));
        assertNull(resourceUpdated.getValueMap().get(REMOTE_SYNC_FAILED, Calendar.class));

        Asset asset = DamUtil.resolveToAsset(resourceUpdated);
        assertRenditionSynced(asset, DamConstants.ORIGINAL_FILE);
        assertRenditionSynced(asset, TEST_RENDITION_1280);
        assertNull(asset.getRendition(TEST_RENDITION_140));
        assertRenditionSynced(asset, TEST_RENDITION_48);
    }

    @Test
    public void testSycnAssetFailureDueToRenditionError() throws Exception {
        renditionResponseStatusMap.put(TEST_RENDITION_140, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        setupFinish();

        Resource resource = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertFalse(remoteAssetsBinarySync.syncAsset(resource));

        Resource resourceUpdated = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertTrue(resourceUpdated.getValueMap().get(IS_REMOTE_ASSET, false));

        Calendar remoteSyncFailTime = resourceUpdated.getValueMap().get(REMOTE_SYNC_FAILED, Calendar.class);
        assertTrue((System.currentTimeMillis() - remoteSyncFailTime.getTimeInMillis()) < 10000);

        Asset asset = DamUtil.resolveToAsset(resourceUpdated);
        assertRenditionNotSynced(asset, TEST_RENDITION_140);
        assertRenditionNotSynced(asset, TEST_RENDITION_48);
        verify(remoteAssetsConfig.getResourceResolver()).revert();
    }

    @Test
    public void testSycnAssetFailureDueToOriginalRenditionNotFound() throws Exception {
        renditionResponseStatusMap.put(DamConstants.ORIGINAL_FILE, HttpServletResponse.SC_NOT_FOUND);
        setupFinish();

        Resource resource = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertFalse(remoteAssetsBinarySync.syncAsset(resource));

        Resource resourceUpdated = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertTrue(resourceUpdated.getValueMap().get(IS_REMOTE_ASSET, false));

        Calendar remoteSyncFailTime = resourceUpdated.getValueMap().get(REMOTE_SYNC_FAILED, Calendar.class);
        assertTrue((System.currentTimeMillis() - remoteSyncFailTime.getTimeInMillis()) < 10000);

        Asset asset = DamUtil.resolveToAsset(resourceUpdated);
        assertRenditionNotSynced(asset, DamConstants.ORIGINAL_FILE);
        assertRenditionNotSynced(asset, TEST_RENDITION_1280);
        assertRenditionNotSynced(asset, TEST_RENDITION_140);
        assertRenditionNotSynced(asset, TEST_RENDITION_48);
    }

    @Test
    public void testSycnAssetFailureLogsErrorIfAssetCannotBeFlaggedAsFailed() throws Exception {
        setupFinish();

        ResourceResolver remoteAssetsResourceResolver = remoteAssetsConfig.getResourceResolver();
        doThrow(new RuntimeException("test unable to fetch")).when(remoteAssetsResourceResolver).getResource(TEST_ASSET_CONTENT_PATH);
        doReturn(remoteAssetsResourceResolver).when(remoteAssetsConfig).getResourceResolver();

        Resource resource = context.resourceResolver().getResource(TEST_ASSET_CONTENT_PATH);
        assertFalse(remoteAssetsBinarySync.syncAsset(resource));
        
        LogTester.assertLogText("Failed to mark sync of "+TEST_ASSET_CONTENT_PATH + " as failed");

    }
}

