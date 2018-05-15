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
package com.adobe.acs.commons.redirectmaps.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirectmaps.models.MapEntry;
import com.adobe.acs.commons.redirectmaps.models.RedirectMapModel;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Utilities for interacting with the redirect entries
 */
public class RedirectEntriesUtils {

	private RedirectEntriesUtils() {
	}

	private static final Gson gson = new Gson();

	private static final Logger log = LoggerFactory.getLogger(RedirectEntriesUtils.class);

	protected static final List<String> readEntries(SlingHttpServletRequest request) throws IOException {
		List<String> lines = null;
		InputStream is = null;
		try {
			is = request.getResource().getChild(RedirectMapModel.MAP_FILE_NODE).adaptTo(InputStream.class);
			lines = IOUtils.readLines(is);
			log.debug("Loaded {} lines", lines.size());
		} finally {
			IOUtils.closeQuietly(is);
		}
		return lines;
	}

	protected static final void updateRedirectMap(SlingHttpServletRequest request, List<String> entries)
			throws PersistenceException {
		ModifiableValueMap mvm = request.getResource().getChild(RedirectMapModel.MAP_FILE_NODE)
				.getChild(JcrConstants.JCR_CONTENT).adaptTo(ModifiableValueMap.class);
		mvm.put(JcrConstants.JCR_DATA, StringUtils.join(entries, "\n"));
		request.getResourceResolver().commit();
		request.getResourceResolver().refresh();
		log.debug("Changes saved...");
	}

	protected static final void writeEntriesToResponse(SlingHttpServletRequest request,
			SlingHttpServletResponse response, String message) throws ServletException, IOException {
		log.trace("writeEntriesToResponse");

		log.debug("Requesting redirect maps from {}", request.getResource());
		RedirectMapModel redirectMap = request.getResource().adaptTo(RedirectMapModel.class);

		response.setContentType(MediaType.JSON_UTF_8.toString());

		JsonObject res = new JsonObject();
		res.addProperty("message", message);

		JsonElement entries = gson.toJsonTree(redirectMap.getEntries(), new TypeToken<List<MapEntry>>() {
		}.getType());
		Iterator<JsonElement> it = entries.getAsJsonArray().iterator();
		for (int i = 0; it.hasNext(); i++) {
			it.next().getAsJsonObject().addProperty("id", i);
		}
		res.add("entries", entries);
		res.add("invalidEntries", gson.toJsonTree(redirectMap.getInvalidEntries(), new TypeToken<List<MapEntry>>() {
		}.getType()));

		IOUtils.write(res.toString(), response.getOutputStream(), StandardCharsets.UTF_8);
	}
}
