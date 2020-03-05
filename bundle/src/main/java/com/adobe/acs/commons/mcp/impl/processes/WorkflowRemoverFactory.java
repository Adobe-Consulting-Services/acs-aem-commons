/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.acs.commons.mcp.AdministratorsOnlyProcessDefinitionFactory;
import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;

@Component(service = ProcessDefinitionFactory.class)
public class WorkflowRemoverFactory extends AdministratorsOnlyProcessDefinitionFactory<WorkflowRemover> {

    private static final String NAME = "Workflow Remover";

    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public WorkflowRemover createProcessDefinitionInstance() {
        return new WorkflowRemover(workflowInstanceRemover);
    }
}
