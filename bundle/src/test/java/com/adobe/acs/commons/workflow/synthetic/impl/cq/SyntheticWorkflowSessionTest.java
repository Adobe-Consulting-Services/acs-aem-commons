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

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowRunnerImpl;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticWorkflowSessionTest {
    final String UNSUPPORTED_EXCEPTION = "Operation not supported by Synthetic Workflow";

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    SyntheticWorkflowSession session;

    @Before
    public void setUp() {
        session = new SyntheticWorkflowSession(new SyntheticWorkflowRunnerImpl(), resourceResolver.adaptTo(Session.class));
    }

    @Test
    public void test_updateWorkflowData() throws Exception {
        final SyntheticWorkflowData workflowData = new SyntheticWorkflowData("JCR_PATH", "/content/test");

        workflowData.getMetaDataMap().put("cat", "meow");
        workflowData.getMetaDataMap().put("bird", "ka-kaw");

        SyntheticWorkflow workflow = new SyntheticWorkflow("test", workflowData);

        workflowData.getMetaDataMap().put("dog", "woof");

        session.updateWorkflowData(workflow, workflowData);

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
}