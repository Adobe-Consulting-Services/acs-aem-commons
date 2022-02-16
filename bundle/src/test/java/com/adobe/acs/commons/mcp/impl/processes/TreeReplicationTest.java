/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

import static com.adobe.acs.commons.fam.impl.ActionManagerTest.getActionManager;
import static com.adobe.acs.commons.fam.impl.ActionManagerTest.getFreshMockResolver;
import static com.adobe.acs.commons.fam.impl.ActionManagerTest.getMockResolver;
import static com.day.cq.commons.jcr.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.ObservationManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.impl.ActionManagerFactoryImpl;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.form.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;

/**
 * Validate correct function of the tree activation utility
 *
 * @author brobert
 */
public class TreeReplicationTest {

    TreeReplicationFactory factory = new TreeReplicationFactory();
    TreeReplication tool;
    ProcessInstanceImpl instance;
    ResourceResolver rr;
    Replicator replicator;

    @Before
    public void setup() throws RepositoryException, PersistenceException, IllegalAccessException, LoginException, ReplicationException {
        replicator =  mock(Replicator.class);
        factory.replicator = replicator;
        tool = prepareProcessDefinition(factory.createProcessDefinition(), null);
        instance = prepareProcessInstance(
                new ProcessInstanceImpl(getControlledProcessManager(), tool, "relocator test")
        );
        rr = getEnhancedMockResolver();
        when(rr.hasChanges()).thenReturn(true);
    }

    @Test
    public void testFactory() {
        TreeReplication replicationProcess = factory.createProcessDefinition();
        assertEquals("Should inject replicator service", replicationProcess.replicatorService, replicator);
    }

    @Test
    public void testTreeOnlyActivation() throws DeserializeException, RepositoryException, ReplicationException {
        Map<String, Object> values = new HashMap<>();
        values.put("startingPath", "/content/dam");
        values.put("publishFilter", "FOLDERS_AND_PAGES_ONLY");
        values.put("action", "PUBLISH");
        values.put("dryRun", false);
        instance.init(rr, values);
        assertEquals(0.0, instance.updateProgress(), 0.00001);
        instance.run(rr);
        assertEquals(1.0, instance.updateProgress(), 0.00001);

        ArgumentCaptor<String> activationCaptor = ArgumentCaptor.forClass(String.class);
        verify(replicator, times(3))
                .replicate(any(Session.class), eq(ReplicationActionType.ACTIVATE), activationCaptor.capture(), any(ReplicationOptions.class));

        ArgumentCaptor<String> deactivationCaptor = ArgumentCaptor.forClass(String.class);
        verify(replicator, never())
                .replicate(any(Session.class), eq(ReplicationActionType.DEACTIVATE), deactivationCaptor.capture(), any(ReplicationOptions.class));

        assertTrue("Should publish /content/dam", activationCaptor.getAllValues().contains("/content/dam"));
        assertTrue("Should publish /content/dam/folderA", activationCaptor.getAllValues().contains("/content/dam/folderA"));
        assertTrue("Should publish /content/dam/folderB", activationCaptor.getAllValues().contains("/content/dam/folderB"));
        assertEquals("Should only publish 3 things", 3, activationCaptor.getAllValues().size());
    }

    @Test
    public void testActivateAll() throws DeserializeException, RepositoryException, ReplicationException {
        Map<String, Object> values = new HashMap<>();
        values.put("startingPath", "/content/dam");
        values.put("publishFilter", "ALL");
        values.put("action", "PUBLISH");
        values.put("dryRun", false);
        instance.init(rr, values);
        assertEquals(0.0, instance.updateProgress(), 0.00001);
        instance.run(rr);

        ArgumentCaptor<String> activationCaptor = ArgumentCaptor.forClass(String.class);
        verify(replicator, times(5))
                .replicate(any(Session.class), eq(ReplicationActionType.ACTIVATE), activationCaptor.capture(), any(ReplicationOptions.class));

        ArgumentCaptor<String> deactivationCaptor = ArgumentCaptor.forClass(String.class);
        verify(replicator, never())
                .replicate(any(Session.class), eq(ReplicationActionType.DEACTIVATE), deactivationCaptor.capture(), any(ReplicationOptions.class));

        assertEquals(1.0, instance.updateProgress(), 0.00001);
        assertTrue("Should publish /content/dam", activationCaptor.getAllValues().contains("/content/dam"));
        assertTrue("Should publish /content/dam/folderA", activationCaptor.getAllValues().contains("/content/dam/folderA"));
        assertTrue("Should publish /content/dam/folderB", activationCaptor.getAllValues().contains("/content/dam/folderB"));
        assertTrue("Should publish /content/dam/folderA/asset1", activationCaptor.getAllValues().contains("/content/dam/folderA/asset1"));
        assertTrue("Should publish /content/dam/folderA/asset2", activationCaptor.getAllValues().contains("/content/dam/folderA/asset2"));
        assertEquals("Should only publish 5 things", 5, activationCaptor.getAllValues().size());
    }

    @Test
    public void testActivateSite() throws DeserializeException, RepositoryException, ReplicationException {
        Map<String, Object> values = new HashMap<>();
        values.put("startingPath", "/content/siteA");
        values.put("publishFilter", "FOLDERS_AND_PAGES_ONLY");
        values.put("action", "PUBLISH");
        values.put("dryRun", false);
        instance.init(rr, values);
        assertEquals(0.0, instance.updateProgress(), 0.00001);
        instance.run(rr);

        ArgumentCaptor<String> activationCaptor = ArgumentCaptor.forClass(String.class);
        verify(replicator, times(4))
                .replicate(any(Session.class), eq(ReplicationActionType.ACTIVATE), activationCaptor.capture(), any(ReplicationOptions.class));

        ArgumentCaptor<String> deactivationCaptor = ArgumentCaptor.forClass(String.class);
        verify(replicator, never())
                .replicate(any(Session.class), eq(ReplicationActionType.DEACTIVATE), deactivationCaptor.capture(), any(ReplicationOptions.class));

        assertEquals(1.0, instance.updateProgress(), 0.00001);
        assertTrue("Should publish /content/siteA", activationCaptor.getAllValues().contains("/content/siteA"));
        assertTrue("Should publish /content/siteA/page1",  activationCaptor.getAllValues().contains("/content/siteA/page1"));
        assertTrue("Should publish /content/siteA/page1/page1a",  activationCaptor.getAllValues().contains("/content/siteA/page1/page1a"));
        assertTrue("Should publish /content/siteA/page2",  activationCaptor.getAllValues().contains("/content/siteA/page2"));
        assertEquals("Should only publish 4 things", 4,  activationCaptor.getAllValues().size());
    }

    @Test
    public void testDeactivate() throws DeserializeException, RepositoryException, ReplicationException {
        Map<String, Object> values = new HashMap<>();
        values.put("startingPath", "/content");
        values.put("publishFilter", "FOLDERS_AND_PAGES_ONLY");
        values.put("action", "UNPUBLISH");
        values.put("dryRun", false);
        instance.init(rr, values);
        assertEquals(0.0, instance.updateProgress(), 0.00001);
        instance.run(rr);

        ArgumentCaptor<String> activationCaptor = ArgumentCaptor.forClass(String.class);
        verify(replicator, never())
                .replicate(any(Session.class), eq(ReplicationActionType.ACTIVATE), activationCaptor.capture(), any(ReplicationOptions.class));

        ArgumentCaptor<String> deactivationCaptor = ArgumentCaptor.forClass(String.class);
        verify(replicator, times(1))
                .replicate(any(Session.class), eq(ReplicationActionType.DEACTIVATE), deactivationCaptor.capture(), any(ReplicationOptions.class));

        assertEquals(1.0, instance.updateProgress(), 0.00001);
        assertTrue("Should unpublish /content", deactivationCaptor.getValue().equals("/content"));
    }

    Map<String, String> testNodes = new TreeMap<String, String>() {
        {
            put("/content", JcrResourceConstants.NT_SLING_FOLDER);
            put("/content/dam", JcrResourceConstants.NT_SLING_FOLDER);
            put("/content/dam/folderA", JcrResourceConstants.NT_SLING_FOLDER);
            put("/content/dam/folderB", JcrResourceConstants.NT_SLING_FOLDER);
            put("/content/dam/folderA/asset1", DamConstants.NT_DAM_ASSET);
            put("/content/dam/folderA/asset1/jcr:content", "NT:UNSTRUCTURED");
            put("/content/dam/folderA/asset2", DamConstants.NT_DAM_ASSET);
            put("/content/dam/folderA/asset2/jcr:content", "NT:UNSTRUCTURED");
            put("/test", "NT:UNSTRUCTURED");
            put("/test/child1", "NT:UNSTRUCTURED");
            put("/content/siteA", NameConstants.NT_PAGE);
            put("/content/siteA/jcr:content", "NT:UNSTRUCTURED");
            put("/content/siteA/page1", NameConstants.NT_PAGE);
            put("/content/siteA/page1/jcr:content", "NT:UNSTRUCTURED");
            put("/content/siteA/page1/page1a", NameConstants.NT_PAGE);
            put("/content/siteA/page1/page1a/jcr:content", "NT:UNSTRUCTURED");
            put("/content/siteA/page2", NameConstants.NT_PAGE);
        }
    };

    private ResourceResolver getEnhancedMockResolver() throws RepositoryException, LoginException, PersistenceException {
        rr = getFreshMockResolver();

        testNodes.entrySet().forEach(entry -> {
            String path = entry.getKey();
            String type = entry.getValue();
            AbstractResourceImpl mockFolder = new AbstractResourceImpl(path, type, "", new ResourceMetadata());
            mockFolder.getResourceMetadata().put(JCR_PRIMARYTYPE, type);

            when(rr.resolve(path)).thenReturn(mockFolder);
            when(rr.getResource(path)).thenReturn(mockFolder);
        });
        testNodes.entrySet().forEach(entry -> {
            String parentPath = StringUtils.substringBeforeLast(entry.getKey(), "/");
            if (rr.getResource(parentPath) != null) {
                AbstractResourceImpl parent = ((AbstractResourceImpl) rr.getResource(parentPath));
                AbstractResourceImpl node = (AbstractResourceImpl) rr.getResource(entry.getKey());
                parent.addChild(node);
            }
        });

        Session ses = mock(Session.class);
        when(ses.nodeExists(any())).thenReturn(true); // Needed to prevent MovingFolder.createFolder from going berserk
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
        doAnswer((InvocationOnMock invocationOnMock) -> {
            CheckedConsumer<ResourceResolver> action = (CheckedConsumer<ResourceResolver>) invocationOnMock.getArguments()[0];
            action.accept(getMockResolver());
            return null;
        }).when(instance).asServiceUser(any());

        return instance;
    }

    private TreeReplication prepareProcessDefinition(TreeReplication source, Function<String, List<String>> refFunction) throws RepositoryException, PersistenceException, IllegalAccessException {
        TreeReplication definition = spy(source);
        doNothing().when(definition).storeReport(any(), any());
        return definition;
    }
}
