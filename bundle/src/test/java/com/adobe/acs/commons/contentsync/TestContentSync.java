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

import com.adobe.acs.commons.adobeio.service.IntegrationService;
import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.internal.ContentReaderWhiteboard;
import org.apache.sling.jcr.contentloader.internal.DefaultContentImporter;
import org.apache.sling.jcr.contentloader.internal.readers.JsonReader;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.adobe.acs.commons.contentsync.ConfigurationUtils.CONNECT_TIMEOUT_KEY;
import static com.adobe.acs.commons.contentsync.ConfigurationUtils.HOSTS_PATH;
import static com.adobe.acs.commons.contentsync.ConfigurationUtils.SETTINGS_PATH;
import static com.adobe.acs.commons.contentsync.ConfigurationUtils.SO_TIMEOUT_STRATEGY_KEY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TestContentSync {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    ContentSync contentSync;
    ContentReader reader;
    RemoteInstance remoteInstance;
    CryptoSupport crypto;


    @Before
    public void setUp() throws Exception {
        context.registerInjectActivateService(new JsonReader());
        context.registerInjectActivateService(new ContentReaderWhiteboard());
        crypto = MockCryptoSupport.getInstance();
        context.registerService(CryptoSupport.class, crypto);

        context.addModelsForClasses(SyncHostConfiguration.class);
        reader = new ContentReader(context.resourceResolver().adaptTo(Session.class));

        String configPath = HOSTS_PATH + "/host1";
        context.build().resource(configPath, "host", "http://localhost:4502", "username", "", "password", "");
        context.build().resource(SETTINGS_PATH, SO_TIMEOUT_STRATEGY_KEY, 1000, CONNECT_TIMEOUT_KEY, "1000");
        ValueMap generalSettings = context.resourceResolver().getResource(configPath).getValueMap();
        SyncHostConfiguration hostConfiguration =
                context.getService(ModelFactory.class)
                        .createModel(context.resourceResolver().getResource(configPath), SyncHostConfiguration.class);
        ContentImporter contentImporter = context.registerInjectActivateService(new DefaultContentImporter());
        remoteInstance = spy(new RemoteInstance(hostConfiguration, generalSettings, null));

        contentSync = new ContentSync(remoteInstance, context.resourceResolver(), contentImporter);

    }

    @Test
    public void sortNodes() throws Exception {
        context.build().atParent()
                .resource("/content/sorted", "jcr:primaryType", "cq:Page")
                .resource("/content/sorted/one")
                .resource("/content/sorted/two")
                .resource("/content/sorted/three")
        ;
        Node node = context.resourceResolver().getResource("/content/sorted").adaptTo(Node.class);
        // assert initial ordering
        assertEquals(Arrays.asList("one", "two", "three"), getChildNodeNames(node));

        contentSync.sort(node, Arrays.asList("three", "one", "two"));
        assertEquals(Arrays.asList("three", "one", "two"), getChildNodeNames(node));

        // assert initial ordering
        contentSync.sort(node, Arrays.asList("one", "unknown", "three", "rep:policy", "two")); // unknown / non-synced nodes are ignored
        assertEquals(Arrays.asList("one", "three", "two"), getChildNodeNames(node));

        context.build()
                .resource("/content/nonsortable", "jcr:primaryType", "sling:Folder")
                .resource("/content/nonsortable/three")
                .resource("/content/nonsortable/two")
                .resource("/content/nonsortable/one")
        ;

        // The sort operation has no effect if the parent node does not support orderable child nodes
        node = context.resourceResolver().getResource("/content/nonsortable").adaptTo(Node.class);
        assertEquals(Arrays.asList("three", "two", "one"), getChildNodeNames(node));
        contentSync.sort(node, Arrays.asList("one", "three", "rep:policy", "two"));
        assertEquals(Arrays.asList("three", "two", "one"), getChildNodeNames(node)); // ordering didn't change
    }

    /**
     * @return list of names of child nodes
     */
    private List<String> getChildNodeNames(Node node) throws RepositoryException {
        List<String> children = new ArrayList<>();
        for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
            Node child = it.nextNode();
            children.add(child.getName());
        }
        return children;
    }

    @Test
    public void sortNodesFromRemote() throws Exception {
        String contentRoot = "/content/dam/riverside-camping-australia";
        context.load().json(getClass().getResourceAsStream("/contentsync/riverside-camping-australia.1.json"), contentRoot);

        //fake HTTP call to get the list of children from, remote
        doReturn(getClass().getResourceAsStream("/contentsync/riverside-camping-australia.1.json"))
                .when(remoteInstance).getStream(anyString());

        Node node = context.resourceResolver().getResource(contentRoot).adaptTo(Node.class);
        List<String> children = contentSync.sort(node);

        assertEquals(Arrays.asList(
                "adobestock-216674449.jpeg",
                "jcr:content",
                "adobe_waadobe_wa_mg_2466.jpg",
                "riverside-camping-australia",
                "adobestock-257541512.jpeg",
                "adobestock-238491803.jpeg",
                "adobestock-178022573.jpeg",
                "adobestock-167833331.jpeg"), children);
    }

    @Test
    public void testClearContent() throws Exception {
        String path = "/content/contentsync/page";
        context.build().resource(path, "jcr:primaryType", "cq:Page");
        context.load().json(getClass().getResourceAsStream("/contentsync/jcr_content.json"), path + "/jcr:content");

        Node node = context.resourceResolver().getResource(path).adaptTo(Node.class);
        Node jcrContent = node.getNode("jcr:content");

        List<String> userProps = Arrays.asList("jcr:title", "cq:conf", "cq:designPath");
        List<String> systemProps = Arrays.asList(
                "jcr:created", "jcr:createdBy", "jcr:uuid", "jcr:versionHistory",
                "jcr:predecessors", "jcr:isCheckedOut", "jcr:mixinTypes", "jcr:primaryType", "jcr:baseVersion");


        assertTrue(jcrContent.getNodes().hasNext()); // jcr:content has children initially
        for (String name : userProps) {
            assertTrue(name, jcrContent.hasProperty(name));  // there is user data in jcr:content
        }
        for (String name : systemProps) {
            assertTrue(name, jcrContent.hasProperty(name)); // there are system properties
        }

        contentSync.clearContent(node);

        assertFalse(jcrContent.getNodes().hasNext()); // no children under jcr:content
        for (String name : userProps) {
            assertFalse(name, jcrContent.hasProperty(name)); // user properties in jcr:content are cleared
        }
        for (String name : systemProps) {
            assertTrue(name, jcrContent.hasProperty(name)); // system properties are retained
        }
    }

    @Test
    public void testEnsureParent_NonExisting() throws IOException, RepositoryException, URISyntaxException {
        String path = "/content/my-site/folder/page";

        // parent does not exist. make HTTP call to fetch jcr:primaryType of it from remote
        doReturn("cq:Page").when(remoteInstance).getPrimaryType(anyString());
        Node parent = contentSync.ensureParent(path);

        assertEquals("/content/my-site/folder", parent.getPath());
        assertEquals("cq:Page", parent.getPrimaryNodeType().getName());
        assertEquals("cq:Page", parent.getParent().getPrimaryNodeType().getName());
    }

    @Test
    public void testEnsureParent_Existing() throws IOException, RepositoryException, URISyntaxException {
        String path = "/content/my-site/folder/page";
        context.build().withIntermediatePrimaryType("sling:Folder")
                .resource(path);

        Node parent = contentSync.ensureParent(path);
        verify(remoteInstance, never()).getPrimaryType(anyString()); // no need to call the remote instance

        assertEquals("/content/my-site/folder", parent.getPath());
        assertEquals("sling:Folder", parent.getPrimaryNodeType().getName());
        assertEquals("sling:Folder", parent.getParent().getPrimaryNodeType().getName());
    }

    @Test
    public void testCopyBinaryData_ExistingProperty() throws Exception {
        context.build()
                .resource("/content/image")
                .file("file", new ByteArrayInputStream(new byte[]{1, 2, 3}), "text/plain", 0L);

        // fetch binary data from remote
        doReturn(new ByteArrayInputStream(new byte[]{2, 3})).when(remoteInstance).getStream(anyString());

        String propertyPath = "/content/image/file/jcr:content/jcr:data";
        List<String> paths = Arrays.asList(propertyPath);
        contentSync.copyBinaries(paths);

        byte[] data = IOUtils.toByteArray(
                context.resourceResolver().adaptTo(Session.class).getProperty(propertyPath).getBinary().getStream()
        );
        assertArrayEquals(new byte[]{2, 3}, data);

    }

    @Test
    public void testCopyBinaryData_NewProperty() throws Exception {
        context.build()
                .resource("/content/image/file", "jcr:primaryType", "nt:file")
                .resource("/content/image/file/jcr:content",
                        "jcr:primaryType", "nt:resource", "jcr:data", ContentReader.BINARY_DATA_PLACEHOLDER)
                .commit()
        ;

        // fetch binary data from remote
        doReturn(new ByteArrayInputStream(new byte[]{2, 3})).when(remoteInstance).getStream(anyString());

        String propertyPath = "/content/image/file/jcr:content/jcr:data";
        List<String> paths = Arrays.asList(propertyPath);
        contentSync.copyBinaries(paths);

        byte[] data = IOUtils.toByteArray(
                context.resourceResolver().adaptTo(Session.class).getProperty(propertyPath).getBinary().getStream()
        );
        assertArrayEquals(new byte[]{2, 3}, data);
    }


    @Test
    public void testImportNewAsset() throws Exception {
        context.build().resource("/content/dam", "jcr:primaryType", "sling:OrderedFolder");
        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", "/content/dam/asset")
                .add("exportUri", "/content/dam/asset/jcr:content.infinity.json")
                .add("jcr:mixinTypes", Json.createArrayBuilder().add("mix:referenceable").build() )
                .add("jcr:primaryType", "dam:Asset")
                .build();

        JsonObject object = Json.createReader(getClass().getResourceAsStream("/contentsync/asset.json")).readObject();
        JsonObject sanitizedJson = reader.sanitize(object);
        contentSync.importData(new CatalogItem(catalogItem), sanitizedJson);

        Asset asset = context.resourceResolver().getResource("/content/dam/asset").adaptTo(Asset.class);
        assertArrayEquals(new String[]{"mix:referenceable"}, asset.adaptTo(Resource.class).getValueMap().get("jcr:mixinTypes", String[].class));

        byte[] data = IOUtils.toByteArray(
                asset.getOriginal().getStream()
        );
        assertArrayEquals(ContentReader.BINARY_DATA_PLACEHOLDER.getBytes(), data);
        assertEquals("image/jpeg", asset.getMimeType());

        assertEquals("no", asset.getMetadata("dam:Progressive"));
        assertEquals("Adobe PDF library 15.00", asset.getMetadata("pdf:Producer"));
        assertEquals((long) 657, asset.getMetadata("tiff:ImageWidth"));
    }


    @Test
    public void testUpdateExistingAsset() throws Exception {
        context.build().resource("/content/dam", "jcr:primaryType", "sling:OrderedFolder");
        String assetPath = "/content/dam/asset";
        context.assetManager().createAsset(assetPath, new ByteArrayInputStream(new byte[]{1, 2, 3}), "text/plain", false);
        context.resourceResolver()
                .getResource(assetPath + "/jcr:content/metadata")
                .adaptTo(ModifiableValueMap.class)
                .put("test", "remove me");

        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", assetPath)
                .add("exportUri", assetPath + "/jcr:content.infinity.json")
                .add("jcr:primaryType", "dam:Asset")
                .build();

        JsonObject object = Json.createReader(getClass().getResourceAsStream("/contentsync/asset.json")).readObject();
        JsonObject sanitizedJson = reader.sanitize(object);
        contentSync.importData(new CatalogItem(catalogItem), sanitizedJson);

        Asset asset = context.resourceResolver().getResource(assetPath).adaptTo(Asset.class);

        byte[] data = IOUtils.toByteArray(
                asset.getOriginal().getStream()
        );
        assertArrayEquals(ContentReader.BINARY_DATA_PLACEHOLDER.getBytes(), data);
        assertEquals("image/jpeg", asset.getMimeType());

        assertEquals(null, asset.getMetadata("test")); // any properties from that existed before import are wiped off
        assertEquals("no", asset.getMetadata("dam:Progressive"));
        assertEquals("Adobe PDF library 15.00", asset.getMetadata("pdf:Producer"));
        assertEquals((long) 657, asset.getMetadata("tiff:ImageWidth"));
    }

    @Test
    public void testUpdatePagePreserveVersionHistory() throws Exception {
        context.build().resource("/content/wknd", "jcr:primaryType", "cq:Page");
        Page page = context.pageManager().create("/content/wknd", "test", "test", "Test");
        Node jcrContent = page.getContentResource().adaptTo(Node.class);

        Session session = context.resourceResolver().adaptTo(Session.class);
        VersionManager versionManager = session.getWorkspace().getVersionManager();

        createVersion(jcrContent, "Version 1");

        String pagePath = "/content/wknd/test";
        assertNotNull(versionManager.getVersionHistory(pagePath + "/jcr:content").getVersionByLabel("Version 1"));

        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", pagePath)
                .add("exportUri", pagePath + "/jcr:content.infinity.json")
                .add("jcr:primaryType", "cq:Page")
                .build();

        JsonObject object = Json.createReader(getClass().getResourceAsStream("/contentsync/wknd-faqs.json")).readObject();
        JsonObject sanitizedJson = reader.sanitize(object);
        contentSync.importData(new CatalogItem(catalogItem), sanitizedJson);

        // assert the version is still there
        assertNotNull(versionManager.getVersionHistory(pagePath + "/jcr:content").getVersionByLabel("Version 1"));
    }

    /**
     * Mimic PageManagerImpl#createVersion
     */
    private void createVersion(Node node, String versionLabel) throws RepositoryException {
        Session session = context.resourceResolver().adaptTo(Session.class);
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        node.addMixin("mix:versionable");
        session.save();

        try {
            Version v = versionManager.checkin(node.getPath());
            v.getContainingHistory().addVersionLabel(v.getName(), versionLabel, false);

        } finally {
            versionManager.checkout(node.getPath());
        }
        session.save();
    }

    @Test
    public void testImportFolder() throws Exception {
        context.build().resource("/content/wknd", "jcr:primaryType", "cq:Page");

        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", "/content/wknd/test")
                .add("exportUri", "/content/wknd/test.json")
                .add("jcr:primaryType", "sling:OrderedFolder")
                .build();

        JsonObject object = Json.createReader(getClass().getResourceAsStream("/contentsync/ordered-folder.json")).readObject();
        JsonObject sanitizedJson = reader.sanitize(object);
        contentSync.importData(new CatalogItem(catalogItem), sanitizedJson);

        ValueMap vm = context.resourceResolver().getResource("/content/wknd/test").getValueMap();
        assertEquals("Wknd Fragments", vm.get("jcr:title"));
        assertEquals("sling:OrderedFolder", vm.get("jcr:primaryType"));
        assertEquals("html", vm.get("cq:adobeTargetExportFormat"));
    }

    @Test
    public void testAutoCheckout() throws Exception {
        String path = "/content/wknd/page";
        Page pg = context.create().page(path);
        Node jcrContent = pg.getContentResource().adaptTo(Node.class);
        createVersion(jcrContent, "test 1");
        jcrContent.checkin();

        JsonObject catalogItem = Json.createObjectBuilder()
                .add("path", path)
                .add("exportUri", path + "/jcr:content.infinity.json")
                .add("jcr:primaryType", "cq:Page")
                .build();

        JsonObject object = Json.createReader(getClass().getResourceAsStream("/contentsync/wknd-faqs.json")).readObject();
        JsonObject sanitizedJson = reader.sanitize(object);
        contentSync.importData(new CatalogItem(catalogItem), sanitizedJson);

        ValueMap vm = context.resourceResolver().getResource(path + "/jcr:content").getValueMap();
        assertEquals("FAQs", vm.get("jcr:title"));
        assertEquals(true, vm.get("jcr:isCheckedOut"));
    }

    @Test
    public void testSetupOauthInstance() throws Exception {
        String configPath =  HOSTS_PATH + "/oauthHost";

        IntegrationService integrationService = mock(IntegrationService.class);

        Resource configResource = context.create().resource(configPath,
                        "host", "http://localhost:4502", "authType", "oauth", "accessTokenProviderName", "publish-cloud");
        Resource generalSettings = context.resourceResolver().getResource(SETTINGS_PATH);
        SyncHostConfiguration hostConfiguration =
                context.getService(ModelFactory.class).createModel(configResource, SyncHostConfiguration.class);

        remoteInstance = new RemoteInstance(hostConfiguration, generalSettings.getValueMap(), integrationService);
        verify(integrationService, atLeastOnce()).getAccessToken();
    }

    @Test
    public void testErrorGettingAccessToken() throws Exception {
        String configPath =  HOSTS_PATH + "/oauthHost";

        IntegrationService integrationService = mock(IntegrationService.class);
        doThrow(new RuntimeException("unauthorized client")).when(integrationService).getAccessToken();

        Resource configResource = context.create().resource(configPath,
                "host", "http://localhost:4502", "authType", "oauth", "accessTokenProviderName", "publish-cloud");
        Resource generalSettings = context.resourceResolver().getResource(SETTINGS_PATH);
        SyncHostConfiguration hostConfiguration =
                context.getService(ModelFactory.class).createModel(configResource, SyncHostConfiguration.class);

        try {
            remoteInstance = new RemoteInstance(hostConfiguration, generalSettings.getValueMap(), integrationService);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Failed to get an access token: unauthorized client.", e.getMessage());
        }
    }
}