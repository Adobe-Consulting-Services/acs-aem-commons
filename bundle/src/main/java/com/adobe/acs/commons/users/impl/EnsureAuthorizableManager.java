/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.users.impl;

import aQute.bnd.annotation.ProviderType;
import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

@ProviderType
@Description("ACS AEM Commons - Ensure Authorizable MBean")
public interface EnsureAuthorizableManager {

    @Description("Execute all Ensure Service User and Ensure Group configurations")
    void ensureAll();

    @Description("Execute all Ensure Service User and Ensure Group configurations for the provided principal name")
    void ensurePrincipalName(@Name(value="Principal Name")String principalName);
}
