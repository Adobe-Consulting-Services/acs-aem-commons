/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.model.WorkflowModel;
import org.apache.lucene.analysis.util.CharArrayMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component(service = ProcessDefinitionFactory.class)
public class BulkWorkflowFactory extends ProcessDefinitionFactory<BulkWorkflow> {
    private static final Logger log = LoggerFactory.getLogger(BulkWorkflowFactory.class);

    @Reference
    private QueryHelper queryHelper;

    @Reference
    private SyntheticWorkflowRunner syntheticWorkflowRunner;

    @Override
    public String getName() {
        return BulkWorkflow.PROCESS_NAME;
    }

    @Override
    public BulkWorkflow createProcessDefinitionInstance() {
        return new BulkWorkflow(queryHelper, syntheticWorkflowRunner);
    }

    /**
     * Selector that lists available Workflow Models in alphabetical order by Title. The selection value is the Workflow Model ID.
     */
    public static class WorkflowModelSelector extends SelectComponent {
        @Override
        public Map<String, String> getOptions() {
            Map<String, String> options = new TreeMap<>();

            final ResourceResolver resourceResolver = getHelper().getRequest().getResourceResolver();
            final WorkflowSession workflowSession = resourceResolver.adaptTo(WorkflowSession.class);

            try {
                options = Arrays.stream(workflowSession.getModels())
                        .collect(Collectors.toMap(
                                WorkflowModel::getId,
                                WorkflowModel::getTitle))
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> e.getValue(),
                                (k, v)-> { throw new IllegalArgumentException("cannot merge"); },
                                LinkedHashMap::new));

            } catch (WorkflowException e) {
                log.error("Could not collect workflow models", e);
            }

            return options;
        }
    }
}
