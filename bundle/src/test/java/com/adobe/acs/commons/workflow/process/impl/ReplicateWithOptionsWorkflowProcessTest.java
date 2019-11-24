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
package com.adobe.acs.commons.workflow.process.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.replication.BrandPortalAgentFilter;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.day.cq.replication.Agent;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;

import io.wcm.testing.mock.aem.junit.AemContext;

public class ReplicateWithOptionsWorkflowProcessTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    private WorkflowPackageManager workflowPackageManager;

    @Mock
    private Replicator replicator;

    @Mock
    private ThrottledTaskRunner throttledTaskRunner;

    @Mock
    private WorkflowHelper workflowHelper;

    @Mock
    private WorkItem workItem;

    @Mock
    private WorkflowData workflowData;

    @Mock
    private WorkflowSession workflowSession;

    @Mock
    private MetaDataMap metaDataMap;

    @InjectMocks
    private ReplicateWithOptionsWorkflowProcess process = new ReplicateWithOptionsWorkflowProcess();

    @Before
    public void setup() throws Exception {
        context.load().json(getClass().getResourceAsStream("ReplicateWithOptionsWorkflowProcessTest.json"), "/content");

        when(workflowHelper.getResourceResolver(workflowSession)).thenReturn(context.resourceResolver());
        when(workItem.getWorkflowData()).thenReturn(workflowData);

        when(workflowData.getPayload()).thenReturn("/content/payload");
    }

    @Test
    public void testExecuteDeactivateWithoutTraversal() throws Exception {
        when(workflowPackageManager.getPaths(context.resourceResolver(), "/content/payload"))
                .thenReturn(Arrays.asList("/content/page", "/content/asset"));

        StringBuilder args = new StringBuilder();
        args.append("replicationActionType=Deactivate");
        args.append(System.lineSeparator());
        args.append("agents=agent1");

        when(metaDataMap.get(WorkflowHelper.PROCESS_ARGS, "")).thenReturn(args.toString());
        process.execute(workItem, workflowSession, metaDataMap);

        ReplicationOptionsMatcher optionsMatcher = new ReplicationOptionsMatcher().withAgentIdFilter("agent1");

        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/page"), argThat(optionsMatcher));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/asset"), argThat(optionsMatcher));
        verifyNoMoreInteractions(replicator);
        verifyNoInteractions(throttledTaskRunner);
    }

    @Test
    public void testExecuteDeactivateWithThrottling() throws Exception {
        when(workflowPackageManager.getPaths(context.resourceResolver(), "/content/payload"))
                .thenReturn(Arrays.asList("/content/page", "/content/asset"));

        StringBuilder args = new StringBuilder();
        args.append("replicationActionType=Deactivate");
        args.append(System.lineSeparator());
        args.append("agents=agent1");
        args.append(System.lineSeparator());
        args.append("throttle=true");

        when(metaDataMap.get(WorkflowHelper.PROCESS_ARGS, "")).thenReturn(args.toString());
        process.execute(workItem, workflowSession, metaDataMap);

        ReplicationOptionsMatcher optionsMatcher = new ReplicationOptionsMatcher().withAgentIdFilter("agent1");

        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/page"), argThat(optionsMatcher));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/asset"), argThat(optionsMatcher));
        verify(throttledTaskRunner, times(2)).waitForLowCpuAndLowMemory();
        verifyNoMoreInteractions(replicator, throttledTaskRunner);
    }

    @Test
    public void testExecuteDeactivateWithTraversal() throws Exception {
        when(workflowPackageManager.getPaths(context.resourceResolver(), "/content/payload"))
                .thenReturn(Arrays.asList("/content/page", "/content/asset"));

        StringBuilder args = new StringBuilder();
        args.append("replicationActionType=Deactivate");
        args.append(System.lineSeparator());
        args.append("agents=agent1");
        args.append(System.lineSeparator());
        args.append("traverseTree=true");

        when(metaDataMap.get(WorkflowHelper.PROCESS_ARGS, "")).thenReturn(args.toString());
        process.execute(workItem, workflowSession, metaDataMap);

        ReplicationOptionsMatcher optionsMatcher = new ReplicationOptionsMatcher().withAgentIdFilter("agent1");

        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/page"), argThat(optionsMatcher));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/page/child1"), argThat(optionsMatcher));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/page/child2"), argThat(optionsMatcher));
        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/asset"), argThat(optionsMatcher));
        verifyNoMoreInteractions(replicator);
        verifyNoInteractions(throttledTaskRunner);
    }

    @Test(expected = WorkflowException.class)
    public void testExceptionWithNoType() throws Exception {
        StringBuilder args = new StringBuilder();
        args.append("agents=agent1");
        args.append(System.lineSeparator());
        args.append("traverseTree=true");

        when(metaDataMap.get(WorkflowHelper.PROCESS_ARGS, "")).thenReturn(args.toString());
        process.execute(workItem, workflowSession, metaDataMap);
    }

    @Test
    public void testExecuteBrandPortal() throws Exception {
        when(workflowPackageManager.getPaths(context.resourceResolver(), "/content/payload"))
                .thenReturn(Arrays.asList("/content/asset"));

        StringBuilder args = new StringBuilder();
        args.append("replicationActionType=Deactivate");
        args.append(System.lineSeparator());
        args.append("agents=BRAND_PORTAL_AGENTS");

        when(metaDataMap.get(WorkflowHelper.PROCESS_ARGS, "")).thenReturn(args.toString());
        process.execute(workItem, workflowSession, metaDataMap);

        ReplicationOptionsMatcher optionsMatcher = new ReplicationOptionsMatcher().withBrandPortalFilter();

        verify(replicator).replicate(any(), eq(ReplicationActionType.DEACTIVATE), eq("/content/asset"), argThat(optionsMatcher));
        verifyNoMoreInteractions(replicator);
        verifyNoInteractions(throttledTaskRunner);
    }

    private static class ReplicationOptionsMatcher implements ArgumentMatcher<ReplicationOptions> {

        private String filterAgentId;
        private boolean brandPortalFilter;

        public ReplicationOptionsMatcher withAgentIdFilter(String filterAgentId) {
            this.filterAgentId = filterAgentId;
            return this;
        }

        public ReplicationOptionsMatcher withBrandPortalFilter() {
            this.brandPortalFilter = true;
            return this;
        }

        @Override
        public boolean matches(ReplicationOptions argument) {
            ReplicationOptions options = (ReplicationOptions) argument;
            boolean matches = true;
            if (filterAgentId != null) {
                Agent agent = mock(Agent.class);
                when(agent.getId()).thenReturn(filterAgentId);
                matches = matches && options.getFilter().isIncluded(agent);
            }
            if (brandPortalFilter) {
                matches = matches && options.getFilter() instanceof BrandPortalAgentFilter;
            }
            return matches;
        }

    }
}