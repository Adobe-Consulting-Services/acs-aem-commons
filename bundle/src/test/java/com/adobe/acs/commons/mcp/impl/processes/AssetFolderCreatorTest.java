/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.impl.JcrValidNameDefinitionBuilderImpl;
import com.adobe.acs.commons.util.datadefinitions.impl.LowercaseWithDashesDefinitionBuilderImpl;
import com.adobe.acs.commons.util.datadefinitions.impl.TitleAndNodeNameDefinitionBuilderImpl;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@RunWith(MockitoJUnitRunner.class)
public class AssetFolderCreatorTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Mock
    private ActionManager actionManager;

    @Captor
    private ArgumentCaptor<String> currentItemCaptor;

    @Captor
    private ArgumentCaptor<String> assetPathCaptor;

    private AssetFolderCreator assetFolderCreator;

    @Before
    public void setUp() throws Exception {
        context.create().resource("/content/dam", JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        context.resourceResolver().commit();

        final Map<String, ResourceDefinitionBuilder> resourceDefinitionBuilders = new HashMap<>();
        resourceDefinitionBuilders.put(AssetFolderCreator.AssetFolderBuilder.LOWERCASE_WITH_DASHES.name(), new LowercaseWithDashesDefinitionBuilderImpl());
        resourceDefinitionBuilders.put(AssetFolderCreator.AssetFolderBuilder.TITLE_AND_NODE_NAME.name(), new TitleAndNodeNameDefinitionBuilderImpl());
        resourceDefinitionBuilders.put(AssetFolderCreator.AssetFolderBuilder.TITLE_TO_NODE_NAME.name(), new JcrValidNameDefinitionBuilderImpl());

        assetFolderCreator = new AssetFolderCreator(resourceDefinitionBuilders);
        assetFolderCreator.excelFile = getClass().getResourceAsStream("/com/adobe/acs/commons/mcp/impl/processes/asset-folder-creator.xlsx");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
                method.accept(context.resourceResolver());
                return null;
            }
        }).when(actionManager).withResolver(any(CheckedConsumer.class));
    }

    @Test
    public void parseAssetFolderDefinitions() throws Exception {
        assetFolderCreator.parseAssetFolderDefinitions(actionManager);
        final int expected = 9 // Col 3
                + 2 // Col 2
                + 2 // Col 1
                + 3; // Numeric folders 2019/9/16

        assertEquals(expected, assetFolderCreator.assetFolderDefinitions.size());
    }

    @Test
    public void createAssetFolders() throws Exception {
        assetFolderCreator.primary = AssetFolderCreator.AssetFolderBuilder.TITLE_AND_NODE_NAME;
        assetFolderCreator.fallback = AssetFolderCreator.AssetFolderBuilder.LOWERCASE_WITH_DASHES;

        assetFolderCreator.parseAssetFolderDefinitions(actionManager);
        assetFolderCreator.createAssetFolders(actionManager);

        assertTrue(context.resourceResolver().hasChanges());

        assertEquals("Michigan",
                context.resourceResolver().getResource("/content/dam/mi/jcr:content").getValueMap().get("jcr:title", String.class));

        assertEquals("Charlestown",
                context.resourceResolver().getResource("/content/dam/ma/boston/charlestown/jcr:content").getValueMap().get("jcr:title", String.class));

        assertEquals("West Michigan",
                context.resourceResolver().getResource("/content/dam/mi/west-mi/jcr:content").getValueMap().get("jcr:title", String.class));

        assertEquals("16",
                context.resourceResolver().getResource("/content/dam/2019/9/16/jcr:content").getValueMap().get("jcr:title", String.class));

    }

    @Test
    public void updateTitle() throws Exception {
        context.create().resource("/content/dam/ma", JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        context.create().resource("/content/dam/ma/jcr:content", JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured", "jcr:title", "Mass");

        assetFolderCreator.primary = AssetFolderCreator.AssetFolderBuilder.TITLE_AND_NODE_NAME;
        assetFolderCreator.fallback = AssetFolderCreator.AssetFolderBuilder.LOWERCASE_WITH_DASHES;

        assetFolderCreator.parseAssetFolderDefinitions(actionManager);
        assetFolderCreator.createAssetFolders(actionManager);

        assertTrue(context.resourceResolver().hasChanges());

        assertEquals("Massachusetts",
                context.resourceResolver().getResource("/content/dam/ma/jcr:content").getValueMap().get("jcr:title", String.class));
    }
}