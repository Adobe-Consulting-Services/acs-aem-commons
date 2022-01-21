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
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Objects;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticWorkflowSessionTest {
    AemContext context;

    SyntheticWorkflowSession session;

    SyntheticWorkflow syntheticWorkflow;

    @Before
    public void setUp() throws Exception {
        context = new AemContext(ResourceResolverType.JCR_MOCK);
        ResourceResolver resourceResolver = context.resourceResolver();
        session = SyntheticWorkflowSession.createSyntheticWorkflowSession(new SyntheticWorkflowRunnerImpl(), resourceResolver.adaptTo(Session.class));
        SyntheticWorkflowData workflowData = new SyntheticWorkflowData("JCR_PATH", "/content/test");
        workflowData.getMetaDataMap().put("cat", "meow");
        workflowData.getMetaDataMap().put("bird", "ka-kaw");

        syntheticWorkflow = new SyntheticWorkflow("test", workflowData);
    }

    @Test(expected = SyntheticTerminateWorkflowException.class)
    public void test_terminateWorkflow() throws Exception {
        session.terminateWorkflow(syntheticWorkflow);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_complete() throws Exception {
        SyntheticWorkItem syntheticWorkItem = SyntheticWorkItem.createSyntheticWorkItem(syntheticWorkflow.getWorkflowData());
        WorkItem wrappedWorkItem = (WorkItem) Proxy.newProxyInstance(WrappedSyntheticWorkItem.class.getClassLoader(), new Class[] { WorkItem.class, WrappedSyntheticWorkItem.class  }, syntheticWorkItem);
        Route route = (Route) new SyntheticRoute(false);
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
        Assert.assertNotNull(session.getRoutes());
    }

    @Test
    public void test_getBackRoutes() throws Exception {
        Assert.assertNotNull(session.getBackRoutes());
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
        Assert.assertEquals(newSession.getClass(), Objects.requireNonNull(context.resourceResolver().adaptTo(Session.class)).getClass());
    }

    @Test (expected = NullPointerException.class) //this is because SyntheticWorkflowRunnerImpl has a null resourceResolverFactory during the test
    public void test_adaptToNull() throws Exception {
        Assert.assertNull(session.adaptTo(ResourceResolver.class));
    }



}
