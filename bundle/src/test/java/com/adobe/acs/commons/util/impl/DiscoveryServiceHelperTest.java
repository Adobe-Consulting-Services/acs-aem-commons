/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2021 Adobe
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
package com.adobe.acs.commons.util.impl;

import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.TopologyEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.adobe.acs.commons.util.ClusterLeader;


@RunWith(MockitoJUnitRunner.class)
public class DiscoveryServiceHelperTest {
    private DiscoveryServiceHelper helper;
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DiscoveryService discoveryService;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ServiceRegistration<ClusterLeader> serviceRegistration;

    @Before
    public void setUp() {
        helper = new DiscoveryServiceHelper();
        helper.discoveryService = discoveryService;
        Mockito.when(bundleContext.registerService(Mockito.eq(ClusterLeader.class), Mockito.any(ClusterLeader.class), Mockito.isNull())).thenReturn(serviceRegistration);
    }

    @Test
    public void testActivateOnLeader() {
        Mockito.when(discoveryService.getTopology().getLocalInstance().isLeader()).thenReturn(true);
        helper.activate(bundleContext);
        Assert.assertNotNull(helper.clusterLeaderServiceRegistration);
        helper.deactivate();
        Assert.assertNull(helper.clusterLeaderServiceRegistration);
    }

    @Test
    public void testActivateNotOnLeader() {
        Mockito.when(discoveryService.getTopology().getLocalInstance().isLeader()).thenReturn(false);
        helper.activate(bundleContext);
        Mockito.verify(bundleContext, Mockito.never()).registerService(Mockito.eq(ClusterLeader.class), Mockito.any(ClusterLeader.class), Mockito.isNull());
    }

    @Test
    public void testRegisterUnregister() {
        Mockito.when(discoveryService.getTopology().getLocalInstance().isLeader()).thenReturn(false);
        helper.activate(bundleContext);
        Assert.assertNull(helper.clusterLeaderServiceRegistration);

        TopologyEvent topologyEvent = Mockito.mock(TopologyEvent.class, Answers.RETURNS_DEEP_STUBS);
        helper.handleTopologyEvent(topologyEvent); // make sure not NPE is thrown here

        Mockito.when(topologyEvent.getNewView().getLocalInstance().isLeader()).thenReturn(true);
        helper.handleTopologyEvent(topologyEvent); // now become leader
        Mockito.verify(bundleContext).registerService(Mockito.eq(ClusterLeader.class), Mockito.any(ClusterLeader.class), Mockito.isNull());
        Assert.assertEquals(serviceRegistration, helper.clusterLeaderServiceRegistration);

        Mockito.when(topologyEvent.getNewView().getLocalInstance().isLeader()).thenReturn(false);
        helper.handleTopologyEvent(topologyEvent); // now become non-leader
        Assert.assertNull(helper.clusterLeaderServiceRegistration);
    }
}
