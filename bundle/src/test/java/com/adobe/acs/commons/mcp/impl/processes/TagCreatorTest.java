package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.impl.JcrValidNameDefinitionBuilderImpl;
import com.adobe.acs.commons.util.datadefinitions.impl.LowercaseWithDashesDefinitionBuilderImpl;
import com.adobe.acs.commons.util.datadefinitions.impl.TitleAndNodeNameDefinitionBuilderImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
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
        final String rootPath = "/etc/tags";

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