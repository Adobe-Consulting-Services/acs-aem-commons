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
import org.apache.jackrabbit.api.security.user.User;

@ConsumerType
public abstract class ProcessDefinitionFactory<P extends ProcessDefinition> {

    public abstract String getName();

    @SuppressWarnings("squid:S1172")
    public boolean isAllowed(User user) {
        return true;
    }

    public final P createProcessDefinition() {
        P processDefinition = createProcessDefinitionInstance();
        processDefinition.setName(getName());
        return processDefinition;
    }

    protected abstract P createProcessDefinitionInstance();
}
