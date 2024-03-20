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
package com.adobe.acs.commons.sorter;

import com.adobe.acs.commons.sorter.impl.NodeNameSorter;
import com.adobe.acs.commons.sorter.impl.NodeTitleSorter;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.sorter.SortNodesOperation.RP_SORTER_NAME;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SortNodesOperationTest {
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Spy
    private SortNodesOperation sorter = new SortNodesOperation();

    @Before
    public void setUp(){
        Map<String, String> sortableContent = new LinkedHashMap<>();
        sortableContent.put("aaa", "aaa"); // name -> title
        sortableContent.put("Bbb", "Bbb");
        sortableContent.put("Zorro", "Zorro");
        sortableContent.put("1", "1");
        sortableContent.put("20", "20");
        sortableContent.put("1-A", "1-A");
        sortableContent.put("200", "200");
        sortableContent.put("2-B", "2-B");
        sortableContent.put("101", "101");
        sortableContent.put("11", "11");
        sortableContent.put("22", "22");
        sortableContent.put("Two", "Two");

        sorter.bindNodeSorter(new NodeNameSorter(), Collections.emptyMap());
        sorter.bindNodeSorter(new NodeTitleSorter(), Collections.emptyMap());
        ResourceBuilder bldr = context.build()
                .resource("/sortable", JCR_PRIMARYTYPE, "cq:Page")
                .siblingsMode();
        for(String nodeName : sortableContent.keySet()){
            String title = sortableContent.get(nodeName);
            bldr.resource(nodeName + "/jcr:content", JCR_PRIMARYTYPE, "cq:PageContent", "jcr:title", title)
                    .resource(nodeName, JCR_PRIMARYTYPE, "cq:Page");
        }
         bldr.resource("rep:policy", JCR_PRIMARYTYPE, "rep:ACL")
             .resource("jcr:content", JCR_PRIMARYTYPE, "nt:unstructured");
    }


    /**
     * by node name, case insensitive
     */
    @Test
    public void testSortByNameCaseInsensitive() throws RepositoryException {
        MockSlingHttpServletRequest request = context.request();
        Comparator<Node> comparator = sorter.getNodeSorter(request);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "1", "1-A", "101", "11", "2-B", "20", "200", "22", "aaa", "Bbb", "Two", "Zorro"), list);

    }

    /**
     * don't move non-hierarchy nodes to the top, sort them along with other nodes
     */
    @Test
    public void testDisableNonHierarchyFirst() throws RepositoryException {
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter(":nonHierarchyFirst", "false");
        Comparator<Node> comparator = sorter.getNodeSorter(request);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("1", "1-A", "101", "11", "2-B", "20", "200", "22", "aaa", "Bbb", "jcr:content", "rep:policy", "Two", "Zorro"
        ), list);

    }

    /**
     * by node name, case sensitive
     */
    @Test
    public void testSortByNameCaseSensitive() throws RepositoryException {
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter(NodeTitleSorter.RP_CASE_SENSITIVE, "true");
        Comparator<Node> comparator = sorter.getNodeSorter(request);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "1", "1-A", "101", "11", "2-B", "20", "200", "22", "Bbb", "Two", "Zorro", "aaa"), list);

    }

    /**
     * by title, case insensitive
     */
    @Test
    public void testSortByTitleCaseInsensitive() throws RepositoryException {
        MockSlingHttpServletRequest request = context.request();
        Comparator<Node> comparator = sorter.getNodeSorter(request);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "1", "1-A", "101", "11", "2-B", "20", "200", "22", "aaa", "Bbb", "Two", "Zorro"), list);

    }

    /**
     * by title, case sensitive
     */
    @Test
    public void testSortByTitleCaseSensitive() throws RepositoryException {
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter(RP_SORTER_NAME, NodeTitleSorter.SORTER_NAME);
        request.addRequestParameter(NodeTitleSorter.RP_CASE_SENSITIVE, "true");
        Comparator<Node> comparator = sorter.getNodeSorter(request);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "1", "1-A", "101", "11", "2-B", "20", "200", "22", "Bbb", "Two", "Zorro", "aaa"), list);
    }

    /**
     * by name, case insensitive, numbers first followed by strings
     */
    @Test
    public void testSortByNameCaseInsensitiveRespectNumbers() throws RepositoryException {
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter(RP_SORTER_NAME, NodeTitleSorter.SORTER_NAME);
        request.addRequestParameter(NodeTitleSorter.RP_CASE_SENSITIVE, "true");
        request.addRequestParameter(NodeTitleSorter.RP_RESPECT_NUMBERS, "true");
        Comparator<Node> comparator = sorter.getNodeSorter(request);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "1", "11", "20", "22", "101", "200", "1-A", "2-B", "Bbb", "Two", "Zorro", "aaa"), list);
    }

    @Test
    public void testDefaultRequestParameters() {
        Resource resource  = context.resourceResolver().getResource("/sortable");
        MockSlingHttpServletRequest request = context.request();
        request.setResource(resource);
        sorter.run(context.request(), mock(PostResponse.class), null);

        List<Node> children = new ArrayList<>();
        resource.getChildren().forEach(r -> children.add(r.adaptTo(Node.class)));

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "1", "1-A", "101", "11", "2-B", "20", "200", "22", "aaa", "Bbb", "Two", "Zorro"), children);
    }

    @Test
    public void testInvalidTargetResource()  {
        String targetPath = "/unknown";
        PostResponse response = mock(PostResponse.class);
        MockSlingHttpServletRequest request = context.request();
        request.setResource(new NonExistingResource(context.resourceResolver(), targetPath));
        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        sorter.run(request, response, null);

        verify(response).setStatus(statusCaptor.capture(), msgCaptor.capture());
        assertEquals(HttpServletResponse.SC_NOT_FOUND, (int)statusCaptor.getValue());
        assertEquals("Missing target node to sort: " + targetPath, msgCaptor.getValue());
    }

    @Test
    public void testNotSortableTarget()  {
        String targetPath = "/invalid";
        context.build()
                .resource(targetPath, JCR_PRIMARYTYPE, "sling:Folder")
                .resource(targetPath + "/node1", JCR_PRIMARYTYPE, "sling:Folder")
                .resource(targetPath + "/node1", JCR_PRIMARYTYPE, "sling:Folder")
                .resource(targetPath + "/jcr:content", JCR_PRIMARYTYPE, "nt:unstructured");
        PostResponse response = mock(PostResponse.class);
        MockSlingHttpServletRequest request = context.request();
        request.setResource(context.resourceResolver().getResource(targetPath));
        ArgumentCaptor<Throwable> errCaptor = ArgumentCaptor.forClass(Throwable.class);
        sorter.run(request, response, null);

        verify(response).setError(errCaptor.capture());
        assertEquals("Child node ordering is not supported on this node",
                errCaptor.getValue().getMessage());
    }

    @Test
    public void testUnknownSorter()  {
        PostResponse response = mock(PostResponse.class);
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter(RP_SORTER_NAME, "sorterNA");
        request.setResource(context.resourceResolver().getResource("/sortable"));
        ArgumentCaptor<Throwable> errCaptor = ArgumentCaptor.forClass(Throwable.class);
        sorter.run(request, response, null);

        verify(response).setError(errCaptor.capture());
        assertEquals("NodeSorter was not found: sorterNA. Available sorters are: [byName, byTitle]",
                errCaptor.getValue().getMessage());
    }

    private static void assertSortOrder(List<String> expected, List<Node> list){
        List<String> actual = list.stream().map(n -> {
            try {
                return n.getName();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        assertEquals(expected, actual);

    }
 }
