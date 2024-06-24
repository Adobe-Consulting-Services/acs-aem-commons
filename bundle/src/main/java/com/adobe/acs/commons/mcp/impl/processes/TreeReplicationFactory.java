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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.util.RequireAem;
import com.day.cq.replication.Replicator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(service = {ProcessDefinitionFactory.class})
public class TreeReplicationFactory extends ProcessDefinitionFactory<TreeReplication> {

    // Disable this feature on AEM as a Cloud Service
    @Reference(target = "(distribution=classic)")
    transient RequireAem requireAem;

    @Reference
    transient Replicator replicator;


    @Override
    public String getName() {
        return "Tree Activation";
    }

    @Override
    protected TreeReplication createProcessDefinitionInstance() {
        return new TreeReplication(replicator);
    }

}
