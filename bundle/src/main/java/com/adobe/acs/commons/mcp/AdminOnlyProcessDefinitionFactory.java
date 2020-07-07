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

import org.apache.jackrabbit.api.security.user.User;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * ProcessDefinitionFactory which limits availablity of a process to the literal
 * 'admin' user.
 *
 * @param <P> Process definition class
 * @deprecated Please use AdministratorsOnlyProcessDefinitionFactory as it is
 * still sufficiently restrictive but more generally usable in environments
 * where the admin account is impossible to attain. This class will still
 * continue to work but is generally discouraged.
 */
@Deprecated
@ConsumerType
public abstract class AdminOnlyProcessDefinitionFactory<P extends ProcessDefinition> extends ProcessDefinitionFactory<P> {

    @Override
    public boolean isAllowed(User user) {
        return user.isAdmin();
    }
}
