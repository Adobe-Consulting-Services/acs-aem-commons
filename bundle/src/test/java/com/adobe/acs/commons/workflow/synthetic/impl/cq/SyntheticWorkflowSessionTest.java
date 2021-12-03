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

package com.adobe.acs.commons.workflow.synthetic.impl.cq;

import com.adobe.acs.commons.workflow.synthetic.cq.WrappedSyntheticWorkItem;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowRunnerImpl;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticRestartWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticTerminateWorkflowException;
import com.day.cq.workflow.exec.Route;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.filter.WorkItemFilter;
import com.day.cq.workflow.model.WorkflowModel;
import com.day.cq.workflow.model.WorkflowModelFilter;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticWorkflowSessionTest {
    private static final String UNSUPPORTED_EXCEPTION = "Operation not supported by Synthetic Workflow";

    @Mock
    ResourceResolver resourceResolver;

    @Mock
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

    @Mock
    WorkflowModelFilter workflowModelFilter;

    @Before
    public void setUp() {
        session = new SyntheticWorkflowSession(new SyntheticWorkflowRunnerImpl(), resourceResolver.adaptTo(Session.class));
        workflowData = new SyntheticWorkflowData("JCR_PATH", "/content/test");

        workflowData.getMetaDataMap().put("cat", "meow");
        workflowData.getMetaDataMap().put("bird", "ka-kaw");

        syntheticWorkflow = new SyntheticWorkflow("test", workflowData);
        SyntheticWorkItem syntheticWorkItem = SyntheticWorkItem.createSyntheticWorkItem(syntheticWorkflow.getWorkflowData());
        wrappedWorkItem = (WorkItem) Proxy.newProxyInstance(WrappedSyntheticWorkItem.class.getClassLoader(), new Class[] { WorkItem.class, WrappedSyntheticWorkItem.class  }, syntheticWorkItem);
        SyntheticRoute syntheticRoute = new SyntheticRoute(false);
        route = (Route) syntheticRoute;

        workItemFilter = new WorkItemFilter() {
            @Override
            public boolean doInclude(WorkItem workItem) {
                return false;
            }
        };

        workflowModelFilter = new WorkflowModelFilter() {
            @Override
            public boolean doInclude(WorkflowModel workflowModel) {
                return false;
            }
        };
    }

    @Test
    public void test_updateWorkflowData() throws Exception {
        workflowData.getMetaDataMap().put("dog", "woof");

        session.updateWorkflowData(syntheticWorkflow, workflowData);

        // This test is a bit strange since the maps should always be in sync; this updateWorkflowData simply updates itself with itself
        // This is to mimic CQ Workflow behavior which has to manage this persistence via JCR nodes

        Assert.assertEquals(3, workflowData.getMetaDataMap().size());
        Assert.assertEquals("woof", workflowData.getMetaDataMap().get("dog"));
    }

    @Test
    public void test_getWorkflowService() throws Exception {
        Assert.assertNotNull(session.getWorkflowService());
    }

    @Test
    public void test_getSession() throws Exception {
        Assert.assertNull(session.getSession());
    }

    @Test
    public void test_getUser() throws Exception {
        Assert.assertNull(session.getUser());
    }

    @Test
    public void test_isSuperuser() throws Exception {
        Assert.assertTrue(session.isSuperuser());
    }

    @Test
    public void test_getRoutes() throws Exception {
        Assert.assertNotNull(session.getRoutes(wrappedWorkItem));
    }

    @Test
    public void test_getRoutesWithBoolean() throws Exception {
        Assert.assertNotNull(session.getRoutes(wrappedWorkItem, false));
    }

    @Test
    public void test_getBackRoutes() throws Exception {
        Assert.assertNotNull(session.getBackRoutes(wrappedWorkItem));
    }

    @Test
    public void test_getBackRoutesWithBoolean() throws Exception {
        Assert.assertNotNull(session.getBackRoutes(wrappedWorkItem, false));
    }

    @Test
    public void test_newWorkflowData() throws Exception {
        Assert.assertNotNull(session.newWorkflowData("test", ""));
    }

    @Test
    public void test_getDelegatees() throws Exception {
        Assert.assertNotNull(session.getDelegatees(wrappedWorkItem));
    }

    @Test(expected = SyntheticTerminateWorkflowException.class)
    public void test_terminateWorkflow() throws Exception {
        final SyntheticWorkflowData workflowData = new SyntheticWorkflowData("JCR_PATH", "/content/test");

        workflowData.getMetaDataMap().put("cat", "meow");
        workflowData.getMetaDataMap().put("bird", "ka-kaw");

        SyntheticWorkflow workflow = new SyntheticWorkflow("test", workflowData);
        session.terminateWorkflow(workflow);
    }

    // unsupported operations tests
    @Test(expected = UnsupportedOperationException.class)
    public void test_evaluate() throws Exception {
        final SyntheticWorkflowData workflowData = new SyntheticWorkflowData("JCR_PATH", "/content/test");

        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.evaluate(workflowData, "test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_createNewModel() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.createNewModel("test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_createNewModelTwoStrings() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.createNewModel("test", "test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_complete() throws Exception {
        session.complete(wrappedWorkItem, route);
    }

    @Test(expected = SyntheticRestartWorkflowException.class)
    public void test_restartWorkflow() throws Exception {
        session.restartWorkflow(syntheticWorkflow);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_deployModel() throws Exception {
        session.deployModel(syntheticWorkflow.getWorkflowModel());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_deleteModel() throws Exception {
        session.deleteModel("empty");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getModels() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getModels());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getModelsWithFilter() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getModels(workflowModelFilter));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getModelsLongLong() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getModels(1L, 1L));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getModelsLongLongWithFilter() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getModels(1L, 1L, workflowModelFilter));
    }

    @Test(expected = NullPointerException.class)
    public void test_getModel() throws Exception {
        Assert.assertNotNull(session.getModel("test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getModelTwoStrings() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getModel("test", "test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getActiveWorkItems() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getActiveWorkItems());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getActiveWorkItemsTwoLongs() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getActiveWorkItems(1L, 1L));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getActiveWorkItemsTwoLongsWithFilter() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getActiveWorkItems(1L, 1L, workItemFilter));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getAllWorkItems() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getAllWorkItems());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getAllWorkItemsTwoLongs() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getAllWorkItems(1L, 1L));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getWorkItem() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getWorkItem("test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getWorkflows() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getWorkflows(new String[]{}));
    }


    @Test(expected = UnsupportedOperationException.class)
    public void test_getWorkflowsLongLong() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getWorkflows(new String[]{}, 1L, 1L));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getAllWorkflows() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getAllWorkflows());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getWorkflow() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getWorkflow("test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_delegateWorkItem() throws Exception {
        Resource resource = resourceResolver.getResource("/content/test");
        if (resource != null) {
            Authorizable authorizable = resource.adaptTo(Authorizable.class);
            session.delegateWorkItem(wrappedWorkItem, authorizable);
        }
        throw new UnsupportedOperationException("Unsupported & null resource");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_getHistory() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.getHistory(syntheticWorkflow));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_startWorkflow() throws Exception {
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.startWorkflow(syntheticWorkflow.getWorkflowModel(), workflowData));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_startWorkflowWithDictionary() throws Exception {
        Dictionary<String, String> stringStringDictionary = new Hashtable<>();
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.startWorkflow(syntheticWorkflow.getWorkflowModel(), workflowData, stringStringDictionary));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_startWorkflowWithMap() throws Exception {
        Map<String, Object> stringObjectMap = new HashMap<>();
        Assert.assertSame(UNSUPPORTED_EXCEPTION, session.startWorkflow(syntheticWorkflow.getWorkflowModel(), workflowData, stringObjectMap));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_resumeWorkflow() throws Exception {
        session.resumeWorkflow(syntheticWorkflow);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_suspendWorkflow() throws Exception {
        session.suspendWorkflow(syntheticWorkflow);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_logout() throws Exception {
        session.logout();
    }
}