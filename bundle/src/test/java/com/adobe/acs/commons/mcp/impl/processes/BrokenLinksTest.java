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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.impl.ActionManagerFactoryImpl;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.form.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import com.adobe.acs.commons.mcp.impl.processes.BrokenLinksReport.Report;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.adobe.acs.commons.fam.impl.ActionManagerTest.*;
import static com.adobe.acs.commons.mcp.impl.processes.BrokenLinksReport.collectBrokenReferences;
import static com.adobe.acs.commons.mcp.impl.processes.BrokenLinksReport.collectPaths;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class BrokenLinksTest {
    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private BrokenLinksReport tool;

    @Before
    public void setup() {
        tool = getBrokenLinksReport();
    }

    @Test
    public void reportBrokenReferences() throws Exception {
        final ResourceResolver rr = getEnhancedMockResolver();

        AbstractResourceImpl content = new AbstractResourceImpl("/content", "cq:Page", "cq:Page", new ResourceMetadata());

        AbstractResourceImpl pageA = new AbstractResourceImpl("/content/pageA", "cq:Page", "cq:Page", new ResourceMetadata());
        content.addChild(pageA);
        AbstractResourceImpl pageAcontent = new AbstractResourceImpl("/content/pageA/jcr:content", "cq:PageContent", "cq:PageContent", new ResourceMetadata() {{
                put("ref1", "/content/pageA");
                put("ref2", "/content/pageB");
                put("ref3", "/content/pageC");
            }
        });
        pageA.addChild(pageAcontent);
        AbstractResourceImpl pageB = new AbstractResourceImpl("/content/pageB", "cq:Page", "cq:Page", new ResourceMetadata());
        content.addChild(pageB);
        AbstractResourceImpl pageBcontent = new AbstractResourceImpl("/content/pageB/jcr:content", "cq:PageContent", "cq:PageContent", new ResourceMetadata() {{
                put("ignoredRef", "/content/pageC");
                put("ref2", "/content/pageD");
                put("ref3", "/content/pageE");
            }
        });
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

        Map<String, EnumMap<Report, Object>> reportData = tool.getReportData();
        assertEquals(3, reportData.size());
        assertEquals("/content/pageC", reportData.get("/content/pageA/jcr:content/ref3").get(Report.reference));
        assertEquals("/content/pageD", reportData.get("/content/pageB/jcr:content/ref2").get(Report.reference));
        assertEquals("/content/pageE", reportData.get("/content/pageB/jcr:content/ref3").get(Report.reference));

        assertFalse("ignoredRef is in the exclude list", reportData.containsKey("/content/pageB/jcr:content/ignoredRef"));
    }

    private BrokenLinksReport getBrokenLinksReport() {

        BrokenLinksReportFactory t = new BrokenLinksReportFactory();
        slingContext.registerInjectActivateService(t);

        return t.createProcessDefinition();
    }

    private ResourceResolver getEnhancedMockResolver() throws RepositoryException, LoginException, PersistenceException {
        final ResourceResolver rr = getFreshMockResolver();

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


    private ControlledProcessManager getControlledProcessManager() throws LoginException, PersistenceException {
        ActionManager am = getActionManager();

        ActionManagerFactory amf = mock(ActionManagerFactoryImpl.class);
        when(amf.createTaskManager(any(), any(), anyInt())).thenReturn(am);

        ControlledProcessManager cpm = mock(ControlledProcessManager.class);
        when(cpm.getActionManagerFactory()).thenReturn(amf);
        when(cpm.getServiceResourceResolver()).thenReturn(getMockResolver());
        return cpm;
    }

    @Test
    public void testCollectPaths(){
        Set<String> htmlFields = new HashSet<>();
        htmlFields.add("text");

        assertEquals(Arrays.asList("/ref1"), collectPaths(property("fileReference", "/ref1"), htmlFields).collect(Collectors.toList()));
        assertEquals(Arrays.asList("/ref1", "/ref2"), collectPaths(property("fileReference", new String[]{"/ref1", "/ref2"}), htmlFields).collect(Collectors.toList()));
        assertEquals(Arrays.asList("/ref1"), collectPaths(property("text", "<p><a href='/ref1'>hello</p>"), htmlFields).collect(Collectors.toList()));
    }

    private Map.Entry property(String key, Object value){
        return new Map.Entry<String, Object>() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public Object setValue(Object value) {
                return null;
            }
        };
    }

    @Test
    public void testCollectBrokenReferences(){
        Pattern ptrn = Pattern.compile("/content/.+");
        Set<String> skipList = new HashSet<>(Arrays.asList("skip1", "skip2"));
        Set<String> htmlFields = new HashSet<>(Arrays.asList("text"));
        slingContext.build()
                .resource("/test1",
                        "p1", "/content/ref1",
                        "p2", "/content/ref2",
                        "p3", new String[]{"/content/ref1"},
                        "p4", new String[]{"/content/ref1", "/content/ref2"},
                        "skip1", "/content/ref2")
                .resource("/test2",
                        "text", "<p><a href='/content/ref2'>hello</a><img src='/content/ref3'>hello</img></p>",
                        "skip2", "<p><a href='/content/ref2'>hello</a><img src='/content/ref3'>hello</img></p>")
                .resource("/content/ref1")
                .commit();

        Map<String, List<String>> refs1 = collectBrokenReferences(slingContext.resourceResolver().getResource("/test1"), ptrn, skipList, htmlFields);
        assertEquals(2, refs1.size());
        assertEquals(Arrays.asList("/content/ref2"), refs1.get("/test1/p2"));
        assertEquals(Arrays.asList("/content/ref2"), refs1.get("/test1/p4"));

        Map<String, List<String>> refs2 = collectBrokenReferences(slingContext.resourceResolver().getResource("/test2"), ptrn, skipList, htmlFields);
        assertEquals(1, refs2.size());
        assertEquals(Arrays.asList("/content/ref2", "/content/ref3"), refs2.get("/test2/text"));
    }


}
