/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.impl.processes.asset.AssetIngestor.ReportColumns;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.google.common.base.Function;
import io.findify.s3mock.S3Mock;
import me.alexpanov.net.FreePortFinder;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class S3AssetIngestorTest {

    private static final String TEST_BUCKET = "testbucket";

    private static final int FILE_SIZE = 57797;

    private S3Mock s3Mock;

    @Rule // Use JCR_OAK instead of JCR_MOCK so long as JCR_MOCK's MockSession.refresh() throws UnsupportedOperationException
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Mock
    private ActionManager actionManager;

    @Mock
    private AssetManager assetManager;

    @Mock
    private Asset createdAsset;

    @Captor
    private ArgumentCaptor<String> currentItemCaptor;

    @Captor
    private ArgumentCaptor<String> assetPathCaptor;

    private S3AssetIngestor ingestor;

    private AmazonS3 s3Client;

    @Before
    public void setup() throws PersistenceException {
        context.registerAdapter(ResourceResolver.class, AssetManager.class, new Function<ResourceResolver, AssetManager>() {
            @Nullable
            @Override
            public AssetManager apply(@Nullable ResourceResolver input) {
                return assetManager;
            }
        });

        context.create().resource("/content/dam", JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        context.resourceResolver().commit();
        ingestor = new S3AssetIngestor(context.getService(MimeTypeService.class));
        ingestor.jcrBasePath = "/content/dam";
        ingestor.fileFilter = new NamesFilter();
        ingestor.extensionFilter = new NamesFilter();
        ingestor.folderFilter = new NamesFilter("-.ds_store");
        ingestor.existingAssetAction = AssetIngestor.AssetAction.skip;
        ingestor.dryRunMode = false;

        int port = FreePortFinder.findFreeLocalPort();
        s3Mock = new S3Mock.Builder().withPort(port).withInMemoryBackend().build();
        s3Mock.start();

        S3ClientOptions options = S3ClientOptions.builder().setPathStyleAccess(true).build();
        s3Client = new AmazonS3Client(new AnonymousAWSCredentials());
        s3Client.setS3ClientOptions(options);
        s3Client.setEndpoint("http://localhost:" + port);
        ingestor.s3Client = s3Client;
        ingestor.bucket = TEST_BUCKET;

        s3Client.createBucket(TEST_BUCKET);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
                method.accept(context.resourceResolver());
                return null;
            }
        }).when(actionManager).deferredWithResolver(any(CheckedConsumer.class));
    }

    @After
    public void teardown() {
        s3Mock.stop();
    }

    @Test
    public void testCreateFoldersWithEmptyBucket() throws Exception {
        ingestor.init();
        ingestor.createFolders(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertFalse(context.resourceResolver().getResource("/content/dam").hasChildren());

        verify(actionManager).setCurrentItem(currentItemCaptor.capture());
        assertEquals(1, currentItemCaptor.getAllValues().size());
        assertEquals(TEST_BUCKET, currentItemCaptor.getValue());
    }

    @Test
    public void testImportAssetsWithEmptyBucket() throws Exception {
        ingestor.init();
        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertFalse(context.resourceResolver().getResource("/content/dam").hasChildren());

        verify(actionManager).setCurrentItem(currentItemCaptor.capture());
        assertEquals(1, currentItemCaptor.getAllValues().size());
        assertEquals(TEST_BUCKET, currentItemCaptor.getValue());

    }

    @Test
    public void testImportAssetsWithBucketContainingJustFolders() throws Exception {
        ingestor.init();
        s3Client.putObject(TEST_BUCKET, "folder1/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/folder3/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertFalse(context.resourceResolver().getResource("/content/dam").hasChildren());

        verify(actionManager).setCurrentItem(currentItemCaptor.capture());
        assertEquals(1, currentItemCaptor.getAllValues().size());
        assertEquals(TEST_BUCKET, currentItemCaptor.getValue());

    }

    @Test
    public void testImportAssets() throws Exception {
        ingestor.init();
        s3Client.putObject(TEST_BUCKET, "image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder1/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder1/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/folder3/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/folder3/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        when(assetManager.createAsset(anyString(), any(), anyString(), any(Boolean.class))).thenReturn(createdAsset);

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(3, ingestor.getCount(ingestor.importedAssets));
        assertEquals(3, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE * 3, (long) ingestor.importedData.get(ReportColumns.bytes));
        verify(assetManager, times(3)).createAsset(assetPathCaptor.capture(), any(), any(), eq(false));
        assertEquals(Arrays.asList("/content/dam/folder1/image.png", "/content/dam/folder2/folder3/image.png", "/content/dam/image.png"), assetPathCaptor.getAllValues());

        verify(actionManager, times(4)).setCurrentItem(currentItemCaptor.capture());
        assertEquals(Arrays.asList("testbucket", "testbucket:folder1/image.png", "testbucket:folder2/folder3/image.png", "testbucket:image.png"), currentItemCaptor.getAllValues());
    }

    @Test(expected = AssetIngestorException.class)
    public void testImportAssetsWithException() throws Exception {
        ingestor.init();
        s3Client.putObject(TEST_BUCKET, "image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());

        ingestor.importAssets(actionManager);
    }

    @Test
    public void testImportAssetsToNewRootFolder() throws Exception {
        ingestor.jcrBasePath = "/content/dam/test";
        ingestor.init();
        s3Client.putObject(TEST_BUCKET, "image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        when(assetManager.createAsset(anyString(), any(), anyString(), any(Boolean.class))).thenReturn(createdAsset);

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());

        assertNull(context.resourceResolver().getResource("/content/dam/test").getValueMap().get("jcr:title"));
        assertEquals(1, ingestor.getCount(ingestor.importedAssets));
        assertEquals(0, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE, (long)  ingestor.importedData.get(ReportColumns.bytes));
        verify(assetManager, times(1)).createAsset(assetPathCaptor.capture(), any(), any(), eq(false));
        assertEquals("/content/dam/test/image.png", assetPathCaptor.getValue());

        verify(actionManager, times(2)).setCurrentItem(currentItemCaptor.capture());
        assertEquals(Arrays.asList("testbucket", "testbucket:image.png"), currentItemCaptor.getAllValues());
    }


    @Test
    public void testImportAssetsToExistingRootFolder() throws Exception {
        ingestor.jcrBasePath = "/content/dam/test";
        ingestor.init();
        context.create().resource("/content/dam/test", "jcr:primaryType", "sling:Folder", "jcr:title", "testTitle");
        s3Client.putObject(TEST_BUCKET, "image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        when(assetManager.createAsset(anyString(), any(), anyString(), any(Boolean.class))).thenReturn(createdAsset);

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());

        assertEquals("testTitle", context.resourceResolver().getResource("/content/dam/test").getValueMap().get("jcr:title"));
        assertEquals(1, ingestor.getCount(ingestor.importedAssets));
        assertEquals(0, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE, (long) ingestor.importedData.get(ReportColumns.bytes));
        verify(assetManager, times(1)).createAsset(assetPathCaptor.capture(), any(), any(), eq(false));
        assertEquals("/content/dam/test/image.png", assetPathCaptor.getValue());

        verify(actionManager, times(2)).setCurrentItem(currentItemCaptor.capture());
        assertEquals(Arrays.asList("testbucket", "testbucket:image.png"), currentItemCaptor.getAllValues());
    }

    @Test
    public void testImportAssetsWithBasePath() throws Exception {
        ingestor.s3BasePath = "folder2/";
        ingestor.init();

        s3Client.putObject(TEST_BUCKET, "image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder1/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder1/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/folder3/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/folder3/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        when(assetManager.createAsset(anyString(), any(), anyString(), any(Boolean.class))).thenReturn(createdAsset);

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(1, ingestor.getCount(ingestor.importedAssets));
        assertEquals(1, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE, (long) ingestor.importedData.get(ReportColumns.bytes));
        verify(assetManager, times(1)).createAsset(assetPathCaptor.capture(), any(), any(), eq(false));
        assertEquals("/content/dam/folder3/image.png", assetPathCaptor.getValue());

        verify(actionManager, times(2)).setCurrentItem(currentItemCaptor.capture());
        assertEquals(Arrays.asList("testbucket:folder2/", "testbucket:folder2/folder3/image.png"), currentItemCaptor.getAllValues());
    }

    @Test
    public void testCreateFolders() throws Exception {
        ingestor.init();
        s3Client.putObject(TEST_BUCKET, "image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder1/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder1/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/folder3/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder2/folder3/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());

        ingestor.createFolders(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(3, ingestor.getCount(ingestor.createdFolders));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder1"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder2"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder2/folder3"));

        verify(actionManager, times(4)).setCurrentItem(currentItemCaptor.capture());
        assertEquals(Arrays.asList("testbucket", "testbucket:folder1/", "testbucket:folder2/", "testbucket:folder2/folder3/"), currentItemCaptor.getAllValues());
    }

    @Test
    public void testCreateFoldersWithBasePath() throws Exception {
        ingestor.s3BasePath = "a/";
        ingestor.init();
        s3Client.putObject(TEST_BUCKET, "image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "a/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "a/folder1/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "a/folder1/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "a/folder2/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "a/folder2/folder3/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "a/folder2/folder3/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "b/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());

        ingestor.createFolders(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(3, ingestor.getCount(ingestor.createdFolders));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder1"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder2"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder2/folder3"));

        verify(actionManager, times(4)).setCurrentItem(currentItemCaptor.capture());
        assertEquals(Arrays.asList("testbucket:a/", "testbucket:a/folder1/", "testbucket:a/folder2/", "testbucket:a/folder2/folder3/"), currentItemCaptor.getAllValues());
    }

    @Test // issue #1476
    public void testCreateFoldersWithHyphens() throws Exception {
        ingestor.init();
        s3Client.putObject(TEST_BUCKET, "image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder-with-hyphens-after-16chars/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder-with-hyphens-after-16chars/nested-folder-with-hyphens-after-16chars/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder-with-hyphens-after-16chars/nested-folder-with-hyphens-after-16chars/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder-with-hyphens-after-16chars-and-%/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder-with-hyphens-after-16chars-and-%/nested-folder-with-hyphens-after-16chars/", new ByteArrayInputStream(new byte[0]), new ObjectMetadata());
        s3Client.putObject(TEST_BUCKET, "folder-with-hyphens-after-16chars-and-%/nested-folder-with-hyphens-after-16chars/image.png", getClass().getResourceAsStream("/img/test.png"), new ObjectMetadata());

        ingestor.createFolders(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(4, ingestor.getCount(ingestor.createdFolders));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder-with-hyphens-after-16chars"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder-with-hyphens-after-16chars/nested-folder-with-hyphens-after-16chars"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder-with-hyphensafter16charsand"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder-with-hyphensafter16charsand/nested-folder-with-hyphens-after-16chars"));
    }

}
