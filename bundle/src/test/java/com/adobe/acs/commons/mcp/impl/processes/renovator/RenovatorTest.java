/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.renovator;

import static com.adobe.acs.commons.fam.impl.ActionManagerTest.getActionManager;
import static com.adobe.acs.commons.fam.impl.ActionManagerTest.getFreshMockResolver;
import static com.adobe.acs.commons.fam.impl.ActionManagerTest.getMockResolver;
import static com.day.cq.commons.jcr.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.ObservationManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.impl.ActionManagerFactoryImpl;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.form.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import com.day.cq.dam.api.DamConstants;

/**
 * Tests a few cases for folder relocator
 */
@RunWith(MockitoJUnitRunner.class)
public class RenovatorTest {
    RenovatorFactory factory = new RenovatorFactory();
    Renovator tool;
    ProcessInstanceImpl instance;
    ReplicatorQueue queue;
    ResourceResolver rr;

    @Before
    public void setup() throws RepositoryException, PersistenceException, IllegalAccessException, LoginException {
        queue = spy(new ReplicatorQueue());
        factory.setReplicator(queue);
        tool = prepareProcessDefinition(factory.createProcessDefinition(), null);
        instance = prepareProcessInstance(
                new ProcessInstanceImpl(getControlledProcessManager(), tool, "relocator test")
        );
        rr = getEnhancedMockResolver();
        when(rr.hasChanges()).thenReturn(true);
    }

    @Test(expected = RepositoryException.class)
    public void testRequiredFields() throws LoginException, DeserializeException, RepositoryException {
        assertEquals("Renovator: relocator test", instance.getName());
        instance.init(rr, Collections.EMPTY_MAP);
        tool.buildProcess(instance, rr);
        fail("That should have thrown an error");
    }

    @Test
    public void barebonesRun() throws DeserializeException, RepositoryException, PersistenceException {
        assertEquals("Renovator: relocator test", instance.getName());
        Map<String, Object> values = new HashMap<>();
        values.put("sourceJcrPath", "/content/dam/folderA");
        values.put("destinationJcrPath", "/content/dam/folderB");
        instance.init(rr, values);
        assertEquals(0.0, instance.updateProgress(), 0.00001);
        instance.run(rr);
        assertEquals(1.0, instance.updateProgress(), 0.00001);
        verify(rr, atLeast(3)).commit();
    }

    @Test
    public void noPublishTest() throws Exception {
        assertEquals("Renovator: relocator test", instance.getName());
        Map<String, Object> values = new HashMap<>();
        values.put("sourceJcrPath", "/content/dam/folderA");
        values.put("destinationJcrPath", "/content/dam/republishA");
        values.put("dryRun", "false");

        instance.init(rr, values);
        instance.run(rr);
        assertTrue("Should unpublish the source folder", queue.getDeactivateOperations().containsKey("/content/dam/folderA"));
        assertTrue("Should publish the moved source folder", queue.getActivateOperations().containsKey("/content/dam/republishA"));
    }

    @Test
    public void testHaltingScenario() throws DeserializeException, LoginException, RepositoryException, InterruptedException, ExecutionException, PersistenceException {
        assertEquals("Renovator: relocator test", instance.getName());
        Map<String, Object> values = new HashMap<>();
        values.put("sourceJcrPath", "/content/dam/folderA");
        values.put("destinationJcrPath", "/content/dam/folderB");
        instance.init(rr, values);

        CompletableFuture<Boolean> f = new CompletableFuture<>();

        instance.defineAction("Halt", rr, am -> {
            instance.halt();
            try {
                assertTrue(instance.updateProgress() < 1.0);
                assertFalse(instance.getInfo().isIsRunning());
                f.complete(true);
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        instance.run(rr);
        assertTrue(f.get());
        verify(rr, atLeastOnce()).commit();
    }

    @Test
    public void testUpdateAssetReferences() throws RepositoryException, PersistenceException, LoginException, IllegalAccessException {
        final ModifiableValueMap test1 = rr.resolve("/test").adaptTo(ModifiableValueMap.class);
        final ModifiableValueMap test2 = rr.resolve("/test/child1").adaptTo(ModifiableValueMap.class);
        MovingAsset asset = new MovingAsset();
        asset.setSourcePath("/source");
        asset.setDestinationPath("/target");
        test1.put("attr1", "/source");
        test2.put("attr2", "/source");
        asset.updateReferences(queue, rr, "/test");
        assertEquals("/target", test1.get("attr1"));
        assertEquals("/target", test2.get("attr2"));
    }

    @Test
    public void testMoveManyAssets() throws DeserializeException, RepositoryException {
        Map<String, Object> values = new HashMap<>();
        values.put("dryRun", "false");
        // Provide input so that the init() method doesn't error out, but this should be ignored later
        values.put("sourceJcrPath", "/content/dam/folderB");
        values.put("destinationJcrPath", "/content/dam/ignoreFolderB");
        // Inject some move paths
        tool.movePaths.put("/content/dam/folderA/asset1", "/content/dam/folderC/subfolder/asset1-renamed");
        tool.movePaths.put("/content/dam/folderA/asset2", "/content/dam/folderC/subfolder/asset2-renamed");

        instance.init(rr, values);

        instance.run(rr);
        // Make sure that target folders were created
        assertNotNull(rr.getResource("/content/dam/folderC"));
        assertNotNull(rr.getResource("/content/dam/folderC/subfolder"));
        // Ensure process finished
        assertEquals(1.0, instance.updateProgress(), 0.00001);
        assertTrue("Should publish new folders", queue.getActivateOperations().containsKey("/content/dam/folderC"));
        assertTrue("Should publish new folders", queue.getActivateOperations().containsKey("/content/dam/folderC/subfolder"));
    }

    Map<String, String> testNodes = new TreeMap<String, String>() {
        {
            put("/content", JcrResourceConstants.NT_SLING_FOLDER);
            put("/content/dam", JcrResourceConstants.NT_SLING_FOLDER);
            put("/content/dam/folderA", JcrResourceConstants.NT_SLING_FOLDER);
            put("/content/dam/folderB", JcrResourceConstants.NT_SLING_FOLDER);
            put("/content/dam/folderA/asset1", DamConstants.NT_DAM_ASSET);
            put("/content/dam/folderA/asset2", DamConstants.NT_DAM_ASSET);
            put("/test", "NT:UNSTRUCTURED");
            put("/test/child1", "NT:UNSTRUCTURED");
        }
    };

    private ResourceResolver getEnhancedMockResolver() throws RepositoryException, LoginException, PersistenceException {
        rr = getFreshMockResolver();

        for (Map.Entry<String, String> entry : testNodes.entrySet()) {
            String path = entry.getKey();
            String type = entry.getValue();
            AbstractResourceImpl mockFolder = new AbstractResourceImpl(path, type, "", new ResourceMetadata());
            mockFolder.getResourceMetadata().put(JCR_PRIMARYTYPE, type);

            when(rr.resolve(path)).thenReturn(mockFolder);
            when(rr.getResource(path)).thenReturn(mockFolder);
        }
        for (Map.Entry<String, String> entry : testNodes.entrySet()) {
            String parentPath = StringUtils.substringBeforeLast(entry.getKey(), "/");
            if (rr.getResource(parentPath) != null) {
                AbstractResourceImpl parent = ((AbstractResourceImpl) rr.getResource(parentPath));
                AbstractResourceImpl node = (AbstractResourceImpl) rr.getResource(entry.getKey());
                parent.addChild(node);
            }
        }

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
//        doNothing().when(instance).recordErrors(anyInt(), any(), any());
        doAnswer((InvocationOnMock invocationOnMock) -> {
            CheckedConsumer<ResourceResolver> action = (CheckedConsumer<ResourceResolver>) invocationOnMock.getArguments()[0];
            action.accept(getMockResolver());
            return null;
        }).when(instance).asServiceUser(any());

        return instance;
    }

    private Renovator prepareProcessDefinition(Renovator source, Function<String, List<String>> refFunction) throws RepositoryException, PersistenceException, IllegalAccessException {
        Renovator definition = spy(source);
        doNothing().when(definition).storeReport(any(), any());
        doNothing().when(definition).checkNodeAcls(any(), any(), any());
        doAnswer((InvocationOnMock invocationOnMock) -> {
            if (refFunction != null) {
                MovingNode node = (MovingNode) invocationOnMock.getArguments()[1];
                node.getAllReferences().addAll(refFunction.apply(node.getSourcePath()));
            }
            return null;
        }).when(definition).findReferences(any(), any());
        return definition;
    }
}
