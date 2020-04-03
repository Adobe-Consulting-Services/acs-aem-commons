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

import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowRunnerImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticWorkflowSessionTest {
    @Test
    public void test_updateWorkflowData() throws Exception {
        final SyntheticWorkflowSession session = new SyntheticWorkflowSession(new SyntheticWorkflowRunnerImpl(), mock(Session.class));
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
}