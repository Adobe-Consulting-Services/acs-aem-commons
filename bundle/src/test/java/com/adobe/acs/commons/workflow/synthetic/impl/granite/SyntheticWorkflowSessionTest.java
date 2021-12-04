/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.workflow.synthetic.impl.granite;

import com.adobe.acs.commons.workflow.synthetic.granite.WrappedSyntheticWorkItem;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowRunnerImpl;
import com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticRestartWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticTerminateWorkflowException;
import com.adobe.granite.workflow.exec.Route;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.filter.WorkItemFilter;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.oak.jcr.session.SessionImpl;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticWorkflowSessionTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Mock
    ResourceResolverFactory resourceResolverFactory;

    @InjectMocks
    SyntheticWorkflowSession session;

    @Mock
    SyntheticWorkflowData workflowData;

    @Mock
    SyntheticWorkflow syntheticWorkflow;

    @Mock
    WorkItem wrappedWorkItem;

    @Mock
    WorkItemFilter workItemFilter;

    @Mock
    Route route;

    private static ResourceResolver resourceResolver;

    @Before
    public void setUp() throws Exception {
        resourceResolver = context.resourceResolver();
        session = SyntheticWorkflowSession.createSyntheticWorkflowSession(new SyntheticWorkflowRunnerImpl(), resourceResolver.adaptTo(Session.class));
        workflowData = new SyntheticWorkflowData("JCR_PATH", "/content/test");
        when(resourceResolverFactory.getResourceResolver(anyMap())).thenReturn(resourceResolver);
        //todo: come back and fix the resourceResolverFactory being null when being called in test_getModel
        workflowData.getMetaDataMap().put("cat", "meow");
        workflowData.getMetaDataMap().put("bird", "ka-kaw");

        syntheticWorkflow = new SyntheticWorkflow("test", workflowData);
        SyntheticWorkItem syntheticWorkItem = SyntheticWorkItem.createSyntheticWorkItem(syntheticWorkflow.getWorkflowData());
        wrappedWorkItem = (WorkItem) Proxy.newProxyInstance(WrappedSyntheticWorkItem.class.getClassLoader(), new Class[] { WorkItem.class, WrappedSyntheticWorkItem.class  }, syntheticWorkItem);
        SyntheticRoute syntheticRoute = new SyntheticRoute(false);
        route = (Route) syntheticRoute;
    }

    @Test(expected = SyntheticTerminateWorkflowException.class)
    public void test_terminateWorkflow() throws Exception {
        session.terminateWorkflow(syntheticWorkflow);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_complete() throws Exception {
        session.complete(wrappedWorkItem, route);
    }

    @Test(expected = SyntheticRestartWorkflowException.class)
    public void test_restartWorkflow() throws Exception {
        session.restartWorkflow(syntheticWorkflow);
    }

    @Test
    public void test_isSuperuser() throws Exception {
        Assert.assertTrue(session.isSuperuser());
    }

    @Test(expected = NullPointerException.class)
    public void test_getModel() throws Exception {
        Object[] objects = new Object[]{ new String("test")};
        session.getModel(objects);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getModelMultipleArgs() throws Exception {
        Object[] objects = new Object[]{ new String("test"), new String("testagain")};
        session.getModel(objects);
    }

    @Test
    public void test_getRoutes() throws Exception {
        Assert.assertEquals(session.getRoutes().getClass(), ArrayList.class);
    }

    @Test
    public void test_getBackRoutes() throws Exception {
        Assert.assertEquals(session.getBackRoutes().getClass(), ArrayList.class);
    }

    @Test
    public void test_newWorkflowData() throws Exception {
        session.newWorkflowData("test", "test");
    }

    @Test
    public void test_updateWorkflowData() throws Exception {
        WorkflowData workflowData = session.newWorkflowData("newtesttype", "newtest");
        session.updateWorkflowData(syntheticWorkflow, workflowData);
        Assert.assertEquals(syntheticWorkflow.getWorkflowData().getPayload(), "newtest");
        Assert.assertEquals(syntheticWorkflow.getWorkflowData().getPayloadType(), "newtesttype");
    }

    @Test
    public void test_adaptTo() throws Exception {
        Session newSession = (Session) session.adaptTo(Session.class);
        Assert.assertEquals(newSession.getClass(), SessionImpl.class);
    }

    @Test
    public void test_adaptToNull() throws Exception {
        Assert.assertNull(session.adaptTo(ResourceResolver.class));
    }



}
