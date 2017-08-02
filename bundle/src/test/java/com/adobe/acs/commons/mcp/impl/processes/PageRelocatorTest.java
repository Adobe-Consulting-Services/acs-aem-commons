/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.impl.ActionManagerFactoryImpl;
import static com.adobe.acs.commons.fam.impl.ActionManagerTest.*;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.impl.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 *
 */
public class PageRelocatorTest {
    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    PageRelocator tool = getPageRelocatorTool();

    @Test
    public void barebonesRun() throws LoginException, DeserializeException, RepositoryException, PersistenceException {
        ResourceResolver rr = getEnhancedMockResolver(true);
        ProcessInstance instance = new ProcessInstanceImpl(getControlledProcessManager(), tool, "relocator test");

        assertEquals("Page Relocator: relocator test", instance.getName());
        initInstance(instance, rr);
        assertEquals(0.0, instance.updateProgress(), 0.00001);
        instance.run(rr);
        assertEquals(1.0, instance.updateProgress(), 0.00001);
        verify(rr, atLeast(3)).commit();
    }

    @Test
    public void aclFail() throws LoginException, DeserializeException, RepositoryException, PersistenceException {
        ResourceResolver rr = getEnhancedMockResolver(false);
        ProcessInstance instance = new ProcessInstanceImpl(getControlledProcessManager(), tool, "relocator test");

        assertEquals("Page Relocator: relocator test", instance.getName());
        initInstance(instance, rr);
        instance.run(rr);
        assertFalse("ACL issues should have been tracked", instance.getInfo().getReportedErrors().isEmpty());
        assertEquals("Aborted", instance.getInfo().getStatus());
    }

    @Test
    public void validateTreeCreate() throws LoginException, DeserializeException, RepositoryException, PersistenceException {
        ResourceResolver rr = getEnhancedMockResolver(true);
        ProcessInstance instance = new ProcessInstanceImpl(getControlledProcessManager(), tool, "relocator test");
        initInstance(instance, rr);

        tool.buildTargetStructure(getActionManager());
        Resource pageB = rr.getResource("/content/pageB");
        verify(rr, times(1)).create(eq(pageB), eq("pageA"), any());
        verify(rr, atLeastOnce()).commit();
    }

    @Test
    public void validateMoveOperation() throws RepositoryException, LoginException, DeserializeException, PersistenceException {
        ResourceResolver rr = getEnhancedMockResolver(true);
        ProcessInstance instance = new ProcessInstanceImpl(getControlledProcessManager(), tool, "relocator test");
        initInstance(instance, rr);

        ActionManager manager = getActionManager();
        tool.buildTargetStructure(manager);
        tool.movePages(manager);
        assertTrue("Should be no reported errors", manager.getErrorCount() == 0);
        assertFalse("Should have captured activate requests", tool.replicatorQueue.activateOperations.isEmpty());
        assertFalse("Should have captured deactivate requests", tool.replicatorQueue.deactivateOperations.isEmpty());
        
        Resource pageB = rr.getResource("/content/pageB");
        verify(rr, times(1)).create(eq(pageB), eq("pageA"), any());
        verify(rr, atLeastOnce()).commit();
    }
    
    private PageRelocator getPageRelocatorTool() {
        PageRelocator t = new PageRelocator();
        slingContext.registerService(ProcessDefinition.class, t);
        t.pageManagerFactory = mock(PageManagerFactory.class);
        PageManager mockPageManager = mock(PageManager.class);
        when(t.pageManagerFactory.getPageManager(any())).then(invocation->new MockPageManager(getMockResolver()));
        return t;
    }

    private void initInstance(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, DeserializeException {
        Map<String, Object> values = new HashMap<>();
        values.put("sourcePath", "/content/pageA");
        values.put("destinationPath", "/content/pageB");
        values.put("mode", PageRelocator.Mode.MOVE.toString());
        values.put("publishMethod", PageRelocator.PUBLISH_METHOD.SELF_MANAGED.toString());
        values.put("createVerionsOnReplicate", "false");
        values.put("updateStatus", "true");
        instance.init(rr, values);

        AbstractResourceImpl processNode = new AbstractResourceImpl(instance.getPath(), null, null, new ResourceMetadata());
        when(rr.getResource(instance.getPath())).thenReturn(processNode);
        AbstractResourceImpl processContentNode = new AbstractResourceImpl(instance.getPath() + "/jcr:content", null, null, new ResourceMetadata());
        when(rr.getResource(instance.getPath() + "/jcr:content")).thenReturn(processContentNode);
        AbstractResourceImpl processResultNode = new AbstractResourceImpl(instance.getPath() + "/jcr:content/result", null, null, new ResourceMetadata());
        when(rr.getResource(instance.getPath() + "/jcr:content/result")).thenReturn(processResultNode);
        AbstractResourceImpl failuresNode = new AbstractResourceImpl(instance.getPath() + "/jcr:content/failures", null, null, new ResourceMetadata());
        when(rr.getResource(instance.getPath() + "/jcr:content/failures")).thenReturn(processResultNode);
        when(rr.getResource(instance.getPath() + "/jcr:content/failures/step1")).thenReturn(processResultNode);
    }

    private ResourceResolver getEnhancedMockResolver(final boolean aclChecksPass) throws RepositoryException, LoginException {
        ResourceResolver rr = getFreshMockResolver();

        when(rr.hasChanges()).thenReturn(true);

        AbstractResourceImpl pageA = new AbstractResourceImpl("/content/pageA", "cq:Page", "cq:Page", new ResourceMetadata());
        when(rr.getResource("/content/pageA")).thenReturn(pageA);
        AbstractResourceImpl pageAcontent = new AbstractResourceImpl("/content/pageA/jcr:content", "cq:PageContent", "cq:PageContent", new ResourceMetadata());
        when(rr.getResource("/content/pageA/jcr:content")).thenReturn(pageAcontent);
        AbstractResourceImpl pageB = new AbstractResourceImpl("/content/pageB", "cq:Page", "cq:Page", new ResourceMetadata());
        when(rr.getResource("/content/pageB")).thenReturn(pageB);
        AbstractResourceImpl content = new AbstractResourceImpl("/content", "cq:Page", "cq:Page", new ResourceMetadata());
        AbstractResourceImpl pageBcontent = new AbstractResourceImpl("/content/pageB/jcr:content", "cq:PageContent", "cq:PageContent", new ResourceMetadata());
        when(rr.getResource("/content/pageB/jcr:content")).thenReturn(pageBcontent);
        when(rr.getResource("/content")).thenReturn(content);
        content.addChild(pageA);
        content.addChild(pageB);

        AbstractResourceImpl processes = new AbstractResourceImpl(ProcessInstanceImpl.BASE_PATH, null, null, new ResourceMetadata());
        when(rr.getResource(ProcessInstanceImpl.BASE_PATH)).thenReturn(processes);

        Session ses = mock(Session.class);
        when(rr.adaptTo(Session.class)).thenReturn(ses);
        AccessControlManager acm = mock(AccessControlManager.class);
        when(ses.getAccessControlManager()).thenReturn(acm);
        when(acm.privilegeFromName(any())).thenReturn(mock(Privilege.class));
        when(acm.hasPrivileges(any(), any())).thenReturn(aclChecksPass);

        return rr;
    }

    private ControlledProcessManager getControlledProcessManager() throws LoginException {
        ActionManager am = getActionManager();

        ActionManagerFactory amf = mock(ActionManagerFactoryImpl.class);
        when(amf.createTaskManager(any(), any(), anyInt())).thenReturn(am);

        ControlledProcessManager cpm = mock(ControlledProcessManager.class);
        when(cpm.getActionManagerFactory()).thenReturn(amf);
        when(cpm.getServiceResourceResolver()).thenReturn(getMockResolver());
        return cpm;
    }
}
