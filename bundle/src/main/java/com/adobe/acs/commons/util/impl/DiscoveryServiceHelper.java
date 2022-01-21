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

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.adobe.acs.commons.util.ClusterLeader;

/**
 * This component registers a {@link ClusterLeader} service in case the current topology instance is the leader and
 * unregisters that service if that is no longer the case.
 * 
 * Components which are only supposed to be executed on the cluster leader can depend on the 
 * service with marker interface {@link ClusterLeader}.
 */
@Component
public class DiscoveryServiceHelper implements TopologyEventListener {

    private BundleContext bundleContext;

    ServiceRegistration<ClusterLeader> clusterLeaderServiceRegistration;

    @Override
    public void handleTopologyEvent(TopologyEvent event) {
        TopologyView newView = event.getNewView();
        if (newView != null) {
            if (newView.getLocalInstance().isLeader()) {
                registerClusterLeader();
            } else {
                unregisterClusterLeader();
            }
        }
    }

    @Activate
    public synchronized void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        unregisterClusterLeader();
    }

    private synchronized void registerClusterLeader() {
        if (clusterLeaderServiceRegistration == null) {
            clusterLeaderServiceRegistration = bundleContext.registerService(ClusterLeader.class, new ClusterLeader() {}, null);
        }
    }

    private synchronized void unregisterClusterLeader() {
        if (clusterLeaderServiceRegistration != null) {
            clusterLeaderServiceRegistration.unregister();
            clusterLeaderServiceRegistration = null;
        }
    }
}
