package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.assets.FileExtensionMimeTypeConstants;
import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.adobe.acs.commons.testutil.LogTester;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.tagging.TagConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.TEST_DAM_PATH_A;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.TEST_DAM_PATH_B;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.TEST_TAGS_PATH_A;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.TEST_TAGS_PATH_B;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.getBytes;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.getPlaceholderAsset;
import static com.adobe.acs.commons.remoteassets.impl.RemoteAssetsTestUtil.getRemoteAssetsConfigs;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class RemoteAssetsNodeSyncImplTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);
    private MockServerClient mockServerClient;

    private RemoteAssetsConfig remoteAssetsConfig;
    private RemoteAssetsNodeSyncImpl remoteAssetsNodeSync;
    private Map<String, List<String>> nodeMixinsTracker;

    @Before
    public void setup() throws Exception {
        ResourceResolver resourceResolver = context.resourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);

        Node nodeContent = session.getRootNode().addNode("content", DamConstants.NT_SLING_ORDEREDFOLDER);
        Node nodeDam = nodeContent.addNode("dam", DamConstants.NT_SLING_ORDEREDFOLDER);
        Node nodeDamFolder = nodeDam.addNode("test", DamConstants.NT_SLING_ORDEREDFOLDER);

        Map<String, Object> remoteAssetsConfigs = getRemoteAssetsConfigs();
        remoteAssetsConfigs.put("server.url", "http://localhost:" + mockServerRule.getPort());
        remoteAssetsConfigs.put("server.insecure", true);
        remoteAssetsConfigs.put("save.interval", 2);

        context.registerInjectActivateService(new RemoteAssetsConfigImpl(), remoteAssetsConfigs);
        RemoteAssetsNodeSyncImpl remoteAssetsNodeSyncImpl = spy(new RemoteAssetsNodeSyncImpl());
        remoteAssetsNodeSync = context.registerInjectActivateService(remoteAssetsNodeSyncImpl);

        nodeMixinsTracker = new HashMap<>();
        // This hackery is in place because MockNode#addMixin throws an UnsupportedOperationException
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                // Need to cast this to Invocation so that we can get at the raw arguments
                // to modify them prior to delegating the call to callRealMethod().
                Invocation invocation = (Invocation) invocationOnMock;

                JsonArray mixins = (JsonArray) invocation.getRawArguments()[0];
                Resource resourceSpy = spy((Resource) invocation.getRawArguments()[2]);

                Node mockResourceNode = mock(Node.class);
                doNothing().when(mockResourceNode).addMixin(anyString());
                doReturn(mockResourceNode).when(resourceSpy).adaptTo(Node.class);

                invocation.getRawArguments()[2] = resourceSpy;
                invocation.callRealMethod();

                Iterator<JsonElement> mixinIter = mixins.iterator();
                while (mixinIter.hasNext()) {
                    List<String> nodeMixins = nodeMixinsTracker.get(resourceSpy.getPath());
                    if (nodeMixins == null) {
                        nodeMixins = new ArrayList<>();
                        nodeMixinsTracker.put(resourceSpy.getPath(), nodeMixins);
                    }
                    nodeMixins.add(mixinIter.next().getAsString());
                }
                return null;
            }
        }).when(remoteAssetsNodeSyncImpl).setNodeMixinsProperty(any(JsonArray.class), any(String.class), any(Resource.class));

        LogTester.reset();
    }

    private void setupMockRequest(String path, String filename) throws IOException {
        String responseJson = new String(getBytes(ClassLoader.getSystemResourceAsStream("remoteassetstest/nodesync/" + filename)), StandardCharsets.UTF_8);
        HttpRequest request = request().withMethod("GET").withPath(path);
        HttpResponse response = response().withStatusCode(HttpServletResponse.SC_OK).withBody(responseJson);
        mockServerClient.when(request).respond(response);
    }

    private void setupMockSyncRequests() throws IOException {
        mockServerClient = new MockServerClient("localhost", mockServerRule.getPort());

        setupMockRequest(TEST_TAGS_PATH_A + ".1.json", "tags.a.json");
        setupMockRequest(TEST_TAGS_PATH_A + "/tag_a1.1.json", "tags.a.a1.json");
        setupMockRequest(TEST_TAGS_PATH_A + "/tag_a1/tag_a1a.1.json", "tags.a.a1.a1a.json");
        setupMockRequest(TEST_TAGS_PATH_A + "/tag_a2.1.json", "tags.a.a2.json");

        setupMockRequest(TEST_TAGS_PATH_B + ".1.json", "tags.b.json");
        setupMockRequest(TEST_TAGS_PATH_B + "/tag_b1.1.json", "tags.b.b1.json");

        setupMockRequest(TEST_DAM_PATH_A + ".1.json", "dam.a.json");
        setupMockRequest(TEST_DAM_PATH_A + "/jcr:content.1.json", "dam.a.content.json");

        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg.1.json", "dam.a.a1jpg.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content.1.json", "dam.a.a1jpg.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content/metadata.1.json", "dam.a.a1jpg.content.metadata.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content/renditions.1.json", "dam.a.a1jpg.content.renditions.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content/renditions/original.1.json", "dam.a.a1jpg.content.renditions.original.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content/renditions/original/jcr:content.1.json", "dam.a.a1jpg.content.renditions.original.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content/renditions/cq5dam.web.1280.1280.jpeg.1.json", "dam.a.a1jpg.content.renditions.1280.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content/renditions/cq5dam.web.1280.1280.jpeg/jcr:content.1.json", "dam.a.a1jpg.content.renditions.1280.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content/renditions/cq5dam.thumbnail.48.48.png.1.json", "dam.a.a1jpg.content.renditions.48.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a1.jpg/jcr:content/renditions/cq5dam.thumbnail.48.48.png/jcr:content.1.json", "dam.a.a1jpg.content.renditions.48.content.json");

        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1.1.json", "dam.a.a1sub.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/jcr:content.1.json", "dam.a.a1sub.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif.1.json", "dam.a.a1sub.a3gif.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content.1.json", "dam.a.a1sub.a3gif.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content/metadata.1.json", "dam.a.a1sub.a3gif.content.metadata.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content/renditions.1.json", "dam.a.a1sub.a3gif.content.renditions.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content/renditions/original.1.json", "dam.a.a1sub.a3gif.content.renditions.original.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content/renditions/original/jcr:content.1.json", "dam.a.a1sub.a3gif.content.renditions.original.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content/renditions/cq5dam.web.1280.1280.gif.1.json", "dam.a.a1sub.a3gif.content.renditions.1280.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content/renditions/cq5dam.web.1280.1280.gif/jcr:content.1.json", "dam.a.a1sub.a3gif.content.renditions.1280.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content/renditions/cq5dam.thumbnail.48.48.png.1.json", "dam.a.a1sub.a3gif.content.renditions.48.json");
        setupMockRequest(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif/jcr:content/renditions/cq5dam.thumbnail.48.48.png/jcr:content.1.json", "dam.a.a1sub.a3gif.content.renditions.48.content.json");

        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png.1.json", "dam.a.a2png.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content.1.json", "dam.a.a2png.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content/metadata.1.json", "dam.a.a2png.content.metadata.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content/renditions.1.json", "dam.a.a2png.content.renditions.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content/renditions/original.1.json", "dam.a.a2png.content.renditions.original.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content/renditions/original/jcr:content.1.json", "dam.a.a2png.content.renditions.original.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content/renditions/cq5dam.web.1280.1280.png.1.json", "dam.a.a2png.content.renditions.1280.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content/renditions/cq5dam.web.1280.1280.png/jcr:content.1.json", "dam.a.a2png.content.renditions.1280.content.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content/renditions/cq5dam.thumbnail.48.48.png.1.json", "dam.a.a2png.content.renditions.48.json");
        setupMockRequest(TEST_DAM_PATH_A + "/image_a2.png/jcr:content/renditions/cq5dam.thumbnail.48.48.png/jcr:content.1.json", "dam.a.a2png.content.renditions.48.content.json");

        setupMockRequest(TEST_DAM_PATH_B + ".1.json", "dam.b.json");
        setupMockRequest(TEST_DAM_PATH_B + "/jcr:content.1.json", "dam.b.content.json");
    }

    public void verifyPlaceholder(String extension, String mimeType) throws Exception {
        ResourceResolver resourceResolver = context.resourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);

        // Create the asset complete with original and alternate renditions
        Node nodeDamFolder = session.getNode("/content/dam/test");
        Node nodeAsset = nodeDamFolder.addNode("test_asset." + extension, DamConstants.NT_DAM_ASSET);
        Node nodeAssetContent = nodeAsset.addNode(JcrConstants.JCR_CONTENT, DamConstants.NT_DAM_ASSETCONTENT);
        Node nodeAssetRenditions = nodeAssetContent.addNode(DamConstants.RENDITIONS_FOLDER, JcrConstants.NT_FOLDER);
        Node nodeRenditionOriginal = nodeAssetRenditions.addNode("original", JcrConstants.NT_FILE);
        Node nodeRenditionOriginalContent = nodeRenditionOriginal.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        nodeRenditionOriginalContent.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
        Node nodeRenditionAlt = nodeAssetRenditions.addNode("alt." + extension, JcrConstants.NT_FILE);
        Node nodeRenditionAltContent = nodeRenditionAlt.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        nodeRenditionAltContent.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);


        byte[] expectedPlaceholder = getPlaceholderAsset(extension);

        // Test that the original renditon (no extension) gets the expected placeholder
        Resource originalRenditionContent = resourceResolver.getResource(nodeRenditionOriginalContent.getPath());
        byte[] originalRenditionPlaceholder = getBytes(remoteAssetsNodeSync.getRemoteAssetPlaceholder(originalRenditionContent));
        assertEquals(expectedPlaceholder.length, originalRenditionPlaceholder.length);

        // Test that the alternate rendition (with extension) gets the expected placeholder
        Resource altRenditionContent = resourceResolver.getResource(nodeRenditionAltContent.getPath());
        byte[] altRenditionPlaceholder = getBytes(remoteAssetsNodeSync.getRemoteAssetPlaceholder(altRenditionContent));
        assertEquals(expectedPlaceholder.length, altRenditionPlaceholder.length);
    }

    @Test
    public void testSync() throws IOException {
        setupMockSyncRequests();

        remoteAssetsNodeSync.syncAssetNodes();

        validateSyncTags();
        validateSyncAssets();

        LogTester.assertLogText("Starting sync of nodes for " + TEST_TAGS_PATH_A);
        LogTester.assertLogText("Completed sync of nodes for " + TEST_TAGS_PATH_A);
        LogTester.assertLogText("Starting sync of nodes for " + TEST_TAGS_PATH_B);
        LogTester.assertLogText("Completed sync of nodes for " + TEST_TAGS_PATH_B);
        LogTester.assertLogText("Starting sync of nodes for " + TEST_DAM_PATH_A);
        LogTester.assertLogText("Completed sync of nodes for " + TEST_DAM_PATH_A);
        LogTester.assertLogText("Starting sync of nodes for " + TEST_DAM_PATH_B);
        LogTester.assertLogText("Completed sync of nodes for " + TEST_DAM_PATH_B);

        LogTester.assertLogText("Unable to assign property 'testArrayDecimalBadData' to resource '" + TEST_TAGS_PATH_A + "/tag_a1/tag_a1a'");
        LogTester.assertLogText("Executed incremental save of node sync.");
    }

    private void validateSyncTags() {
        ResourceResolver resourceResolver = context.resourceResolver();

        Resource tagFolderA = resourceResolver.getResource(TEST_TAGS_PATH_A);
        assertEquals(TagConstants.NT_TAG, tagFolderA.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
        assertEquals("cq/tagging/components/tag", tagFolderA.getResourceType());

        Resource tagA1 = resourceResolver.getResource(TEST_TAGS_PATH_A + "/tag_a1");
        assertEquals(TagConstants.NT_TAG, tagA1.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
        assertEquals("cq/tagging/components/tag", tagA1.getResourceType());
        assertEquals("Tag A1", tagA1.getValueMap().get(JcrConstants.JCR_TITLE));
        assertEquals("Test Tag #A1", tagA1.getValueMap().get(JcrConstants.JCR_DESCRIPTION));

        Resource tagA2 = resourceResolver.getResource(TEST_TAGS_PATH_A + "/tag_a2");
        assertEquals(TagConstants.NT_TAG, tagA2.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
        assertEquals("cq/tagging/components/tag", tagA2.getResourceType());
        assertEquals("Tag A2", tagA2.getValueMap().get(JcrConstants.JCR_TITLE));
        assertEquals("Test Tag #A2", tagA2.getValueMap().get(JcrConstants.JCR_DESCRIPTION));

        Resource tagA1a = resourceResolver.getResource(TEST_TAGS_PATH_A + "/tag_a1/tag_a1a");
        // Purposely using the version of `ValueMap#get` that does not coerce type so that
        // we can validate that the types are what we expect in the JCR.
        assertEquals("Tag A1a", tagA1a.getValueMap().get(JcrConstants.JCR_TITLE));
        assertEquals("Test Tag #A1a", tagA1a.getValueMap().get(JcrConstants.JCR_DESCRIPTION));
        assertEquals(Boolean.TRUE, tagA1a.getValueMap().get("testBool"));
        Calendar testDate = new GregorianCalendar(2019, 0, 6, 18, 15, 24);
        testDate.setTimeZone(TimeZone.getTimeZone("GMT-06:00"));
        assertEquals(testDate.getTimeInMillis(), ((Calendar) tagA1a.getValueMap().get("testDate")).getTimeInMillis());
        assertEquals(new BigDecimal("453.3218937128937"), tagA1a.getValueMap().get("testDecimal"));
        assertEquals(new Long(4223), tagA1a.getValueMap().get("testLong"));
        assertEquals(Arrays.asList("Hello", "World"), Arrays.asList((String[]) tagA1a.getValueMap().get("testArrayString")));
        assertEquals(Arrays.asList(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE),
                Arrays.asList((Boolean[]) tagA1a.getValueMap().get("testArrayBool")));
        assertEquals(Arrays.asList(new BigDecimal("1.1"), new BigDecimal("28"), new BigDecimal("4.8972834")),
                Arrays.asList((BigDecimal[]) tagA1a.getValueMap().get("testArrayDecimal")));
        assertEquals(Arrays.asList(new Long(53), new Long(4), new Long(55425546)),
                Arrays.asList((Long[]) tagA1a.getValueMap().get("testArrayLong")));
        assertNull(tagA1a.getValueMap().get(":testBinary"));
        assertNull(tagA1a.getValueMap().get("testArrayDecimalBadData"));

        Resource tagFolderB = resourceResolver.getResource(TEST_TAGS_PATH_B);
        assertEquals(TagConstants.NT_TAG, tagFolderB.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
        assertEquals("cq/tagging/components/tag", tagFolderB.getResourceType());

        Resource tagB1 = resourceResolver.getResource(TEST_TAGS_PATH_B + "/tag_b1");
        assertEquals(TagConstants.NT_TAG, tagB1.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
        assertEquals("cq/tagging/components/tag", tagB1.getResourceType());
        assertEquals("Tag B1", tagB1.getValueMap().get(JcrConstants.JCR_TITLE));
        assertEquals("Test Tag #B1", tagB1.getValueMap().get(JcrConstants.JCR_DESCRIPTION));
    }

    private void validateSyncAssets() throws IOException {
        ResourceResolver resourceResolver = context.resourceResolver();

        Resource assetFolderA = resourceResolver.getResource(TEST_DAM_PATH_A);
        assertEquals(DamConstants.NT_SLING_ORDEREDFOLDER, assetFolderA.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
        Resource assetFolderAContent = assetFolderA.getChild(JcrConstants.JCR_CONTENT);
        assertEquals(JcrConstants.NT_UNSTRUCTURED, assetFolderAContent.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
        assertNull(assetFolderAContent.getChild(DamConstants.THUMBNAIL_NODE));

        Resource asset1Resource = resourceResolver.getResource(TEST_DAM_PATH_A + "/image_a1.jpg");
        assertEquals(DamConstants.NT_DAM_ASSET, asset1Resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
        assertEquals(Arrays.asList(JcrConstants.MIX_REFERENCEABLE, JcrConstants.MIX_VERSIONABLE), nodeMixinsTracker.get(asset1Resource.getPath()));
        assertNull(asset1Resource.getValueMap().get(JcrConstants.JCR_CREATED));
        assertNull(asset1Resource.getValueMap().get(JcrConstants.JCR_CREATED_BY));
        assertNull(asset1Resource.getValueMap().get(JcrConstants.JCR_VERSIONHISTORY));
        assertNull(asset1Resource.getValueMap().get(JcrConstants.JCR_BASEVERSION));
        assertNull(asset1Resource.getValueMap().get(JcrConstants.JCR_ISCHECKEDOUT));
        assertNull(asset1Resource.getValueMap().get(JcrConstants.JCR_UUID));
        assertNull(asset1Resource.getValueMap().get(JcrConstants.JCR_PREDECESSORS));

        Resource asset1ContentResource = asset1Resource.getChild(JcrConstants.JCR_CONTENT);
        assertEquals(DamConstants.NT_DAM_ASSETCONTENT, asset1ContentResource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));

        Resource asset1MetadataResource = asset1ContentResource.getChild(DamConstants.METADATA_FOLDER);
        assertEquals(Arrays.asList(TagConstants.NT_TAGGABLE), nodeMixinsTracker.get(asset1MetadataResource.getPath()));

        Asset asset1 = asset1Resource.adaptTo(Asset.class);
        assertEquals(FileExtensionMimeTypeConstants.EXT_JPEG_JPG, asset1.getMetadata("dam:MIMEtype"));
        assertEquals(new BigDecimal("0.6944444179534912"), asset1.getMetadata("dam:Physicalheightininches"));
        assertArrayEquals(new String[] {"a:tag_a1/tag_a1a", "a:tag_a2", "b:tag_b1"}, (String[]) asset1.getMetadata(TagConstants.PN_TAGS));

        Rendition asset1RenditionOriginal = asset1.getOriginal();
        assertEquals(FileExtensionMimeTypeConstants.EXT_JPEG_JPG, asset1RenditionOriginal.getMimeType());
        assertEquals(getPlaceholderAsset("jpg").length, asset1RenditionOriginal.getSize());

        Rendition asset1Rendition1280 = asset1.getRendition("cq5dam.web.1280.1280.jpeg");
        assertNotNull(asset1Rendition1280);
        assertEquals(FileExtensionMimeTypeConstants.EXT_JPEG_JPG, asset1Rendition1280.getMimeType());
        assertEquals(getPlaceholderAsset("jpeg").length, asset1Rendition1280.getSize());

        Rendition asset1Rendition48 = asset1.getRendition("cq5dam.thumbnail.48.48.png");
        assertNotNull(asset1Rendition48);
        assertEquals(FileExtensionMimeTypeConstants.EXT_PNG, asset1Rendition48.getMimeType());
        assertEquals(getPlaceholderAsset("png").length, asset1Rendition48.getSize());


        Resource asset2Resource = resourceResolver.getResource(TEST_DAM_PATH_A + "/image_a2.png");
        assertEquals(DamConstants.NT_DAM_ASSET, asset2Resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));

        Asset asset2 = asset2Resource.adaptTo(Asset.class);
        assertEquals(FileExtensionMimeTypeConstants.EXT_PNG, asset2.getMetadata("dam:MIMEtype"));
        assertEquals(new BigDecimal("0.6943576335906982"), asset2.getMetadata("dam:Physicalheightininches"));

        Rendition asset2RenditionOriginal = asset2.getOriginal();
        assertEquals(FileExtensionMimeTypeConstants.EXT_PNG, asset2RenditionOriginal.getMimeType());
        assertEquals(getPlaceholderAsset("png").length, asset2RenditionOriginal.getSize());

        Rendition asset2Rendition1280 = asset2.getRendition("cq5dam.web.1280.1280.png");
        assertNotNull(asset2Rendition1280);
        assertEquals(FileExtensionMimeTypeConstants.EXT_PNG, asset2Rendition1280.getMimeType());
        assertEquals(getPlaceholderAsset("png").length, asset2Rendition1280.getSize());

        Rendition asset2Rendition48 = asset2.getRendition("cq5dam.thumbnail.48.48.png");
        assertNotNull(asset2Rendition48);
        assertEquals(FileExtensionMimeTypeConstants.EXT_PNG, asset2Rendition48.getMimeType());
        assertEquals(getPlaceholderAsset("png").length, asset2Rendition48.getSize());


        Resource asset3Resource = resourceResolver.getResource(TEST_DAM_PATH_A + "/subfolder1/image_a3.gif");
        assertEquals(DamConstants.NT_DAM_ASSET, asset3Resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));

        Asset asset3 = asset3Resource.adaptTo(Asset.class);
        assertEquals(FileExtensionMimeTypeConstants.EXT_GIF, asset3.getMetadata("dam:MIMEtype"));
        assertEquals(new BigDecimal("0.6944444179534912"), asset3.getMetadata("dam:Physicalheightininches"));

        Rendition asset3RenditionOriginal = asset3.getOriginal();
        assertEquals(FileExtensionMimeTypeConstants.EXT_GIF, asset3RenditionOriginal.getMimeType());
        assertEquals(getPlaceholderAsset("gif").length, asset3RenditionOriginal.getSize());

        Rendition asset3Rendition1280 = asset3.getRendition("cq5dam.web.1280.1280.gif");
        assertNotNull(asset3Rendition1280);
        assertEquals(FileExtensionMimeTypeConstants.EXT_GIF, asset3Rendition1280.getMimeType());
        assertEquals(getPlaceholderAsset("gif").length, asset3Rendition1280.getSize());

        Rendition asset3Rendition48 = asset3.getRendition("cq5dam.thumbnail.48.48.png");
        assertNotNull(asset3Rendition48);
        assertEquals(FileExtensionMimeTypeConstants.EXT_PNG, asset3Rendition48.getMimeType());
        assertEquals(getPlaceholderAsset("png").length, asset3Rendition48.getSize());

        Resource assetFolderB = resourceResolver.getResource(TEST_DAM_PATH_B);
        assertEquals(DamConstants.NT_SLING_ORDEREDFOLDER, assetFolderB.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE));
    }

    @Test
    public void testGetRemoteAssetPlaceholder() throws Exception {
        verifyPlaceholder("3g2", FileExtensionMimeTypeConstants.EXT_3G2);
        verifyPlaceholder("3gp", FileExtensionMimeTypeConstants.EXT_3GP);
        verifyPlaceholder("aac", FileExtensionMimeTypeConstants.EXT_AAC);
        verifyPlaceholder("aiff", FileExtensionMimeTypeConstants.EXT_AIFF);
        verifyPlaceholder("avi", FileExtensionMimeTypeConstants.EXT_AVI);
        verifyPlaceholder("bmp", FileExtensionMimeTypeConstants.EXT_BMP);
        verifyPlaceholder("css", FileExtensionMimeTypeConstants.EXT_CSS);
        verifyPlaceholder("doc", FileExtensionMimeTypeConstants.EXT_DOC);
        verifyPlaceholder("docx", FileExtensionMimeTypeConstants.EXT_DOCX);
        verifyPlaceholder("ai", FileExtensionMimeTypeConstants.EXT_AI_EPS_PS);
        verifyPlaceholder("eps", FileExtensionMimeTypeConstants.EXT_AI_EPS_PS);
        verifyPlaceholder("ps", FileExtensionMimeTypeConstants.EXT_AI_EPS_PS);
        verifyPlaceholder("epub", FileExtensionMimeTypeConstants.EXT_EPUB);
        verifyPlaceholder("f4v", FileExtensionMimeTypeConstants.EXT_F4V);
        verifyPlaceholder("swf", FileExtensionMimeTypeConstants.EXT_FLA_SWF);
        verifyPlaceholder("gif", FileExtensionMimeTypeConstants.EXT_GIF);
        verifyPlaceholder("html", FileExtensionMimeTypeConstants.EXT_HTML);
        verifyPlaceholder("indd", FileExtensionMimeTypeConstants.EXT_INDD);
        verifyPlaceholder("jar", FileExtensionMimeTypeConstants.EXT_JAR);
        verifyPlaceholder("jpeg", FileExtensionMimeTypeConstants.EXT_JPEG_JPG);
        verifyPlaceholder("jpg", FileExtensionMimeTypeConstants.EXT_JPEG_JPG);
        verifyPlaceholder("m4v", FileExtensionMimeTypeConstants.EXT_M4V);
        verifyPlaceholder("midi", FileExtensionMimeTypeConstants.EXT_MIDI);
        verifyPlaceholder("mov", FileExtensionMimeTypeConstants.EXT_MOV);
        verifyPlaceholder("mp3", FileExtensionMimeTypeConstants.EXT_MP3);
        verifyPlaceholder("mp4", FileExtensionMimeTypeConstants.EXT_MP4);
        verifyPlaceholder("m2v", FileExtensionMimeTypeConstants.EXT_M2V_MPEG_MPG);
        verifyPlaceholder("mpeg", FileExtensionMimeTypeConstants.EXT_M2V_MPEG_MPG);
        verifyPlaceholder("mpg", FileExtensionMimeTypeConstants.EXT_M2V_MPEG_MPG);
        verifyPlaceholder("ogg", FileExtensionMimeTypeConstants.EXT_OGG);
        verifyPlaceholder("ogv", FileExtensionMimeTypeConstants.EXT_OGV);
        verifyPlaceholder("pdf", FileExtensionMimeTypeConstants.EXT_PDF);
        verifyPlaceholder("png", FileExtensionMimeTypeConstants.EXT_PNG);
        verifyPlaceholder("ppt", FileExtensionMimeTypeConstants.EXT_PPT);
        verifyPlaceholder("pptx", FileExtensionMimeTypeConstants.EXT_PPTX);
        verifyPlaceholder("psd", FileExtensionMimeTypeConstants.EXT_PSD);
        verifyPlaceholder("rar", FileExtensionMimeTypeConstants.EXT_RAR);
        verifyPlaceholder("rtf", FileExtensionMimeTypeConstants.EXT_RTF);
        verifyPlaceholder("svg", FileExtensionMimeTypeConstants.EXT_SVG);
        verifyPlaceholder("tar", FileExtensionMimeTypeConstants.EXT_TAR);
        verifyPlaceholder("tif", FileExtensionMimeTypeConstants.EXT_TIF_TIFF);
        verifyPlaceholder("tiff", FileExtensionMimeTypeConstants.EXT_TIF_TIFF);
        verifyPlaceholder("txt", FileExtensionMimeTypeConstants.EXT_TXT);
        verifyPlaceholder("wav", FileExtensionMimeTypeConstants.EXT_WAV);
        verifyPlaceholder("webm", FileExtensionMimeTypeConstants.EXT_WEBM);
        verifyPlaceholder("wma", FileExtensionMimeTypeConstants.EXT_WMA);
        verifyPlaceholder("wmv", FileExtensionMimeTypeConstants.EXT_WMV);
        verifyPlaceholder("xls", FileExtensionMimeTypeConstants.EXT_XLS);
        verifyPlaceholder("xlsx", FileExtensionMimeTypeConstants.EXT_XLSX);
        verifyPlaceholder("xml", FileExtensionMimeTypeConstants.EXT_XML);
        verifyPlaceholder("zip", FileExtensionMimeTypeConstants.EXT_ZIP);
        verifyPlaceholder("jpeg", "unknown");
    }
}
