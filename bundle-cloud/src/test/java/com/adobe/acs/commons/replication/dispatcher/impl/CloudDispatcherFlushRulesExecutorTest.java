/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2025 Adobe
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
package com.adobe.acs.commons.replication.dispatcher.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.event.DistributionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushRules;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;

@ExtendWith(MockitoExtension.class)
class CloudDispatcherFlushRulesExecutorTest {

    @Mock
    private DiscoveryService discoveryService;
    @Mock
    private TopologyView topologyView;
    @Mock
    private InstanceDescription localInstance;
    @Mock
    private DispatcherFlushRules dispatcherFlushRules;

    private CloudDispatcherFlushRulesExecutor executor;

    @BeforeEach
    void setUp() {
        Mockito.when(discoveryService.getTopology()).thenReturn(topologyView);
        Mockito.when(topologyView.getLocalInstance()).thenReturn(localInstance);
        Mockito.when(localInstance.isLeader()).thenReturn(true);
        executor = new CloudDispatcherFlushRulesExecutor();
        executor.discoveryService = discoveryService;
        executor.dispatcherFlushRules = List.of(dispatcherFlushRules);
    }

    @Test
    void testHandleEvent() throws ReplicationException {
        Event event = new DistributionEvent(
                "packageId",
                "componentName",
                "componentKind",
                "ADD",
                new String[]{"path1", "path2"},
                null).toEvent("topic");
        executor.handleEvent(event);
        ArgumentCaptor<ReplicationAction> actionArgument = ArgumentCaptor.forClass(ReplicationAction.class);
        Mockito.verify(dispatcherFlushRules, Mockito.times(1)).preprocess(actionArgument.capture(), Mockito.any(ReplicationOptions.class));
        assertEquals(ReplicationActionType.ACTIVATE, actionArgument.getValue().getType());
        assertArrayEquals(new String[] {"path1", "path2"}, actionArgument.getValue().getPaths());
    }

    @Test
    void testHandleEventWithDistributionRequestType() throws ReplicationException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("distribution.paths", new String[] {"path1", "path2"});
        // see https://issues.apache.org/jira/browse/SLING-12860
        properties.put("distribution.type", DistributionRequestType.ADD);
        Event event = new Event("topic", properties);
        executor.handleEvent(event);
    }
}
