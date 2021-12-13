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
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticMetaDataMap;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowRunnerImpl;
import com.adobe.granite.workflow.exec.InboxItem;
import com.adobe.granite.workflow.exec.Status;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;
import java.lang.reflect.Proxy;
import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticWorkItemTest {
    @Mock
    ResourceResolver resourceResolver;

    @Mock
    SyntheticWorkflowSession session;

    @Mock
    SyntheticWorkflowData workflowData;

    @Mock
    SyntheticWorkflow syntheticWorkflow;

    @Mock
    SyntheticWorkItem syntheticWorkItem;

    @Before
    public void setUp() {
        session = SyntheticWorkflowSession.createSyntheticWorkflowSession(new SyntheticWorkflowRunnerImpl(), resourceResolver.adaptTo(Session.class));
        workflowData = new SyntheticWorkflowData("JCR_PATH", "/content/test");
        workflowData.getMetaDataMap().put("dog", "woof");
        syntheticWorkflow = new SyntheticWorkflow("test", workflowData);
        syntheticWorkItem = SyntheticWorkItem.createSyntheticWorkItem(syntheticWorkflow.getWorkflowData());
    }

    @Test
    public void test_getId() throws Exception {
        Assert.assertNotNull(syntheticWorkItem.getId());
    }

    @Test
    public void test_getTimeStarted() throws Exception {
        Assert.assertNotNull(syntheticWorkItem.getTimeStarted());
    }

    @Test
    public void test_getTimeEnded() throws Exception {
        Assert.assertNull(syntheticWorkItem.getTimeEnded());
    }


    @Test
    public void test_setTimeEnded() throws Exception {
        syntheticWorkItem.setTimeEnded(new Date());
        Assert.assertEquals(syntheticWorkItem.getTimeEnded().getClass(), Date.class);
    }

    @Test
    public void test_getProgressBeginTime() throws Exception {
        Assert.assertNull(syntheticWorkItem.getProgressBeginTime());
    }

    @Test
    public void test_setProgressBeginTime() throws Exception {
        syntheticWorkItem.setProgressBeginTime(new Date());
        Assert.assertEquals(syntheticWorkItem.getProgressBeginTime().getClass(), Date.class);
    }

    @Test
    public void test_getPriority() throws Exception {
        Assert.assertNull(syntheticWorkItem.getPriority());
    }

    @Test
    public void test_setPriority() throws Exception {
        syntheticWorkItem.setPriority(InboxItem.Priority.HIGH);
        Assert.assertEquals(syntheticWorkItem.getPriority(), InboxItem.Priority.HIGH);
    }

    @Test
    public void test_getMetaDataMap() throws Exception {
        Assert.assertEquals(syntheticWorkItem.getMetaDataMap().getClass(), SyntheticMetaDataMap.class);
    }

    @Test
    public void test_updateWorkflowData() throws Exception {
        syntheticWorkItem.setDueTime(new Date());
        Assert.assertNotNull(syntheticWorkItem.getDueTime());
    }

    @Test
    public void test_getWorkflow() throws Exception {
        Assert.assertNull(syntheticWorkItem.getWorkflow());
    }

    @Test
    public void test_setWorkflow() throws Exception {
        WorkItem wrappedWorkItem = (WorkItem) Proxy.newProxyInstance(WrappedSyntheticWorkItem.class.getClassLoader(), new Class[] { WorkItem.class, WrappedSyntheticWorkItem.class }, syntheticWorkItem);
        syntheticWorkItem.setWorkflow(wrappedWorkItem, syntheticWorkflow);
        Workflow workflow = syntheticWorkItem.getWorkflow();
        if (workflow != null) {
            Assert.assertEquals(workflow.getId(), "test");
        }
    }

    @Test
    public void test_getStatus() throws Exception {
        Status status = syntheticWorkItem.getStatus();
        Assert.assertEquals(status, Status.ACTIVE);
    }

    @Test
    public void test_getNode() throws Exception {
        Assert.assertNull(syntheticWorkItem.getNode());
    }

    @Test
    public void test_getItemType() throws Exception {
        Assert.assertEquals(syntheticWorkItem.getItemType(), "Synthetic Workflow");
    }

    @Test
    public void test_getItemSubType() throws Exception {
        Assert.assertNull(syntheticWorkItem.getItemSubType());
    }

    @Test
    public void test_getContentPath() throws Exception {
        Assert.assertEquals(syntheticWorkItem.getContentPath(), "/content/test");
    }

    @Test
    public void test_setDueTime() throws Exception {
        syntheticWorkItem.setDueTime(new Date());
    }
}
