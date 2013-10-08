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

import com.day.cq.jcrclustersupport.ClusterAware;

/**
 * Abstact base class for scheduled job to be run only on the cluster master.
 */
public abstract class RunnableOnMaster implements ClusterAware, Runnable {

    private boolean isMaster;

    /**
     * Run the scheduled job.
     */
    protected abstract void runOnMaster();

    /**
     * {@inheritDoc}
     */
    @Override
    public final void bindRepository(String repositoryId, String clusterId, boolean master) {
        this.isMaster = master;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void unbindRepository() {
        isMaster = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void run() {
        if (isMaster) {
            runOnMaster();
        }
    }

}
