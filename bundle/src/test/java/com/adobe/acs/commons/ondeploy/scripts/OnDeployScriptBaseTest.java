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
package com.adobe.acs.commons.ondeploy.scripts;

import com.adobe.acs.commons.testutil.LogTester;
import com.day.cq.commons.jcr.JcrConstants;
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;
import java.util.Collections;

import static com.adobe.acs.commons.testutil.LogTester.assertLogText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class OnDeployScriptBaseTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private ResourceResolver resourceResolver;
    private QueryBuilder queryBuilder;
    private OnDeployScriptBase onDeployScript;

    @Before
    public void setup() throws Exception {
        resourceResolver = spy(context.resourceResolver());

        Session session = resourceResolver.adaptTo(Session.class);
        Node nodeContent = session.getRootNode().addNode("content", "nt:resource");
        nodeContent.addNode("to-delete");
        Node nodeContentResType1 = nodeContent.addNode("resource-type-update1");
        nodeContentResType1.setProperty("sling:resourceType", "test/component/comp1");
        nodeContentResType1.setProperty("text", "hello world");
        Node nodeContentResType2 = nodeContent.addNode("resource-type-update2");
        nodeContentResType2.setProperty("sling:resourceType", "test/component/comp2");
        nodeContent.addNode("resource-type-missing");

        // Create the test class instance
        onDeployScript = new OnDeployScriptBaseExt();
        queryBuilder = mock(QueryBuilder.class);
        doReturn(queryBuilder).when(resourceResolver).adaptTo(QueryBuilder.class);
        onDeployScript.execute(resourceResolver);

        // Reset the LogTester
        LogTester.reset();
    }

    @Test
    public void testExecuteException() throws Exception {
        Exception originalException = new Exception("Oops it broke");

        onDeployScript = spy(onDeployScript);
        doThrow(originalException).when(onDeployScript).execute();

        try {
            onDeployScript.execute(resourceResolver);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(OnDeployScriptException.class.isAssignableFrom(e.getClass()));
            assertEquals("On-deploy script failure", e.getMessage());
            assertEquals(originalException, e.getCause());
        }
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

        Node nodeAgain = onDeployScript.getOrCreateNode("/content/test2/folder/page", "nt:folder", "cq:Page");
        assertNotNull(nodeAgain);
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
    public void testRenameProperty() throws RepositoryException {
        final Resource resource = resourceResolver.getResource("/content/resource-type-update1");
        final Node node = resource.adaptTo(Node.class);

        assertTrue(resource.getValueMap().containsKey("text"));
        assertFalse(resource.getValueMap().containsKey("label"));
        assertEquals("hello world", resource.getValueMap().get("text", String.class));

        onDeployScript.renameProperty(node, "text", "label");

        assertFalse(resource.getValueMap().containsKey("text"));
        assertTrue(resource.getValueMap().containsKey("label"));
        assertEquals("hello world", resource.getValueMap().get("label", String.class));
    }

    @Test
    public void testRenamePropertyWhenPropertyDoesNotExist() throws RepositoryException {
        final Resource resource = resourceResolver.getResource("/content/resource-type-update1");
        final Node node = resource.adaptTo(Node.class);

        onDeployScript.renameProperty(node, "bogus", "label");

        assertLogText("Property 'bogus' does not exist on resource: /content/resource-type-update1");
    }

    @Test
    public void testRemoveResourceWhenDoesNotExist() throws RepositoryException {
        final Resource resourceToDelete = resourceResolver.getResource("/content/bogus");
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
        when(queryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).then(invocation -> {
            PredicateGroup pg = invocation.getArgument(0);
            assertEquals("-1", pg.getParameters().get("limit"));
            assertEquals("path", pg.get(0).getType());
            assertEquals("/content", pg.get(0).getParameters().get("path"));
            assertEquals("property", pg.get(1).getType());
            assertEquals("sling:resourceType", pg.get(1).getParameters().get("property"));
            assertEquals("mysite/type/old", pg.get(1).getParameters().get("value"));
            return query;
        });

        onDeployScript.searchAndUpdateResourceType("mysite/type/old", "mysite/type/new");

        assertEquals("mysite/type/new", node1.getProperty("sling:resourceType").getString());
        assertEquals("mysite/type/new", node2.getProperty("sling:resourceType").getString());
    }

    @Test
    public void testSearchAndUpdateResourceTypeWhenNoNodesFound() throws RepositoryException {
        Query query = mock(Query.class);
        SearchResult result = mock(SearchResult.class);
        when(result.getNodes()).thenReturn(Collections.EMPTY_LIST.iterator());
        when(query.getResult()).thenReturn(result);
        when(queryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).thenReturn(query);

        onDeployScript.searchAndUpdateResourceType("mysite/type/old", "mysite/type/new");

        assertLogText("No nodes found with resource type: mysite/type/old");
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
    public void testUpdateResourceTypeWhenTypeMissing() throws RepositoryException {
        Resource resourceToUpdate = resourceResolver.getResource("/content/resource-type-missing");
        assertEquals(JcrConstants.NT_UNSTRUCTURED, resourceToUpdate.getResourceType());

        onDeployScript.updateResourceType(resourceToUpdate.adaptTo(Node.class), "test/component/comp2");

        resourceToUpdate = resourceResolver.getResource("/content/resource-type-missing");
        assertEquals("test/component/comp2", resourceToUpdate.getResourceType());
    }

    @Test
    public void testUpdateResourceTypeWhenAlreadyUpdated() throws RepositoryException {
        Resource resourceToUpdate = resourceResolver.getResource("/content/resource-type-update2");
        assertEquals("test/component/comp2", resourceToUpdate.getResourceType());

        onDeployScript.updateResourceType(resourceToUpdate.adaptTo(Node.class), "test/component/comp2");

        assertLogText("Node at /content/resource-type-update2 is already resource type: test/component/comp2");
    }

    @Test
    public void testGetters() {
        assertNotNull("getResourceResolver()", onDeployScript.getResourceResolver());
        assertNotNull("getPageManager()", onDeployScript.getPageManager());
        assertNotNull("getSession()", onDeployScript.getSession());
        assertNotNull("getWorkspace()", onDeployScript.getWorkspace());
    }

    protected class OnDeployScriptBaseExt extends OnDeployScriptBase {
        @Override
        protected void execute() throws Exception {
            // no op
        }
    }
}
