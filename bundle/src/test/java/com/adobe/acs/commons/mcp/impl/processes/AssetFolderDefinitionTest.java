package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.impl.BasicResourceDefinition;
import com.day.cq.dam.api.Asset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AssetFolderDefinitionTest {

    ResourceDefinition basic;

    @Before
    public void setup() {
        basic = new BasicResourceDefinition("bar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getId() throws Exception {
        AssetFolderCreator.AssetFolderDefinition definition = new AssetFolderCreator.AssetFolderDefinition(basic, "/content/dam/foo", AssetFolderCreator.FolderType.UNORDERED_FOLDER);
        definition.getId();
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