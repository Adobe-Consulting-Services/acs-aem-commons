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
import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSyncTriggerMBean;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

/**
 * Manages the Remote Asset Node Sync's syncAsset JMX trigger.
 */
@Component( immediate = true )
@Service
@Properties({ @Property(name = "jmx.objectname", value = "com.adobe.acs.commons:type=Remote Asset Node Sync") })
public class RemoteAssetsNodeSyncTrigger implements RemoteAssetsNodeSyncTriggerMBean {

    @Reference
    private RemoteAssetsNodeSync assetNodeSyncService;

    /**
     * @see RemoteAssetsNodeSyncTriggerMBean#syncAssetNodes().
     */
    @Override
    public void syncAssetNodes() {
        this.assetNodeSyncService.syncAssetNodes();
    }
}
