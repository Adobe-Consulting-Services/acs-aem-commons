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

import com.adobe.acs.commons.mcp.impl.processes.asset.FileAssetIngestor;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.day.cq.dam.api.AssetManager;
import com.google.common.base.Function;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

@RunWith(MockitoJUnitRunner.class)
public class FileAssetIngestorTest {
    private static final int FILE_SIZE = 57797;

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Mock
    private ActionManager actionManager;

    @Mock
    private AssetManager assetManager;

    @Captor
    private ArgumentCaptor<String> currentItemCaptor;

    @Captor
    private ArgumentCaptor<String> assetPathCaptor;

    private FileAssetIngestor ingestor;

    private File tempDirectory;

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
        ingestor = new FileAssetIngestor(context.getService(MimeTypeService.class));
        ingestor.jcrBasePath = "/content/dam";
        ingestor.ignoreFileList = Collections.emptyList();
        ingestor.ignoreExtensionList = Collections.emptyList();
        ingestor.ignoreFolderList = Arrays.asList(".ds_store");
        ingestor.existingAssetAction = AssetIngestor.AssetAction.skip;

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
                method.accept(context.resourceResolver());
                return null;
            }
        }).when(actionManager).deferredWithResolver(any(CheckedConsumer.class));
        tempDirectory = Files.createTempDir();
        ingestor.fileBasePath = tempDirectory.getAbsolutePath();
    }

    @After
    public void teardown() throws Exception {
        try {
            FileUtils.deleteDirectory(tempDirectory);
        } catch (IOException ex) {
            // Ideally the files are cleaned up but some Redmond OS's don't readily release locks right away, causing errors we can ignore.
        }
    }

    @Test
    public void testCreateFoldersWithEmptyDirectory() throws Exception {
        ingestor.init();
        ingestor.createFolders(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertFalse(context.resourceResolver().getResource("/content/dam").hasChildren());

        verify(actionManager).setCurrentItem(currentItemCaptor.capture());
        assertEquals(1, currentItemCaptor.getAllValues().size());
        assertEquals(tempDirectory.getAbsolutePath(), currentItemCaptor.getValue());

    }

    @Test
    public void testCreateFolders() throws Exception {
        ingestor.init();
        addFile(tempDirectory, "image.png", "/img/test.png");
        File folder1 = mkdir(tempDirectory, "folder1");
        addFile(folder1, "image.png", "/img/test.png");
        File folder2 = mkdir(tempDirectory, "folder2");
        File folder3 = mkdir(folder2, "folder3");
        addFile(folder3, "image.png", "/img/test.png");

        java.nio.file.Files.walk(tempDirectory.toPath()).forEach(p -> System.out.println(p.toString()));

        ingestor.createFolders(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(3, ingestor.getCount(ingestor.createdFolders));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder1"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder2"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder2/folder3"));

        verify(actionManager, times(4)).setCurrentItem(currentItemCaptor.capture());
        assertThat(currentItemCaptor.getAllValues(),
                containsInAnyOrder(tempDirectory.getAbsolutePath(), folder1.getAbsolutePath(), folder2.getAbsolutePath(), folder3.getAbsolutePath()));
    }


    @Test
    public void testImportAssetsWithEmptyDirectory() throws Exception {
        ingestor.init();
        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertFalse(context.resourceResolver().getResource("/content/dam").hasChildren());

        verify(actionManager).setCurrentItem(currentItemCaptor.capture());
        assertEquals(1, currentItemCaptor.getAllValues().size());
        assertEquals(tempDirectory.getAbsolutePath(), currentItemCaptor.getValue());

    }

    @Test
    public void testImportAssetsWithDirectoryContainingJustFolders() throws Exception {
        ingestor.init();
        mkdir(tempDirectory, "folder1");
        mkdir(mkdir(tempDirectory, "folder2"), "folder3");

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertFalse(context.resourceResolver().getResource("/content/dam").hasChildren());

        verify(actionManager).setCurrentItem(currentItemCaptor.capture());
        assertEquals(1, currentItemCaptor.getAllValues().size());
        assertEquals(tempDirectory.getAbsolutePath(), currentItemCaptor.getValue());

    }

    @Test
    public void testImportAssets() throws Exception {
        ingestor.init();
        File rootImage = addFile(tempDirectory, "image.png", "/img/test.png");
        File folder1 = mkdir(tempDirectory, "folder1");
        File folder1Image = addFile(folder1, "image.png", "/img/test.png");
        File folder2 = mkdir(tempDirectory, "folder2");
        File folder3 = mkdir(folder2, "folder3");
        File folder3Image = addFile(folder3, "image.png", "/img/test.png");

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(3, ingestor.getCount(ingestor.importedAssets));
        assertEquals(3, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE * 3, ingestor.importedData.get(AssetIngestor.ReportColumns.bytes));
        verify(assetManager, times(3)).createAsset(assetPathCaptor.capture(), any(), any(), eq(false));
        assertThat(assetPathCaptor.getAllValues(),
                containsInAnyOrder("/content/dam/folder1/image.png", "/content/dam/folder2/folder3/image.png", "/content/dam/image.png"));

        verify(actionManager, times(4)).setCurrentItem(currentItemCaptor.capture());
        assertThat(currentItemCaptor.getAllValues(),
                containsInAnyOrder(tempDirectory.getAbsolutePath(), folder1Image.getAbsolutePath(), folder3Image.getAbsolutePath(), rootImage.getAbsolutePath()));
    }


    @Test
    public void testImportAssetsToNewRootFolder() throws Exception {
        ingestor.jcrBasePath = "/content/dam/test";
        ingestor.init();
        File rootImage = addFile(tempDirectory, "image.png", "/img/test.png");

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());

        assertNull(context.resourceResolver().getResource("/content/dam/test").getValueMap().get("jcr:title"));
        assertEquals(1, ingestor.getCount(ingestor.importedAssets));
        assertEquals(0, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE, ingestor.importedData.get(AssetIngestor.ReportColumns.bytes));
        verify(assetManager, times(1)).createAsset(assetPathCaptor.capture(), any(), any(), eq(false));
        assertEquals("/content/dam/test/image.png", assetPathCaptor.getValue());

        verify(actionManager, times(2)).setCurrentItem(currentItemCaptor.capture());
        assertThat(currentItemCaptor.getAllValues(),
                containsInAnyOrder(tempDirectory.getAbsolutePath(), rootImage.getAbsolutePath()));
    }


    @Test
    public void testImportAssetsToExistingRootFolder() throws Exception {
        ingestor.jcrBasePath = "/content/dam/test";
        ingestor.init();
        context.create().resource("/content/dam/test", "jcr:primaryType", "sling:Folder", "jcr:title", "testTitle");
        File rootImage = addFile(tempDirectory, "image.png", "/img/test.png");

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());

        assertEquals("testTitle", context.resourceResolver().getResource("/content/dam/test").getValueMap().get("jcr:title"));
        assertEquals(1, ingestor.getCount(ingestor.importedAssets));
        assertEquals(0, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE, ingestor.importedData.get(AssetIngestor.ReportColumns.bytes));
        verify(assetManager, times(1)).createAsset(assetPathCaptor.capture(), any(), any(), eq(false));
        assertEquals("/content/dam/test/image.png", assetPathCaptor.getValue());

        verify(actionManager, times(2)).setCurrentItem(currentItemCaptor.capture());
        assertThat(currentItemCaptor.getAllValues(),
                containsInAnyOrder(tempDirectory.getAbsolutePath(), rootImage.getAbsolutePath()));
    }

    private File mkdir(File dir, String name) {
        File newDir = new File(dir, name);
        newDir.mkdir();
        return newDir;
    }

    private File addFile(File dir, String name, String resourcePath) throws IOException {
        File newFile = new File(dir, name);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(resourcePath), newFile);
        return newFile;
    }

}
