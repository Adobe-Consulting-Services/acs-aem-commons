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
package com.adobe.acs.commons.mcp.impl.processes.cfi;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import org.osgi.service.component.annotations.Component;

/**
 * OSGi factory for Content Import utility
 */
@Component(service = ProcessDefinitionFactory.class)
public class ContentFragmentImportFactory extends ProcessDefinitionFactory<ContentFragmentImport> {

    @Override
    public String getName() {
        return "Content Fragment Import";
    }

    @Override
    protected ContentFragmentImport createProcessDefinitionInstance() {
        return new ContentFragmentImport();
    }
}
