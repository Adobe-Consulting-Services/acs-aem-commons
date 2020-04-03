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
package com.adobe.acs.commons.mcp.impl.processes.cfi;

import com.adobe.acs.commons.data.CompositeVariant;
import com.adobe.acs.commons.data.Spreadsheet;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.impl.ActionManagerFactoryImpl;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.ObservationManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static com.adobe.acs.commons.fam.impl.ActionManagerTest.*;
import static com.adobe.acs.commons.mcp.impl.processes.cfi.ContentFragmentImport.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Assert that folders will be detected or skipped in different cases
 */
public class ContentFragmentImportTest {

    public ContentFragmentImportTest() {
    }

    ResourceResolver rr;
    ContentFragmentImport importer;
    ProcessInstanceImpl instance;
    MockContentFragment mockFragment;
    String currentNodePath = "";
    ArrayList<String> createdNodePaths = new ArrayList<>();

    @Before
    public void setUp() throws RepositoryException, LoginException, PersistenceException, IllegalAccessException, ContentFragmentException {
        rr = getEnhancedMockResolver();
        mockFragment = new MockContentFragment();
        importer = prepareProcessDefinition(new ContentFragmentImport());
        importer.spreadsheet = new Spreadsheet(false, PATH, FOLDER_TITLE, NAME, TITLE, TEMPLATE);
        importer.dryRunMode = false;
        instance = prepareProcessInstance(new ProcessInstanceImpl(getControlledProcessManager(), importer, "Test content fragment import"));
        currentNodePath = "";
        createdNodePaths.clear();
    }

    @Test
    public void basicSetupTest() {
        importer.dryRunMode = true;
        instance.run(rr);
        assertEquals("Should finish process", instance.getInfo().getProgress(), 1.0, 0.0001);
    }

    @Test
    public void importOne() {
        importer.dryRunMode = false;
        addImportRow("/test/path/fragment1", "Fragment 1", "element1", "element1value");
        mockFragment.elements.put("element1", null);
        instance.run(rr);
        assertEquals("Should finish process", instance.getInfo().getProgress(), 1.0, 0.0001);
        assertEquals("Should set fragment element", "element1value", mockFragment.elements.get("element1"));
    }

    @Test
    public void assertFolderCreation() {
        importer.dryRunMode = false;
        addImportRow("/test/path/fragment1", "Fragment 1", "element1", "element1value");
        mockFragment.elements.put("element1", null);
        instance.run(rr);
        assertTrue("Should have created test folder", createdNodePaths.contains("/test/path"));
        assertTrue("Should have created test metadata", createdNodePaths.contains("/test/path/jcr:content"));
        assertTrue("Should have created fragment1 folder", createdNodePaths.contains("/test/path/fragment1"));
        assertTrue("Should have created fragment1 metadata", createdNodePaths.contains("/test/path/fragment1/jcr:content"));
    }

    //------------------------------------------------------------------------------------------------------------------
    private void addImportRow(String path, String title, String... values) {
        Map<String, CompositeVariant> row = new HashMap<>();
        row.put(PATH, new CompositeVariant(path));
        row.put(FOLDER_TITLE, new CompositeVariant("test folder"));
        row.put(NAME, new CompositeVariant(StringUtils.substringAfter(path, "/")));
        row.put(TITLE, new CompositeVariant(title));
        row.put(TEMPLATE, new CompositeVariant("/test/template"));
        for (int i = 0; i < values.length - 1; i += 2) {
            row.put(values[i], new CompositeVariant(values[i + 1]));
        }
        importer.spreadsheet.getDataRowsAsCompositeVariants().add(row);
    }

    private ResourceResolver getEnhancedMockResolver() throws RepositoryException, LoginException, PersistenceException {
        rr = getFreshMockResolver();
        Session ses = mock(Session.class);
        Node node = mock(Node.class);
        when(ses.nodeExists("/test")).thenReturn(true); // Needed to prevent MovingFolder.createFolder from going berserk
        when(ses.getNode(any())).then(invocation -> {
            currentNodePath = invocation.getArgument(0);
            return node;
        });
        when(node.addNode(any(), any())).then(invocation -> {
            String nodeName = invocation.getArgument(0);
            String nodePath = currentNodePath + "/" + nodeName;
            createdNodePaths.add(nodePath);
            currentNodePath = nodePath;
            return node;
        });
        when(rr.adaptTo(Session.class)).thenReturn(ses);

        Workspace wk = mock(Workspace.class);
        ObservationManager om = mock(ObservationManager.class);
        when(ses.getWorkspace()).thenReturn(wk);
        when(wk.getObservationManager()).thenReturn(om);

        AccessControlManager acm = mock(AccessControlManager.class);
        when(ses.getAccessControlManager()).thenReturn(acm);
        when(acm.privilegeFromName(any())).thenReturn(mock(Privilege.class));

        return rr;
    }

    private ControlledProcessManager getControlledProcessManager() throws LoginException {
        ActionManagerFactory amf = mock(ActionManagerFactoryImpl.class);
        doAnswer((InvocationOnMock invocationOnMock) -> getActionManager())
                .when(amf).createTaskManager(any(), any(), anyInt());

        ControlledProcessManager cpm = mock(ControlledProcessManager.class);
        when(cpm.getActionManagerFactory()).thenReturn(amf);
        return cpm;
    }

    private ProcessInstanceImpl prepareProcessInstance(ProcessInstanceImpl source) throws PersistenceException {
        ProcessInstanceImpl instance = spy(source);
        doNothing().when(instance).persistStatus(any());
        doNothing().when(instance).recordErrors(anyInt(), any(), any());
        doAnswer((InvocationOnMock invocationOnMock) -> {
            CheckedConsumer<ResourceResolver> action = (CheckedConsumer<ResourceResolver>) invocationOnMock.getArguments()[0];
            action.accept(getMockResolver());
            return null;
        }).when(instance).asServiceUser(any());
        return instance;
    }

    private ContentFragmentImport prepareProcessDefinition(ContentFragmentImport source) throws RepositoryException, PersistenceException, IllegalAccessException, ContentFragmentException {
        ContentFragmentImport definition = spy(source);
        Resource mockResource = mock(Resource.class);
        doNothing().when(definition).storeReport(any(), any());
        doReturn(mockResource).when(definition).getFragmentTemplateResource(any(), any());
        doReturn(mockFragment).when(definition).getOrCreateFragment(any(), any(), any(), any());
        return definition;
    }
}
