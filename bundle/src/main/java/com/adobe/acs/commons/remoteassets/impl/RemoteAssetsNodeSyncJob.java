/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSync;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job that will sync asset nodes based on OSGi configuration. Implements {@link Runnable}.
 */
@Component(
        label = "ACS AEM Commons - Remote Assets Sync Job",
        description = "Scheduled Service that runs the Remote Assets node sync.",
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true
)
@Properties({
        @Property(
                label = "Cron expression defining when this Scheduled Service will run",
                name = "scheduler.expression",
                description = "Default value ('0 0,4,8,12,16,20 * * *') will run this job every 4 hours starting at 00:00.",
                value = "0 0,4,8,12,16,20 * * *"
        ),
        @Property(
                name = "scheduler.concurrent",
                boolValue = false,
                propertyPrivate = true
        )
})
@Service
public class RemoteAssetsNodeSyncJob implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetsNodeSyncJob.class);

    @Reference
    private RemoteAssetsNodeSync remoteAssetsNodeSync;

    /**
     * Method to run on activation.
     */
    @Activate
    protected final void activate() {
        // Do nothing.
    }

    /**
     * Method to run on deactivation.
     */
    @Deactivate
    protected void deactivate() {
        // Do nothing.
    }

    /**
     * @see Runnable#run().
     */
    @Override
    public final void run() {
        LOG.info("Remote assets node sync job started.");
        this.remoteAssetsNodeSync.syncAssetNodes();
        LOG.info("Remote assets node sync job finished.");
    }
}
