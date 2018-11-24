/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.workflow.process.impl;

import org.apache.commons.lang.ArrayUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.wcm.workflow.process.DeactivatePageProcess;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

@Component(service=WorkflowProcess.class)
@Designate(ocd=ParameterizedDeactivatePageProcess.Config.class)
public class ParameterizedDeactivatePageProcess extends DeactivatePageProcess {
	
    @ObjectClassDefinition( name = "ACS AEM Commons - Workflow Process - Parameterized Deactivate Resource",
            description = "Triggers a deactivation replication event, but only to specifically configured agents.")
    public @interface Config {
       @AttributeDefinition(defaultValue = {
             "Parameterized Deactivate Resource Process" }, name = "Workflow Label", description = "Triggers a deactivation replication event, but only to specifically configured agents.")
       String process_label();
    }


    private static final String AGENT_ARG = "replicationAgent";

    private transient ThreadLocal<String[]> agentId = new ThreadLocal<String[]>();

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {
        agentId.set(args.get(AGENT_ARG, new String[] {}));
        super.execute(workItem, workflowSession, args);

    }

    @Override
    protected ReplicationOptions prepareOptions(ReplicationOptions opts) {

        if (opts == null) {
            opts = new ReplicationOptions();
        }
        opts.setFilter(new AgentFilter() {

            @Override
            public boolean isIncluded(Agent agent) {

                if (ArrayUtils.isEmpty(agentId.get())) {
                    return false;
                }
                return ArrayUtils.contains(agentId.get(), agent.getId());
            }
        });
        return opts;
    }

}