/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.util.mbeans;

import aQute.bnd.annotation.ProviderType;
import com.adobe.granite.jmx.annotation.Description;
import org.apache.sling.api.resource.LoginException;

import java.util.List;

/**
 * Base methods for the JMX Interface for a ResourceServiceManager
 */
@ProviderType
public interface ResourceServiceManagerMBean {

    @Description("Gets the configurations currently registered")
    List<String> getRegisteredConfigurations();

    @Description("Refreshes the cache of registered configurations")
    void refreshCache() throws LoginException;
}
