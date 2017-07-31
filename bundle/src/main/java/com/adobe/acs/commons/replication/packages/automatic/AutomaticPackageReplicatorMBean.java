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
package com.adobe.acs.commons.replication.packages.automatic;

import com.adobe.acs.commons.util.mbeans.ResourceServiceManagerMBean;
import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

import aQute.bnd.annotation.ProviderType;

/**
 * MBean interface for interacting with the Automatic Package Replicator
 */
@ProviderType
@Description("MBean for managing the Automatic Package Replicator.")
public interface AutomaticPackageReplicatorMBean extends ResourceServiceManagerMBean {

	@Description("Executes the automatic package replication configuration with the specified id")
	void execute(@Name(value = "Configuration ID") String id);
}
