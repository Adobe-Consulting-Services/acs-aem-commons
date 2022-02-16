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

package com.adobe.acs.commons.replication;

import com.day.cq.replication.Agent;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AgentIdsAgentFilterTest {

    AgentIdsAgentFilter filter;

    @Mock
    Agent agentAcceptOne;

    @Mock
    Agent agentAcceptTwo;

    @Mock
    Agent agentAcceptThree;

    @Before
    public void setUp() throws Exception {
        when(agentAcceptOne.getId()).thenReturn("one");
        when(agentAcceptTwo.getId()).thenReturn("two");
        when(agentAcceptThree.getId()).thenReturn("three");
    }

    @Test
    public void isIncluded() throws Exception {
        filter = new AgentIdsAgentFilter(ImmutableList.<String>builder().add("one").add("two").build());
        assertTrue(filter.isIncluded(agentAcceptOne));
        assertTrue(filter.isIncluded(agentAcceptTwo));
        assertFalse(filter.isIncluded(agentAcceptThree));
    }
}