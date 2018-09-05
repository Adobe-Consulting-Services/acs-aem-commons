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
package com.adobe.acs.commons.mcp;

import aQute.bnd.annotation.ConsumerType;
import com.adobe.acs.commons.mcp.form.FormProcessor;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Describes a process and provides a builder which creates the process
 */
@ConsumerType
public abstract class ProcessDefinition implements FormProcessor {

    String name;

    public final void setName(String n) {
        name = n;
    }

    public final String getName() {
        return name;
    }

    public abstract void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException;

    public abstract void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException;
}
