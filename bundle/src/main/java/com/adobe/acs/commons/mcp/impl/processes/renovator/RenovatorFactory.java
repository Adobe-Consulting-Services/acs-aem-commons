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
package com.adobe.acs.commons.mcp.impl.processes.renovator;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.day.cq.audit.AuditLog;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.PageManagerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(ProcessDefinitionFactory.class)
public class RenovatorFactory extends ProcessDefinitionFactory<Renovator> {

    @Reference
    PageManagerFactory pageManagerFactory;

    @Reference
    Replicator replicator;

    @Reference
    AuditLog auditLog;

    @Override
    public String getName() {
        return "Renovator";
    }

    @Override
    public Renovator createProcessDefinitionInstance() {
        return new Renovator(pageManagerFactory, replicator, auditLog);
    }
    
    /**
     * Used to inject mock services
     * @param factory mock factory
     */
    public void setPageManagerFactory(PageManagerFactory factory) {
        pageManagerFactory = factory;
    }
    
    /**
     * Used to inject mock services
     * @param replicator mock replicator service
     */
    public void setReplicator(Replicator replicator) {
        this.replicator = replicator;
    }

    /**
     * Used to inject mock services
     * @param auditLog mock audit log service
     */
    public void setAuditLog(AuditLog auditLog) {
        this.auditLog = auditLog;
    }
}
