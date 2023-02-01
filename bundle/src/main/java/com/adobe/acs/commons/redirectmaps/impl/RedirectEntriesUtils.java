/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.redirectmaps.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.redirectmaps.models.MapEntry;
import com.adobe.acs.commons.redirectmaps.models.RedirectMapModel;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.google.common.base.Charsets;
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

        List<String> lines = new ArrayList<>();
        Resource redirectMap = request.getResource().getChild(RedirectMapModel.MAP_FILE_NODE);
        if (redirectMap != null) {
            lines = IOUtils.readLines(redirectMap.adaptTo(InputStream.class), StandardCharsets.UTF_8);
        }
        log.trace("Loaded lines: {}", lines);

        return lines;
    }

    protected static final void updateRedirectMap(SlingHttpServletRequest request, List<String> entries)
            throws PersistenceException {
        Resource resource = request.getResource();

        log.info("Updating redirect map at {}", request.getResource().getPath());

        Calendar now = Calendar.getInstance();
        ModifiableValueMap contentProperties = resource.adaptTo(ModifiableValueMap.class);
        if (contentProperties == null) {
            throw new PersistenceException("Failed to retrieve resource " + resource + " for editing");
        }
        contentProperties.put(NameConstants.PN_PAGE_LAST_MOD, now);
        contentProperties.put(NameConstants.PN_PAGE_LAST_MOD_BY, request.getResourceResolver().getUserID());

        Map<String, Object> fileParams = new HashMap<>();
        fileParams.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE);
        Resource fileResource = ResourceUtil.getOrCreateResource(request.getResourceResolver(),
                resource.getPath() + "/" + RedirectMapModel.MAP_FILE_NODE, fileParams, JcrConstants.NT_UNSTRUCTURED,
                false);

        Map<String, Object> contentParams = new HashMap<>();
        contentParams.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
        contentParams.put(JcrConstants.JCR_MIMETYPE, "text/plain");
        Resource contentResource = ResourceUtil.getOrCreateResource(resource.getResourceResolver(),
                fileResource.getPath() + "/" + JcrConstants.JCR_CONTENT, contentParams, JcrConstants.NT_UNSTRUCTURED,
                false);

        ModifiableValueMap mvm = contentResource.adaptTo(ModifiableValueMap.class);
        if (mvm == null) {
            throw new PersistenceException("Retrieve resource " + contentResource + " for editing");
        }
        mvm.put(JcrConstants.JCR_DATA,
                new ByteArrayInputStream(StringUtils.join(entries, "\n").getBytes(Charsets.UTF_8)));
        mvm.put(JcrConstants.JCR_LASTMODIFIED, now);
        mvm.put(JcrConstants.JCR_LAST_MODIFIED_BY, request.getResourceResolver().getUserID());
        request.getResourceResolver().commit();
        request.getResourceResolver().refresh();
        log.debug("Changes saved...");
    }

    protected static final void writeEntriesToResponse(SlingHttpServletRequest request,
            SlingHttpServletResponse response, String message) throws IOException {
        log.trace("writeEntriesToResponse");

        log.debug("Requesting redirect maps from {}", request.getResource());
        RedirectMapModel redirectMap = request.getResource().adaptTo(RedirectMapModel.class);

        response.setContentType(MediaType.JSON_UTF_8.toString());

        JsonObject res = new JsonObject();
        res.addProperty("message", message);

        if (redirectMap != null) {
            JsonElement entries = gson.toJsonTree(redirectMap.getEntries(0), new TypeToken<List<MapEntry>>() {
            }.getType());
            res.add("entries", entries);
            res.add("invalidEntries", gson.toJsonTree(redirectMap.getInvalidEntries(), new TypeToken<List<MapEntry>>() {
            }.getType()));
        } else {
            throw new IOException("Failed to get redirect map from " + request.getResource());
        }

        IOUtils.write(res.toString(), response.getOutputStream(), StandardCharsets.UTF_8);
    }
}
