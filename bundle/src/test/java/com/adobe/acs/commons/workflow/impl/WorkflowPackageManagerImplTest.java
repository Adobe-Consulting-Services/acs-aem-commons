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

package com.adobe.acs.commons.workflow.impl;

import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.workflow.collection.ResourceCollection;
import com.day.cq.workflow.collection.ResourceCollectionManager;
import com.day.cq.workflow.collection.ResourceCollectionUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ResourceCollectionUtil.class, JcrUtil.class})
public class WorkflowPackageManagerImplTest {
    private static final String NORMAL_PAGE_PATH = "/content/test";
    private static final String WORKFLOW_PACKAGE_PATH = "/etc/packages/test";
    private static final String[] PAYLOAD_PATHS = {"/content/one", "/content/two"};

    @Mock
    ResourceCollectionManager resourceCollectionManager;

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    Session session;

    @Mock
    PageManager pageManager;

    @Mock
    Page workflowPackagePage;

    @Mock
    Resource workflowPackageResource;

    @Mock
    Node workflowPackageNode;

    @Mock
    Resource contentResource;

    @Mock
    Page normalPage;

    @Mock
    Resource normalPageContentResource;

    @InjectMocks
    WorkflowPackageManager wpm = new WorkflowPackageManagerImpl();

    @Before
    public void setUp() throws Exception {
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(resourceResolver.getResource(WORKFLOW_PACKAGE_PATH)).thenReturn(workflowPackageResource);
        when(workflowPackageResource.adaptTo(Node.class)).thenReturn(workflowPackageNode);
        when(workflowPackageNode.getSession()).thenReturn(session);
        when(pageManager.getPage(WORKFLOW_PACKAGE_PATH)).thenReturn(workflowPackagePage);
        when(pageManager.getContainingPage(WORKFLOW_PACKAGE_PATH)).thenReturn(workflowPackagePage);
        when(workflowPackagePage.getContentResource()).thenReturn(contentResource);
        when(contentResource.isResourceType("cq/workflow/components/collection/page")).thenReturn(true);
        when(contentResource.getChild("vlt:definition")).thenReturn(mock(Resource.class));
        when(contentResource.adaptTo(Node.class)).thenReturn(workflowPackageNode);

        // Normal Page Path

        when(pageManager.getPage(NORMAL_PAGE_PATH)).thenReturn(normalPage);
        when(pageManager.getContainingPage(NORMAL_PAGE_PATH)).thenReturn(normalPage);
        when(normalPage.getContentResource()).thenReturn(normalPageContentResource);
        when(normalPageContentResource.isResourceType("cq/workflow/components/collection/page")).thenReturn(false);
    }


    @Test
    public void testCreate() throws Exception {
        // Create method requires complete mocking of impl; No point.
    }

    @Test
    public void testGetPaths() throws Exception {
        PowerMockito.mockStatic(ResourceCollectionUtil.class);
        when(ResourceCollectionUtil.getResourceCollection(workflowPackageNode,
                resourceCollectionManager)).thenReturn(testResourceCollection);

        final String[] expected = PAYLOAD_PATHS;

        List<String> paths = wpm.getPaths(resourceResolver, WORKFLOW_PACKAGE_PATH);
        final String[] actual = paths.toArray(new String[0]);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testGetPaths_NormalPage() throws Exception {

        final String[] expected = new String[]{};

        List<String> paths = wpm.getPaths(resourceResolver, NORMAL_PAGE_PATH);
        final String[] actual = paths.toArray(new String[0]);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testDelete() throws Exception {
        wpm.delete(resourceResolver, WORKFLOW_PACKAGE_PATH);

        verify(workflowPackageNode, times(1)).remove();
        verify(session, times(1)).save();
    }

    @Test
    public void testIsWorkflowPackage() throws Exception {
        assertTrue(wpm.isWorkflowPackage(resourceResolver, WORKFLOW_PACKAGE_PATH));
    }

    @Test
    public void testIsWorkflowPackage_NormalPage() throws Exception {
        assertFalse(wpm.isWorkflowPackage(resourceResolver, NORMAL_PAGE_PATH));
    }

    ResourceCollection testResourceCollection = new ResourceCollection() {
        private String[] paths = PAYLOAD_PATHS;

        @Override
        public List<Node> list(final String[] strings) throws RepositoryException {
            List<Node> nodes = new ArrayList<Node>();

            for(String path : paths) {
                Node node = mock(Node.class);
                when(node.getPath()).thenReturn(path);
                nodes.add(node);
            }

            return nodes;
        }

        @Override
        public void add(final Node node) {

        }

        @Override
        public void remove(final Node node) {

        }

        @Override
        public void startRecording(final String s, final String s2, final String[] strings) {

        }

        @Override
        public void stopRecording(final String s) {

        }

        @Override
        public String getPath() {
            return WORKFLOW_PACKAGE_PATH;
        }
    };
}