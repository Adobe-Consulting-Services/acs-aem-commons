/*
 * #%L
 * ACS AEM Tools Bundle - Automatic Package Replicator
 * %%
 * Copyright (C) 2017 - Dan Klco
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
package com.adobe.acs.commons.automatic_package_replicator;

import java.util.List;

import org.apache.sling.api.resource.LoginException;

import com.adobe.granite.jmx.annotation.Description;

/**
 * MBean interface for interacting with the Automatic Package Replicator
 */
@Description("MBean for managing the Automatic Package Replicator.")
public interface AutomaticPackageReplicatorMBean {

	@Description("Executes the automatic package replication configuration with the specified id")
	void execute(String id);

	@Description("Gets the automatic package replication configurations currently registered")
	List<String> getRegisteredConfigurations();

	@Description("Refreshes the cache of registered automatic package replication configurations")
	void refreshCache() throws LoginException;
}
