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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.impl.JcrValidNameDefinitionBuilderImpl;
import com.adobe.acs.commons.util.datadefinitions.impl.LowercaseWithDashesDefinitionBuilderImpl;
import com.adobe.acs.commons.util.datadefinitions.impl.TitleAndNodeNameDefinitionBuilderImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@RunWith(MockitoJUnitRunner.class)
public class TagCreatorTest {

    @Rule
    public AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    private ActionManager actionManager;

    private final Map<String, ResourceDefinitionBuilder> resourceDefinitionBuilders = new HashMap<>();

    private TagCreator tagCreator;

    @Before
    public void setUp() throws Exception {
        resourceDefinitionBuilders.put(AssetFolderCreator.AssetFolderBuilder.LOWERCASE_WITH_DASHES.name(), new LowercaseWithDashesDefinitionBuilderImpl());
        resourceDefinitionBuilders.put(AssetFolderCreator.AssetFolderBuilder.TITLE_AND_NODE_NAME.name(), new TitleAndNodeNameDefinitionBuilderImpl());
        resourceDefinitionBuilders.put(AssetFolderCreator.AssetFolderBuilder.TITLE_TO_NODE_NAME.name(), new JcrValidNameDefinitionBuilderImpl());

        tagCreator = new TagCreator(resourceDefinitionBuilders);
        tagCreator.excelFile = getClass().getResourceAsStream("/com/adobe/acs/commons/mcp/impl/processes/tag-creator.xlsx");

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
                method.accept(ctx.resourceResolver());
                return null;
            }
        }).when(actionManager).withResolver(any(CheckedConsumer.class));
    }

    @Test
    public void testParseAssetFolderDefinitions() throws Exception {
        tagCreator.parseTags(actionManager);

        final int expected = 6;

        assertEquals(expected, tagCreator.tagDefinitions.size());
    }

    @Test
    public void testCreateAssetFolders() throws Exception {
        final String rootPath = "/content/cq:tags";

        ctx.create().resource(rootPath, JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        ctx.resourceResolver().commit();

        tagCreator.primary = TagCreator.TagBuilder.TITLE_AND_NODE_NAME;
        tagCreator.fallback = TagCreator.TagBuilder.LOWERCASE_WITH_DASHES;

        tagCreator.parseTags(actionManager);
        tagCreator.importTags(actionManager);

        assertTrue(ctx.resourceResolver().hasChanges());

        assertEquals("Tag Namespace 1",
                ctx.resourceResolver().getResource(rootPath + "/ns1").getValueMap().get("jcr:title", String.class));

        assertEquals("Tag 1",
                ctx.resourceResolver().getResource(rootPath + "/ns1/tag_1").getValueMap().get("jcr:title", String.class));

        assertEquals("Tag 2",
                ctx.resourceResolver().getResource(rootPath + "/ns1/tag-2").getValueMap().get("jcr:title", String.class));


        assertEquals("Tag Namespace 2",
                ctx.resourceResolver().getResource(rootPath + "/ns2").getValueMap().get("jcr:title", String.class));

        assertEquals("Tag 3",
                ctx.resourceResolver().getResource(rootPath + "/ns2/tag_3").getValueMap().get("jcr:title", String.class));

        assertEquals("Tag 4",
                ctx.resourceResolver().getResource(rootPath + "/ns2/tag-4").getValueMap().get("jcr:title", String.class));

    }
}