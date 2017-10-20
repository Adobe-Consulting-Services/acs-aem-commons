/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.redirectmaps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.query.Query;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;

@Model(adaptables = Resource.class)
public class RedirectMapModel {

	static final Logger log = LoggerFactory.getLogger(RedirectMapModel.class);

	@Inject
	@Named("redirectMap.txt")
	private Resource redirectMap;

	@Inject
	private List<RedirectConfigModel> redirects;

	@Inject
	@Source("sling-object")
	private ResourceResolver resourceResolver;

	private List<MapEntry> addItems(RedirectConfigModel config, Iterator<Resource> items, StringBuilder sb,
			String suffix) {
		List<MapEntry> invalidEntries = new ArrayList<MapEntry>();
		while (items.hasNext()) {
			Resource item = items.next();
			String path = item.getPath();
			ValueMap properties = item.getChild(JcrConstants.JCR_CONTENT).getValueMap();
			FakeSlingHttpServletRequest mockRequest = new FakeSlingHttpServletRequest(resourceResolver,
					config.getProtocol(), config.getDomain(), (config.getProtocol().equals("https") ? 443 : 80));
			String pageUrl = config.getProtocol() + "://" + config.getDomain()
					+ resourceResolver.map(mockRequest, item.getPath() + suffix);

			String[] sources = properties.get(config.getProperty(), String[].class);
			for (String source : sources) {
				MapEntry entry = new MapEntry(item, source, pageUrl);
				if (!entry.isValid()) {
					log.warn("Source path {} for content {} contains whitespace", entry.getSource(), path);
					invalidEntries.add(entry);
				} else {
					sb.append(entry.getSource() + " " + entry.getTarget() + "\n");
				}
			}
		}
		return invalidEntries;
	}

	private List<MapEntry> gatherEntries(RedirectConfigModel config, StringBuilder sb) {

		List<MapEntry> invalidEntries = new ArrayList<MapEntry>();

		sb.append("\n# Dynamic entries for " + config.getResource().getPath() + "\n");

		String pageQuery = "SELECT * FROM [cq:Page] WHERE [jcr:content/" + config.getProperty()
				+ "] IS NOT NULL AND (ISDESCENDANTNODE([" + config.getPath() + "]) OR [jcr:path]='" + config.getPath()
				+ "')";
		log.debug("Finding pages with redirects with query: {}", pageQuery);
		invalidEntries.addAll(addItems(config, resourceResolver.findResources(pageQuery, Query.JCR_SQL2), sb, ".html"));
		String assetQuery = "SELECT * FROM [dam:Asset] WHERE [jcr:content/" + config.getProperty()
				+ "] IS NOT NULL AND (ISDESCENDANTNODE([" + config.getPath() + "]) OR [jcr:path]='" + config.getPath()
				+ "')";
		log.debug("Finding assets with redirects with query: {}", assetQuery);
		invalidEntries.addAll(addItems(config, resourceResolver.findResources(assetQuery, Query.JCR_SQL2), sb, ""));
		return invalidEntries;
	}

	public List<MapEntry> getInvalidEntries() {
		List<MapEntry> invalidEntries = new ArrayList<MapEntry>();
		StringBuilder sb = new StringBuilder();
		for (RedirectConfigModel config : redirects) {
			invalidEntries.addAll(gatherEntries(config, sb));
		}
		return invalidEntries;
	}

	public String getRedirectMap() throws IOException {
		log.debug("Retrieving redirect map from {}", redirectMap);

		StringBuilder sb = new StringBuilder();

		sb.append("# Redirect Map File\n");
		InputStream is = redirectMap.adaptTo(InputStream.class);
		sb.append(IOUtils.toString(is));

		for (RedirectConfigModel config : redirects) {
			gatherEntries(config, sb);
		}
		return sb.toString();
	}

}
