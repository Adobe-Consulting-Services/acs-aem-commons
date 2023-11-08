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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.ServletException;

import com.drew.lang.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.RequireAem;

/**
 * Servlet for updating a line in the redirect map text file
 */
@SlingServlet(methods = { "POST" }, resourceTypes = {
    "acs-commons/components/utilities/redirectmappage" }, selectors = {
    "updateentry" }, extensions = { "json" }, metatype = false)
public class UpdateEntryServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = -1704915461516132101L;
    private static final Logger log = LoggerFactory.getLogger(UpdateEntryServlet.class);
    public static final String ETC_ACS_COMMONS_LISTS_COUNTRIES_JCR_CONTENT_LIST = "/etc/acs-commons/lists/countries/jcr:content/list";
    Map<String, String> countries;

    // Disable this feature on AEM as a Cloud Service
    @Reference(target="(distribution=classic)")
    transient RequireAem requireAem;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
        throws ServletException, IOException {
        log.trace("doPost");
        Resource resource = request.getResourceResolver().getResource(ETC_ACS_COMMONS_LISTS_COUNTRIES_JCR_CONTENT_LIST);
        if (Objects.nonNull(resource)) {
            countries = new HashMap<>();
            @NotNull
            Iterable<Resource> children = resource.getChildren();
            if(children != null){
                for (Resource childResource : children) {
                    ValueMap valueMap = childResource.getValueMap();
                    if (valueMap != null) {
                        String title = valueMap.get("jcr:title", String.class);
                        String nodeValue = valueMap.get("value", String.class);
                        countries.put(nodeValue, title);
                    }
                }
            }
        }
        String bulkParam = request.getParameter("bulk-edit");
        List<String> lines = RedirectEntriesUtils.readEntries(request);
        String source = request.getParameter("edit-source");
        String target = request.getParameter("edit-target");
        String targetBase = request.getParameter("edit-target-base");
        int idx = Integer.parseInt(request.getParameter("edit-id"), 10);

        if(bulkParam!=null) {
            boolean bulkEdit = bulkParam.equals("on") ? true : false;
            String regex = "^/[a-z]+/[a-z-]+";
            URI uri = null;
            try {
                uri = new URI(targetBase);
            } catch (URISyntaxException e) {
                log.error("Updating entry Exception with {}", targetBase,e);
            }
            if (uri!=null && uri.getHost() != null) {
                targetBase = uri.getPath();
            }
            if (bulkEdit) {
                log.debug("Updating entry {} with {} {}", idx, source, target);
                int index = 0;
                for (String line : lines) {
                    // Remove the language and country part using regex
                    String substring = targetBase.replaceFirst(regex, "");
                    if (StringUtils.isNotBlank(line)) {
                        String[] splitSourceTarget = line.split(" ");
                        if (splitSourceTarget.length >= 2) {
                            String splitKey = splitSourceTarget[0];
                            String splitKValue = splitSourceTarget[1];
                            URI uriSource = null;
                            URI uriTarget = null;
                            try {
                                uriSource = new URI(splitKValue);
                                uriTarget = new URI(target);
                            } catch (URISyntaxException e) {
                                log.error("Updating entry Exception with {}", targetBase,e);
                            }
                            if (uriSource !=null && uriSource.getHost() != null) {
                                splitKValue = uriSource.getPath();
                            }
                            if (uriTarget !=null && uriTarget.getHost() != null) {
                                target = uriTarget.getPath();
                                target =  target.replaceFirst(regex, "");
                            }
                            String[] splitVanityKey = splitKey.split("/");
                            String[] splitVanityValue = splitKValue.split("/");
                            if (splitVanityKey.length > 1 && splitVanityValue.length > 2) {
                                String countrySource = splitKey.split(":")[0];
                                String country = splitVanityValue[1].replace(":", "");
                                String codeLanguage = splitVanityValue[2];
                                String countryValue = countries.get(country);
                                String countrySourceValue = countries.get(countrySource);
                                if (line.contains(substring) && !StringUtils.isAnyBlank(country, codeLanguage, countryValue) && countryValue.equals(codeLanguage)) {
                                    if(!isFound(countries,target)) {
                                        if(StringUtils.isNotBlank(countrySourceValue)){
                                            line =  line.replace(country+"/"+countryValue, countrySource+"/"+countrySourceValue);
                                        }
                                        lines.set(index, line.replace(substring, target));
                                    }
                                }
                            }
                        }
                    }
                    index++;
                }
                log.debug("Updated entry...");
            }
        }else{
            lines.set(idx, source + " " + target);
        }
        log.trace("Saving lines {}", lines);
        RedirectEntriesUtils.updateRedirectMap(request, lines);
        RedirectEntriesUtils.writeEntriesToResponse(request, response,
            "Updated entry " + idx + " to " + source + " " + target);
    }
    private boolean isFound(Map<String,String> countries, String searchString) {
        boolean found = false;
        for (String value : countries.values()) {
            if (searchString.contains(value)) {
                found = true;
                break;
            }
        }
        return found;
    }
}