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

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.data.CompositeVariant;
import com.adobe.acs.commons.data.Spreadsheet;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.google.common.base.Function;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Provide code coverage for URL Asset Import
 */
@RunWith(MockitoJUnitRunner.class)
public class UrlAssetImportTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

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

        doAnswer(invocation -> {
            CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
            method.accept(context.resourceResolver());
            return null;
        }).when(actionManager).deferredWithResolver(any(CheckedConsumer.class));
    }

    private void addImportRow(String... cols) {
        List<String> header = importProcess.fileData.getHeaderRow();
        Map<String, CompositeVariant> row = new HashMap<>();
        for (int i = 0; i < cols.length && i < header.size(); i++) {
            row.put(header.get(i), new CompositeVariant(cols[i]));
        }
        importProcess.fileData.getDataRowsAsCompositeVariants().add(row);
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
}
