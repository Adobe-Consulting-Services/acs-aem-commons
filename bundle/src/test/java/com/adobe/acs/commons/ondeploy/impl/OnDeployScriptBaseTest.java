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
package com.adobe.acs.commons.ondeploy.impl;

import com.adobe.acs.commons.testutil.LogTester;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;

import static com.adobe.acs.commons.testutil.LogTester.assertLogText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OnDeployScriptBaseTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private ResourceResolver resourceResolver;
    private QueryBuilder queryBuilder;
    private OnDeployScriptBase onDeployScript;

    @Before
    public void setup() throws Exception {
        resourceResolver = context.resourceResolver();

        Session session = resourceResolver.adaptTo(Session.class);
        Node nodeContent = session.getRootNode().addNode("content", "nt:resource");
        nodeContent.addNode("to-delete");
        Node nodeContentResType1 = nodeContent.addNode("resource-type-update1");
        nodeContentResType1.setProperty("sling:resourceType", "test/component/comp1");
        Node nodeContentResType2 = nodeContent.addNode("resource-type-update2");
        nodeContentResType2.setProperty("sling:resourceType", "test/component/comp2");

        Node nodeEtc = session.getRootNode().addNode("etc", "sling:Folder");
        nodeEtc.addNode("blueprints", "sling:Folder");
        Node nodeEtcRepl = nodeEtc.addNode("replication");
        Node nodeEtcReplAgentsAuthor = nodeEtcRepl.addNode("agents.author");
        Node nodeEtcReplAgentsAuthorPublish = nodeEtcReplAgentsAuthor.addNode("publish").addNode("jcr:content");
        nodeEtcReplAgentsAuthorPublish.setProperty("transportUri", "http://localhost:4503/bin/receive?sling:authRequestLogin=1");
        Node nodeEtcReplAgentsAuthorFlush = nodeEtcReplAgentsAuthor.addNode("flush").addNode("jcr:content");
        nodeEtcReplAgentsAuthorFlush.setProperty("transportUri", "http://localhost:8000/dispatcher/invalidate.cache");
        Node nodeEtcReplAgentsPublish = nodeEtcRepl.addNode("agents.publish");
        Node nodeEtcReplAgentsPublishFlush = nodeEtcReplAgentsPublish.addNode("flush").addNode("jcr:content");
        nodeEtcReplAgentsPublishFlush.setProperty("transportUri", "http://localhost:8000/dispatcher/invalidate.cache");

        // Create the test class instance
        onDeployScript = new OnDeployScriptBaseExt();
        queryBuilder = mock(QueryBuilder.class);
        onDeployScript.execute(resourceResolver, queryBuilder);

        // Reset the LogTester
        LogTester.reset();
    }

    @Test
    public void testGetOrCreateNode() throws RepositoryException {
        Node node = onDeployScript.getOrCreateNode("/content/test1/folder/page");

        assertNotNull(node);
        assertEquals("/content/test1/folder/page", node.getPath());
        assertEquals("nt:unstructured", node.getPrimaryNodeType().getName());

        Node parent = node.getParent();
        assertEquals("/content/test1/folder", parent.getPath());
        assertEquals("nt:unstructured", parent.getPrimaryNodeType().getName());

        Node grandParent = parent.getParent();
        assertEquals("/content/test1", grandParent.getPath());
        assertEquals("nt:unstructured", grandParent.getPrimaryNodeType().getName());

        Node base = grandParent.getParent();
        assertEquals("/content", base.getPath());
        assertEquals("nt:resource", base.getPrimaryNodeType().getName());
    }

    @Test
    public void testGetOrCreateNodeWithAllTypesSpecified() throws RepositoryException {
        Node node = onDeployScript.getOrCreateNode("/content/test2/folder/page", "nt:folder", "cq:Page");

        assertNotNull(node);
        assertEquals("/content/test2/folder/page", node.getPath());
        assertEquals("cq:Page", node.getPrimaryNodeType().getName());

        Node parent = node.getParent();
        assertEquals("/content/test2/folder", parent.getPath());
        assertEquals("nt:folder", parent.getPrimaryNodeType().getName());

        Node grandParent = parent.getParent();
        assertEquals("/content/test2", grandParent.getPath());
        assertEquals("nt:folder", grandParent.getPrimaryNodeType().getName());

        Node base = grandParent.getParent();
        assertEquals("/content", base.getPath());
        assertEquals("nt:resource", base.getPrimaryNodeType().getName());
    }

    @Test
    public void testGetOrCreateNodeWithNodeTypeSpecified() throws RepositoryException {
        Node node = onDeployScript.getOrCreateNode("/content/test3/folder/page", "cq:Page");

        assertNotNull(node);
        assertEquals("/content/test3/folder/page", node.getPath());
        assertEquals("cq:Page", node.getPrimaryNodeType().getName());

        Node parent = node.getParent();
        assertEquals("/content/test3/folder", parent.getPath());
        assertEquals("nt:unstructured", parent.getPrimaryNodeType().getName());

        Node grandParent = parent.getParent();
        assertEquals("/content/test3", grandParent.getPath());
        assertEquals("nt:unstructured", grandParent.getPrimaryNodeType().getName());

        Node base = grandParent.getParent();
        assertEquals("/content", base.getPath());
        assertEquals("nt:resource", base.getPrimaryNodeType().getName());
    }

    @Test
    public void testRemoveResourceWhenDoesNotExist() throws RepositoryException {
        Resource resourceToDelete = resourceResolver.getResource("/content/bogus");
        assertNull(resourceToDelete);

        onDeployScript.removeResource("/content/bogus");

        assertLogText("Node at /content/bogus has already been removed");
    }

    @Test
    public void testRemoveResourceWhenExists() throws RepositoryException {
        Resource resourceToDelete = resourceResolver.getResource("/content/to-delete");
        assertNotNull(resourceToDelete);

        onDeployScript.removeResource("/content/to-delete");

        resourceToDelete = resourceResolver.getResource("/content/to-delete");
        assertNull(resourceToDelete);
    }

    @Test
    public void testSearchAndUpdateResourceType() throws RepositoryException {
        Node contentRoot = resourceResolver.getResource("/content").adaptTo(Node.class);
        Node node1 = contentRoot.addNode("search-and-update-node1");
        node1.setProperty("sling:resourceType", "mysite/type/old");
        Node node2 = contentRoot.addNode("search-and-update-node2");
        node2.setProperty("sling:resourceType", "mysite/type/old");

        final Query query = mock(Query.class);
        SearchResult result = mock(SearchResult.class);
        when(result.getNodes()).thenReturn(Arrays.asList(node1, node2).iterator());
        when(query.getResult()).thenReturn(result);
        when(queryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).then(new Answer<Query>() {
            @Override
            public Query answer(InvocationOnMock invocationOnMock) throws Throwable {
                PredicateGroup pg = invocationOnMock.getArgumentAt(0, PredicateGroup.class);
                assertEquals("-1", pg.getParameters().get("limit"));
                assertEquals("path", pg.get(0).getType());
                assertEquals("/content", pg.get(0).getParameters().get("path"));
                assertEquals("property", pg.get(1).getType());
                assertEquals("sling:resourceType", pg.get(1).getParameters().get("property"));
                assertEquals("mysite/type/old", pg.get(1).getParameters().get("value"));
                return query;
            }
        });

        onDeployScript.searchAndUpdateResourceType("mysite/type/old", "mysite/type/new");

        assertEquals("mysite/type/new", node1.getProperty("sling:resourceType").getString());
        assertEquals("mysite/type/new", node2.getProperty("sling:resourceType").getString());
    }

    @Test
    public void testUpdateResourceType() throws RepositoryException {
        Resource resourceToUpdate = resourceResolver.getResource("/content/resource-type-update1");
        assertEquals("test/component/comp1", resourceToUpdate.getResourceType());

        onDeployScript.updateResourceType(resourceToUpdate.adaptTo(Node.class), "test/component/comp2");

        resourceToUpdate = resourceResolver.getResource("/content/resource-type-update1");
        assertEquals("test/component/comp2", resourceToUpdate.getResourceType());
    }

    @Test
    public void testUpdateResourceTypeWhenAlreadyUpdated() throws RepositoryException {
        Resource resourceToUpdate = resourceResolver.getResource("/content/resource-type-update2");
        assertEquals("test/component/comp2", resourceToUpdate.getResourceType());

        onDeployScript.updateResourceType(resourceToUpdate.adaptTo(Node.class), "test/component/comp2");

        assertLogText("Node at /content/resource-type-update2 is already resource type 'test/component/comp2'");
    }

    private class OnDeployScriptBaseExt extends OnDeployScriptBase {
        @Override
        protected void execute() throws Exception {
            // no op
        }
    }
}
