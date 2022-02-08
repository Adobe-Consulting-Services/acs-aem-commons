/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.data.CompositeVariant;
import com.adobe.acs.commons.data.Spreadsheet;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;

/**
 * Provide code coverage for URL Asset Import
 */
@RunWith(MockitoJUnitRunner.class)
public class UrlAssetImportTest {

    private static List<String> CASE_INSENSITIVE_HEADERS = Arrays.asList("Source", "Rendition", "Target",
                                                                         "Original");

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Mock
    private ActionManager actionManager;

    @Mock
    private AssetManager assetManager;

    private UrlAssetImport importProcess = null;

    @Before
    public void setUp() throws PersistenceException {
        context.registerAdapter(ResourceResolver.class, AssetManager.class, new Function<ResourceResolver, AssetManager>() {
            @Nullable
            @Override
            public AssetManager apply(@Nullable ResourceResolver input) {
                return assetManager;
            }
        });
        
        context.registerAdapter(Resource.class, Asset.class, new Function<Resource, Asset>() {
            @Nullable
            @Override
            public Asset apply(@Nullable Resource input) {
                return mock(Asset.class);
            }
        });        

        context.create().resource("/content/dam", JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        context.resourceResolver().commit();
        doAnswer(invocation -> {
            String path = (String) invocation.getArguments()[0];
            context.create().resource(path, JcrConstants.JCR_PRIMARYTYPE, "dam:Asset");
            context.create().resource(path + "/jcr:content", JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured");
            context.create().resource(path + "/jcr:content/metadata", JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured");
            return mock(Asset.class);
        }).when(assetManager).createAsset(any(String.class), any(InputStream.class), any(String.class), any(Boolean.class));

        importProcess = new UrlAssetImport(context.getService(MimeTypeService.class), null);
        importProcess.fileData = new Spreadsheet(true, "source", "target", "rendition", "original","dc:title", "dc:attr");
        importProcess.dryRunMode = false;

        doAnswer(invocation -> {
            CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
            method.accept(context.resourceResolver());
            return null;
        }).when(actionManager).deferredWithResolver(any(CheckedConsumer.class));
        Actions.setCurrentActionManager(actionManager);
    }

    private void addImportRow(String... cols) {
        List<String> header = importProcess.fileData.getHeaderRow();
        Map<String, CompositeVariant> row = new HashMap<>();
        for (int i = 0; i < cols.length && i < header.size(); i++) {
            row.put(header.get(i), new CompositeVariant(cols[i]));
        }
        importProcess.fileData.appendData(Collections.singletonList(row));
    }

    @Test
    public void testImportFile() throws IOException, RepositoryException {
        importProcess.init();
        URL testImg = getClass().getResource("/img/test.png");
        addImportRow(testImg.toString(), "/content/dam/test");
        addImportRow(testImg.toString(), "/content/dam/test", "rendition", "test.png");
        importProcess.files = importProcess.extractFilesAndFolders(importProcess.fileData.getDataRowsAsCompositeVariants());
        importProcess.createFolders(actionManager);
        importProcess.importAssets(actionManager);
        importProcess.updateMetadata(actionManager);
        importProcess.importRenditions(actionManager);
        assertEquals(1, importProcess.getCount(importProcess.importedAssets));
        assertEquals(1, importProcess.getCount(importProcess.createdFolders));
    }

    @Test
    public void testFolderTitlePreserve() throws IOException, RepositoryException {
        context.load().json("/com/adobe/acs/commons/mcp/impl/processes/asset-ingestor.json", "/content/dam/testfolder");
        importProcess.init();
        importProcess.preserveFolderTitles = true;
        URL testImg = getClass().getResource("/img/test.png");
        addImportRow(testImg.toString(), "/content/dam/testfolder/test");
        addImportRow(testImg.toString(), "/content/dam/testfolder/test", "rendition", "test.png");
        importProcess.files = importProcess.extractFilesAndFolders(importProcess.fileData.getDataRowsAsCompositeVariants());
        importProcess.createFolders(actionManager);
        assertEquals(1, importProcess.getCount(importProcess.createdFolders));
        context.currentResource("/content/dam/testfolder/jcr:content");
        ValueMap vm = context.currentResource().getValueMap();
        assertEquals("Test Folder", vm.get("jcr:title"));
    }

    @Test
    public void testFolderNoTitlePreserve() throws IOException, RepositoryException {
        context.load().json("/com/adobe/acs/commons/mcp/impl/processes/asset-ingestor.json", "/content/dam/testfolder");
        importProcess.init();
        importProcess.preserveFolderTitles = false;
        URL testImg = getClass().getResource("/img/test.png");
        addImportRow(testImg.toString(), "/content/dam/testfolder/test");
        addImportRow(testImg.toString(), "/content/dam/testfolder/test", "rendition", "test.png");
        importProcess.files = importProcess.extractFilesAndFolders(importProcess.fileData.getDataRowsAsCompositeVariants());
        importProcess.createFolders(actionManager);
        assertEquals(1, importProcess.getCount(importProcess.createdFolders));
        context.currentResource("/content/dam/testfolder/jcr:content");
        ValueMap vm = context.currentResource().getValueMap();
        assertEquals("testfolder", vm.get("jcr:title"));
    }

    @Test
    public void testImportFile404() throws IOException, RepositoryException {
        importProcess.init();
        URL testImg = getClass().getResource("/img/test.png");
        addImportRow(testImg.toString(), "/content/dam/test");
        addImportRow(testImg.toString(), "/content/dam/test", "rendition", "test.png");
        addImportRow(testImg.toString() + "-404", "/content/dam/other", "rendition", "no-original-found");
        importProcess.files = importProcess.extractFilesAndFolders(importProcess.fileData.getDataRowsAsCompositeVariants());
        importProcess.createFolders(actionManager);
        assertEquals(2, importProcess.getCount(importProcess.createdFolders));
        importProcess.importAssets(actionManager);
        importProcess.updateMetadata(actionManager);
        importProcess.importRenditions(actionManager);
    }

    @Test
    public void testAddedCamelCaseProperties() throws IOException, RepositoryException {
        importProcess.fileData = new Spreadsheet(true, CASE_INSENSITIVE_HEADERS,
                                                 "source", "target", "rendition", "original", "dc:title", "test:camelCase");
        importProcess.init();
        URL testImg = getClass().getResource("/img/test.png");
        final String expectedTitle = "title";
        final String expectedCamelCaseProp = "come test value";
        addImportRow(testImg.toString(), "/content/dam/test", "", "", expectedTitle, expectedCamelCaseProp);
        importProcess.files = importProcess.extractFilesAndFolders(
                importProcess.fileData.getDataRowsAsCompositeVariants());
        importProcess.createFolders(actionManager);
        importProcess.importAssets(actionManager);
        importProcess.updateMetadata(actionManager);

        Resource metadata = context.resourceResolver().getResource("/content/dam/test/test.png/jcr:content/metadata");
        ValueMap valueMap = metadata.getValueMap();
        assertEquals(expectedTitle, valueMap.get("dc:title"));
        assertEquals(expectedCamelCaseProp, valueMap.get("test:camelCase"));
    }
}
