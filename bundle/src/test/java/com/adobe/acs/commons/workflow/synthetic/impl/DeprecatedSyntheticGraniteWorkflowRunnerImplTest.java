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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses.NoNextWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses.ReadDataWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses.RestartWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses.SetDataWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses.TerminateDataWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses.UpdateWorkflowDataWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses.WfArgsWorkflowProcess;
import com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses.WfDataWorkflowProcess;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeprecatedSyntheticGraniteWorkflowRunnerImplTest {

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
    public void testExecute_WfData() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "test");
        swr.bindGraniteWorkflowProcesses(new WfDataWorkflowProcess(), map);

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
        swr.bindGraniteWorkflowProcesses(new SetDataWorkflowProcess(), map);

        map.put("process.label", "read");
        swr.bindGraniteWorkflowProcesses(new ReadDataWorkflowProcess(), map);


        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        swr.execute(resourceResolver,
                "/content/test",
                new String[] {"set", "read"},
                metadata, false, false);
    }

    @Test
    public void testExecute_updateWorkflowData() throws Exception {
        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "update");
        swr.bindGraniteWorkflowProcesses(new UpdateWorkflowDataWorkflowProcess(), map);

        map.put("process.label", "read");
        swr.bindGraniteWorkflowProcesses(new ReadDataWorkflowProcess(), map);

        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

        swr.execute(resourceResolver,
                "/content/test",
                new String[] {"update", "read"},
                metadata, false, false);
    }

    @Test
    public void testExecute_ProcessArgs() throws Exception {
        Map<String, Object> wfArgs = new HashMap<String, Object>();
        wfArgs.put("hello", "world");

        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("process.label", "wf-args");
        swr.bindGraniteWorkflowProcesses(new WfArgsWorkflowProcess(wfArgs), map);

        /** WF Process Metadata */

        Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();
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
        swr.bindGraniteWorkflowProcesses(restartWorkflowProcess, map);

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

        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "terminate");
        TerminateDataWorkflowProcess terminateDataWorkflowProcess = spy(new TerminateDataWorkflowProcess());
        swr.bindGraniteWorkflowProcesses(terminateDataWorkflowProcess, map);

        map.put("process.label", "nonext");
        swr.bindGraniteWorkflowProcesses(new NoNextWorkflowProcess(), map);

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
        swr.bindGraniteWorkflowProcesses(terminateDataWorkflowProcess, map);

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

        Map<Object, Object> map = new HashMap<Object, Object>();

        map.put("process.label", "terminate");
        TerminateDataWorkflowProcess terminateDataWorkflowProcess = spy(new TerminateDataWorkflowProcess());
        swr.bindGraniteWorkflowProcesses(terminateDataWorkflowProcess, map);

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