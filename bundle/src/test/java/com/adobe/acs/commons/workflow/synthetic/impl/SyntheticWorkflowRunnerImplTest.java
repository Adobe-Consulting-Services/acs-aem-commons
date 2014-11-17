/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.impl.testprocesses.NoNextWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.testprocesses.ReadDataWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.testprocesses.RestartWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.testprocesses.SetDataWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.testprocesses.TerminateDataWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.testprocesses.WFArgsWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.testprocesses.WFDataWorkflowProcess;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticWorkflowRunnerImplTest {

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    Session session;

    SyntheticWorkflowRunnerImpl swr = new SyntheticWorkflowRunnerImpl();

    @Before
    public void setUp() {
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    }

    @Test
    public void testExecute_WFData() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "test");
        swr.bindWorkflowProcesses(new WFDataWorkflowProcess(), map);

        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        swr.execute(resourceResolver,
                "/content/test",
                new String[] {"test"},
                null, false, false);
    }

    @Test
    public void testExecute_PassingDataBetweenProcesses() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "set");
        swr.bindWorkflowProcesses(new SetDataWorkflowProcess(), map);

        map.put("process.label", "read");
        swr.bindWorkflowProcesses(new ReadDataWorkflowProcess(), map);


        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        swr.execute(resourceResolver,
                "/content/test",
                new String[] {"set", "read"},
                metadata, false, false);
    }


    @Test
    public void testExecute_ProcessArgs() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "wf-args");
        swr.bindWorkflowProcesses(new WFArgsWorkflowProcess(), map);

        /** WF Process Metadata */

        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        Map<String, Object> wfArgs = new HashMap<String, Object>();
        wfArgs.put("hello", "world");

        metadata.put("wf-args", wfArgs);

        swr.execute(resourceResolver,
                "/content/test",
                new String[]{ "wf-args" },
                metadata, false, false);
    }

    @Test
    public void testExecute_Restart() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "restart");
        RestartWorkflowProcess restartWorkflowProcess = spy(new RestartWorkflowProcess());
        swr.bindWorkflowProcesses(restartWorkflowProcess, map);

        /** Restart */

        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        Map<String, Object> wfArgs = new HashMap<String, Object>();
        metadata.put("restart", wfArgs);

        swr.execute(resourceResolver,
                "/content/test",
                new String[]{ "restart" },
                metadata, false, false);

        verify(restartWorkflowProcess, times(3)).execute(any(WorkItem.class), any(WorkflowSession.class),
                any(MetaDataMap.class));

    }

    @Test
    public void testExecute_Terminate() throws Exception {
        when(session.hasPendingChanges()).thenReturn(true).thenReturn(false);

        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "terminate");
        TerminateDataWorkflowProcess terminateDataWorkflowProcess = spy(new TerminateDataWorkflowProcess());
        swr.bindWorkflowProcesses(terminateDataWorkflowProcess, map);

        map.put("process.label", "nonext");
        swr.bindWorkflowProcesses(new NoNextWorkflowProcess(), map);

        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        Map<String, Object> wfArgs = new HashMap<String, Object>();
        metadata.put("complete", wfArgs);

        swr.execute(resourceResolver,
                "/content/test",
                new String[]{ "terminate", "nonext" },
                metadata, true, false);

    }


    @Test
    public void testExecute_Terminate_autoSaveAtEnd() throws Exception {
        when(session.hasPendingChanges()).thenReturn(true).thenReturn(false);

        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "terminate");
        TerminateDataWorkflowProcess terminateDataWorkflowProcess = spy(new TerminateDataWorkflowProcess());
        swr.bindWorkflowProcesses(terminateDataWorkflowProcess, map);

        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        Map<String, Object> wfArgs = new HashMap<String, Object>();
        metadata.put("terminate", wfArgs);

        swr.execute(resourceResolver,
                "/content/test",
                new String[]{ "terminate" },
                metadata, false, true);

        verify(terminateDataWorkflowProcess, times(1)).execute(any(WorkItem.class), any(WorkflowSession.class),
                any(MetaDataMap.class));

        verify(session, times(1)).save();

    }

    @Test
    public void testExecute_Complete_noSave() throws Exception {
        when(session.hasPendingChanges()).thenReturn(true).thenReturn(false);

        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "terminate");
        TerminateDataWorkflowProcess terminateDataWorkflowProcess = spy(new TerminateDataWorkflowProcess());
        swr.bindWorkflowProcesses(terminateDataWorkflowProcess, map);

        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        Map<String, Object> wfArgs = new HashMap<String, Object>();
        metadata.put("terminate", wfArgs);

        swr.execute(resourceResolver,
                "/content/test",
                new String[]{ "terminate" },
                metadata, false, false);

        verify(terminateDataWorkflowProcess, times(1)).execute(any(WorkItem.class), any(WorkflowSession.class),
                any(MetaDataMap.class));

        verify(session, times(0)).save();

    }
}