/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.adobe.acs.commons.redirects;

import org.apache.sling.api.resource.Resource;

public class MapEntry {
	private final Resource resource;
	private final String source;
	private final String target;
	private final boolean valid;

	public MapEntry(Resource resource, String source, String target) {
		source = source.trim();
		if (source.matches(".*\\s.*")) {
			RedirectMapModel.log.warn("Source path {} for content {} contains whitespace", source, resource);
			valid = false;
		} else {
			valid = true;
		}
		this.source = source;
		this.target = target;
		this.resource = resource;

	}

	public Resource getResource() {
		return resource;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public boolean isValid() {
		return valid;
	}

	@Override
	public String toString() {
		return "MapEntry [resource=" + resource + ", source=" + source + ", target=" + target + ", valid=" + valid
				+ "]";
	}
}