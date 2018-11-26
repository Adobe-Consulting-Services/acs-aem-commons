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
import com.day.cq.dam.api.AssetManager;
import com.google.common.base.Function;
import com.google.common.io.Files;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

@RunWith(MockitoJUnitRunner.class)
public class FileAssetIngestorTest {
    private static final int FILE_SIZE = 57797;
    private static final String SFTP_HOST_TEST_PATH = "sftp://host/test/path";
    private static final String SFTP_USER_TEST_NAME = "user";
    private static final String SFTP_USER_TEST_PASSWORD = "password";

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
        tempDirectory = Files.createTempDir();
        ingestor = new FileAssetIngestor(context.getService(MimeTypeService.class));
        ingestor.jcrBasePath = "/content/dam";
        ingestor.ignoreFileList = Collections.emptyList();
        ingestor.ignoreExtensionList = Collections.emptyList();
        ingestor.ignoreFolderList = Arrays.asList(".ds_store");
        ingestor.existingAssetAction = AssetIngestor.AssetAction.skip;
        ingestor.fileBasePath = tempDirectory.getAbsolutePath();

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

        assertNotNull(context.resourceResolver().getResource("/content/dam"));

        ingestor.createFolders(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(3, ingestor.getCount(ingestor.createdFolders));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder1"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder2"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/folder2/folder3"));

        verify(actionManager, atLeast(4)).setCurrentItem(currentItemCaptor.capture());
        assertThat(currentItemCaptor.getAllValues(),
                containsInAnyOrder(tempDirectory.getAbsolutePath(), tempDirectory.getAbsolutePath(), folder1.getAbsolutePath(), folder2.getAbsolutePath(), folder3.getAbsolutePath()));
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
        final File rootImage = addFile(tempDirectory, "image.png", "/img/test.png");
        final File folder1 = mkdir(tempDirectory, "folder1");
        final File folder1Image = addFile(folder1, "image.png", "/img/test.png");
        final File folder2 = mkdir(tempDirectory, "folder2");
        final File folder3 = mkdir(folder2, "folder3");
        final File folder3Image = addFile(folder3, "image.png", "/img/test.png");

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());
        assertEquals(3, ingestor.getCount(ingestor.importedAssets));
        assertEquals(3, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE * 3, (long) ingestor.importedData.get(AssetIngestor.ReportColumns.bytes));
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
        final File rootImage = addFile(tempDirectory, "image.png", "/img/test.png");

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());

        assertNull(context.resourceResolver().getResource("/content/dam/test").getValueMap().get("jcr:title"));
        assertEquals(1, ingestor.getCount(ingestor.importedAssets));
        assertEquals(0, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE, (long) ingestor.importedData.get(AssetIngestor.ReportColumns.bytes));
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
        final File rootImage = addFile(tempDirectory, "image.png", "/img/test.png");

        ingestor.importAssets(actionManager);

        assertFalse(context.resourceResolver().hasChanges());

        assertEquals("testTitle", context.resourceResolver().getResource("/content/dam/test").getValueMap().get("jcr:title"));
        assertEquals(1, ingestor.getCount(ingestor.importedAssets));
        assertEquals(0, ingestor.getCount(ingestor.createdFolders));
        assertEquals(FILE_SIZE, (long) ingestor.importedData.get(AssetIngestor.ReportColumns.bytes));
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
    
    @Test
    public void testSftpStructures() throws URISyntaxException, JSchException {
        configureSftpFields();
        FileAssetIngestor.SftpHierarchicalElement elem1 = ingestor.new SftpHierarchicalElement(SFTP_HOST_TEST_PATH);
        elem1.isFile = true;
        assertNull(elem1.getParent());
        assertEquals(SFTP_HOST_TEST_PATH, elem1.getSourcePath());
        // File should be a child node
        assertEquals("/content/dam/path", elem1.getNodePath());
        assertEquals("path", elem1.getNodeName());

        FileAssetIngestor.SftpHierarchicalElement elem2 = ingestor.new SftpHierarchicalElement(SFTP_HOST_TEST_PATH);
        elem1.isFile = false;
        assertNull(elem2.getParent());
        assertEquals(SFTP_HOST_TEST_PATH, elem2.getSourcePath());
        // Folder should map to the root folder
        assertEquals("/content/dam", elem2.getNodePath());
        assertEquals("path", elem2.getNodeName());
    }

    @Test
    public void testSftpRecursion() throws URISyntaxException, JSchException, SftpException {
        configureSftpFields();
        ChannelSftp channel = mock(ChannelSftp.class);
        when(channel.isConnected()).thenReturn(true);
        when(channel.getSession()).thenReturn(mock(Session.class));
        
        Vector<ChannelSftp.LsEntry> entries = (new MockDirectoryBuilder())
                .addDirectory(".")
                .addDirectory("..")
                .addFile("file1.png", 1234L)
                .addFile("file2.png", 4567L)
                .asVector();
        when(channel.ls(anyObject())).thenReturn(entries);
        
        FileAssetIngestor.SftpHierarchicalElement elem1 = ingestor.new SftpHierarchicalElement(SFTP_HOST_TEST_PATH, channel, false);
        int count = 0;
        for (HierarchicalElement e : elem1.getChildren().collect(Collectors.toList())) {
            count++;
            assertTrue("Expected file name", e.getName().equals("file1.png") || e.getName().equals("file2.png"));
            assertTrue("Expected isFile for " + e.getName(), e.isFile());
        }
        assertEquals("Expected only two files", 2, count);
    }

    private File addFile(File dir, String name, String resourcePath) throws IOException {
        File newFile = new File(dir, name);
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(resourcePath), newFile);
        return newFile;
    }

    private void configureSftpFields() {
        ingestor.fileBasePath = SFTP_HOST_TEST_PATH;
        ingestor.username = SFTP_USER_TEST_NAME;
        ingestor.password = SFTP_USER_TEST_PASSWORD;
    }
}
