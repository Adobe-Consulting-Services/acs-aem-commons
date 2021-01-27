/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.sorter;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class SortNodesOperationTest {
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Spy
    private SortNodesOperation sorter = new SortNodesOperation();

    @Before
    public void setUp(){
        context.build()
                .resource("/sortable", JCR_PRIMARYTYPE, "cq:Page")
                .resource("/sortable/page2", JCR_PRIMARYTYPE, "cq:Page")
                .resource("/sortable/page2/jcr:content", JCR_PRIMARYTYPE, "cq:PageContent", "jcr:title", "A: Page-2")
                .resource("/sortable/jcr:content", JCR_PRIMARYTYPE, "nt:unstructured")
                .resource("/sortable/page1", JCR_PRIMARYTYPE, "cq:Page")
                .resource("/sortable/Page3/jcr:content", JCR_PRIMARYTYPE, "cq:PageContent", "jcr:title", "C: Page-3")
                .resource("/sortable/Page3", JCR_PRIMARYTYPE, "cq:Page")
                .resource("/sortable/page1/jcr:content", JCR_PRIMARYTYPE, "cq:PageContent", "jcr:title", "a: Page-1")
                .resource("/sortable/rep:policy", JCR_PRIMARYTYPE, "rep:ACL");
    }

    /**
     * by node name, case insensitive
     */
    @Test
    public void testSortByNameCaseInsensitive() throws RepositoryException {
        Comparator<Node> comparator =  SortNodesOperation.createComparator(false, false);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "page1", "page2", "Page3"), list);

    }

    /**
     * by node name, case sensitive
     */
    @Test
    public void testSortByNameCaseSensitive() throws RepositoryException {
        Comparator<Node> comparator =  SortNodesOperation.createComparator(false, true);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "Page3", "page1", "page2"), list);

    }

    /**
     * by title, case insensitive
     */
    @Test
    public void testSortByTitleCaseInsensitive() throws RepositoryException {
        Comparator<Node> comparator =  SortNodesOperation.createComparator(true, false);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "page1" /*a: Page-1*/, "page2" /*A: Page-2*/, "Page3" /*C: Page-3*/), list);

    }

    /**
     * by title, case sensitive
     */
    @Test
    public void testSortByTitleCaseSensitive() throws RepositoryException {
        Comparator<Node> comparator =  SortNodesOperation.createComparator(true, true);
        Node node = context.resourceResolver().getResource("/sortable").adaptTo(Node.class);
        List<Node> list = sorter.getSortedNodes(node, comparator);

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "page2" /*A: Page-2*/, "Page3" /*C: Page-3*/, "page1" /*a: Page-1*/), list);
    }

    @Test
    public void testDefaultRequestParameters() throws RepositoryException {
        Resource resource  = context.resourceResolver().getResource("/sortable");
        MockSlingHttpServletRequest request = context.request();
        request.setResource(resource);
        sorter.run(context.request(), mock(PostResponse.class), null);

        List<Node> children = new ArrayList<>();
        resource.getChildren().forEach(r -> children.add(r.adaptTo(Node.class)));

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "page1", "page2", "Page3"), children);
    }

    @Test
    public void testRequestParameters() throws RepositoryException {
        Resource resource  = context.resourceResolver().getResource("/sortable");
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter(SortNodesOperation.RP_SORT_BY_TITLE, "true");
        request.addRequestParameter(SortNodesOperation.RP_CASE_SENSITIVE, "true");
        request.setResource(resource);
        sorter.run(context.request(), mock(PostResponse.class), null);

        List<Node> children = new ArrayList<>();
        resource.getChildren().forEach(r -> children.add(r.adaptTo(Node.class)));

        assertSortOrder(Arrays.asList("jcr:content", "rep:policy", "page2" /*A: Page-2*/, "Page3" /*C: Page-3*/, "page1" /*a: Page-1*/), children);
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
