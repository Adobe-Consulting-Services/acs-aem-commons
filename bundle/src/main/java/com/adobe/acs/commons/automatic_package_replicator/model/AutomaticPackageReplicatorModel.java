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
package com.adobe.acs.commons.automatic_package_replicator.model;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

//So interesting note, you can't seem to use Sling models in service
// activators...
/**
 * Model for retrieving Automatic Package Replicator configurations
 */
public class AutomaticPackageReplicatorModel {

	private ValueMap properties;

	public AutomaticPackageReplicatorModel(Resource resource) {
		properties = resource.getValueMap();
	}

	public enum TRIGGER {
		cron, event
	}

	public String getTitle() {
		return properties.get("jcr:content", String.class);
	}

	public String getPackagePath() {
		return properties.get("packagePath", String.class);
	}

	public String getCronTrigger() {
		return properties.get("cronTrigger", String.class);
	}

	public String getEventTopic() {
		return properties.get("eventTopic", String.class);
	}

	public String getEventFilter() {
		return properties.get("eventFilter", String.class);
	}

	public TRIGGER getTrigger() {
		return TRIGGER.valueOf(properties.get("trigger", String.class));
	}

}
