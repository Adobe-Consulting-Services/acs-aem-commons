/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSync;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled Service that runs the Remote Assets node sync.
 *
 * This job will sync asset nodes based on OSGi configuration.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = Runnable.class,
        property = {
                "scheduler.concurrent:Boolean=false"
        }
)
@Designate(ocd = RemoteAssetsNodeSyncScheduler.Config.class)
public class RemoteAssetsNodeSyncScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetsNodeSyncScheduler.class);

    @ObjectClassDefinition(name = "ACS AEM Commons - Remote Assets Sync Scheduler",
            description = "Scheduled Service that runs the Remote Assets node sync.")
    public @interface Config {
        String DEFAULT_SCHEDULER_EXPRESSION = "0 0 0,4,8,12,16,20 ? * *";

        @AttributeDefinition(
                name = "Cron expression defining when this Scheduled Service will run",
                description = "Default value ('0 0 0,4,8,12,16,20 ? * *') will run this job every 4 hours starting at 00:00.",
                defaultValue = DEFAULT_SCHEDULER_EXPRESSION
        )
        String scheduler_expression() default DEFAULT_SCHEDULER_EXPRESSION;
    }

    @Reference
    private RemoteAssetsNodeSync remoteAssetsNodeSync;

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
