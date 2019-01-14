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
package com.adobe.acs.commons.util;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.discovery.TopologyEvent;

import org.apache.sling.discovery.TopologyEventListener;

/**
 * Abstact base class for scheduled job to be run only on the cluster master.
 */
@ConsumerType
public abstract class RunnableOnMaster implements TopologyEventListener, Runnable {

    private volatile boolean isLeader;

    /**
     * Run the scheduled job.
     */
    protected abstract void runOnMaster();

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleTopologyEvent(TopologyEvent te) {
        this.isLeader = te.getNewView().getLocalInstance().isLeader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void run() {
        if (isLeader) {
            runOnMaster();
        }
    }

}
