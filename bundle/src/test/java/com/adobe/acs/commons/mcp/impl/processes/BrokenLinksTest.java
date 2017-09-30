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
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.impl.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import static com.adobe.acs.commons.mcp.impl.processes.BrokenLinksReport.REPORT;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.fam.impl.ActionManagerTest.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class BrokenLinksTest {
    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    BrokenLinksReport tool;

    @Before
    public void setup() {
        tool = getBrokenLinksReport();
    }

    @Test
    public void reportBrokenReferences() throws Exception {
        ResourceResolver rr = getEnhancedMockResolver();

        AbstractResourceImpl content = new AbstractResourceImpl("/content", "cq:Page", "cq:Page", new ResourceMetadata());

        AbstractResourceImpl pageA = new AbstractResourceImpl("/content/pageA", "cq:Page", "cq:Page", new ResourceMetadata());
        content.addChild(pageA);
        AbstractResourceImpl pageAcontent = new AbstractResourceImpl("/content/pageA/jcr:content", "cq:PageContent", "cq:PageContent", new ResourceMetadata() {{
            put("ref1", "/content/pageA");
            put("ref2", "/content/pageB");
            put("ref3", "/content/pageC");
        }});
        pageA.addChild(pageAcontent);
        AbstractResourceImpl pageB = new AbstractResourceImpl("/content/pageB", "cq:Page", "cq:Page", new ResourceMetadata());
        content.addChild(pageB);
        AbstractResourceImpl pageBcontent = new AbstractResourceImpl("/content/pageB/jcr:content", "cq:PageContent", "cq:PageContent", new ResourceMetadata() {{
            put("ignoredRef", "/content/pageC");
            put("ref2", "/content/pageD");
            put("ref3", "/content/pageE");
        }});
        pageB.addChild(pageBcontent);
        when(rr.getResource("/content")).thenReturn(content);

        pageA.setResourceResolver(rr);
        pageAcontent.setResourceResolver(rr);
        pageB.setResourceResolver(rr);
        pageBcontent.setResourceResolver(rr);

        when(rr.resolve("/content/pageA")).thenReturn(pageA);
        when(rr.resolve("/content/pageB")).thenReturn(pageB);
        when(rr.resolve("/content/pageC")).thenReturn(new NonExistingResource(rr, "/content/pageC"));
        when(rr.resolve("/content/pageD")).thenReturn(new NonExistingResource(rr, "/content/pageD"));
        when(rr.resolve("/content/pageE")).thenReturn(new NonExistingResource(rr, "/content/pageE"));

        Map<String, Object> values = new HashMap<>();
        values.put("sourcePath", "/content");
        values.put("propertyRegex", "^/(etc|content)/.+");
        values.put("excludeProperties", "ignoredRef");

        ProcessInstanceImpl instance = new ProcessInstanceImpl(getControlledProcessManager(), tool, "broken references");
        instance.init(rr, values);
        instance.run(rr);

        Map<String, EnumMap<REPORT, Object>> reportData = tool.getReportData();
        assertEquals(3, reportData.size());
        assertEquals("/content/pageC", reportData.get("/content/pageA/jcr:content/ref3").get(REPORT.reference));
        assertEquals("/content/pageD", reportData.get("/content/pageB/jcr:content/ref2").get(REPORT.reference));
        assertEquals("/content/pageE", reportData.get("/content/pageB/jcr:content/ref3").get(REPORT.reference));

        assertFalse("ignoredRef is in the exclude list", reportData.containsKey("/content/pageB/jcr:content/ignoredRef"));
    }

    private BrokenLinksReport getBrokenLinksReport() {

        BrokenLinksReportFactory t = new BrokenLinksReportFactory();
        slingContext.registerInjectActivateService(t);

        return t.createProcessDefinition();
    }

    private ResourceResolver getEnhancedMockResolver() throws RepositoryException, LoginException {
        ResourceResolver rr = getFreshMockResolver();

        AbstractResourceImpl processes = new AbstractResourceImpl(ProcessInstanceImpl.BASE_PATH, null, null, new ResourceMetadata());
        when(rr.getResource(ProcessInstanceImpl.BASE_PATH)).thenReturn(processes);

        Session ses = mock(Session.class);
        when(rr.adaptTo(Session.class)).thenReturn(ses);
        AccessControlManager acm = mock(AccessControlManager.class);
        when(ses.getAccessControlManager()).thenReturn(acm);
        when(acm.privilegeFromName(any())).thenReturn(mock(Privilege.class));
        when(acm.hasPrivileges(any(), any())).thenReturn(true);

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
