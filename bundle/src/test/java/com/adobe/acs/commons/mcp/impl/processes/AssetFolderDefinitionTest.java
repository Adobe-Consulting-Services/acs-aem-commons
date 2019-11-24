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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.impl.BasicResourceDefinition;
import com.day.cq.dam.api.Asset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AssetFolderDefinitionTest {

    ResourceDefinition basic;

    @Before
    public void setup() {
        basic = new BasicResourceDefinition("bar");
    }

    @Test
    public void getId() throws Exception {
        AssetFolderCreator.AssetFolderDefinition definition = new AssetFolderCreator.AssetFolderDefinition(basic, "/content/dam/foo", AssetFolderCreator.FolderType.UNORDERED_FOLDER);

        assertEquals("/content/dam/foo/bar", definition.getId());
    }

    @Test
    public void getParentPath() throws Exception {
        AssetFolderCreator.AssetFolderDefinition definition = new AssetFolderCreator.AssetFolderDefinition(basic, "/content/dam/foo", AssetFolderCreator.FolderType.UNORDERED_FOLDER);

        assertEquals("/content/dam/foo", definition.getParentPath());
    }

    @Test
    public void getPath() throws Exception {
        AssetFolderCreator.AssetFolderDefinition definition = new AssetFolderCreator.AssetFolderDefinition(basic, "/content/dam/foo", AssetFolderCreator.FolderType.UNORDERED_FOLDER);

        assertEquals("/content/dam/foo/bar", definition.getPath());
    }

    @Test
    public void getNodeType() throws Exception {
        AssetFolderCreator.AssetFolderDefinition ordered = new AssetFolderCreator.AssetFolderDefinition(basic, "/content/dam/foo", AssetFolderCreator.FolderType.ORDERED_FOLDER);
        AssetFolderCreator.AssetFolderDefinition unordered = new AssetFolderCreator.AssetFolderDefinition(basic, "/content/dam/foo", AssetFolderCreator.FolderType.UNORDERED_FOLDER);

        assertEquals("sling:OrderedFolder", ordered.getNodeType());
        assertEquals("sling:Folder", unordered.getNodeType());
    }
}