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
import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSyncTriggerMBean;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.management.NotCompliantMBeanException;

/**
 * Manages the Remote Asset Node Sync's syncAsset JMX trigger.
 */
@Component(
        service = RemoteAssetsNodeSyncTriggerMBean.class,
        property= {
                "jmx.objectname=com.adobe.acs.commons:type=Remote Asset Node Sync"
        }
)
public class RemoteAssetsNodeSyncTrigger extends AnnotatedStandardMBean implements RemoteAssetsNodeSyncTriggerMBean {

    @Reference
    private RemoteAssetsNodeSync assetNodeSyncService;

    public RemoteAssetsNodeSyncTrigger() throws NotCompliantMBeanException {
        super(RemoteAssetsNodeSyncTriggerMBean.class);
    }

    /**
     * @see RemoteAssetsNodeSyncTriggerMBean#syncAssetNodes().
     */
    @Override
    public void syncAssetNodes() {
        this.assetNodeSyncService.syncAssetNodes();
    }
}
