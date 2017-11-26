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

package com.adobe.acs.commons.analysis.jcrchecksum.impl.options;

import aQute.bnd.annotation.ProviderType;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@ProviderType
public class RequestChecksumGeneratorOptions extends AbstractChecksumGeneratorOptions {

    public RequestChecksumGeneratorOptions(SlingHttpServletRequest request) throws IOException {
        this.addIncludedNodeTypes(request.getParameterValues(NODES_TYPES));
        this.addExcludedNodeTypes(request.getParameterValues(NODE_TYPE_EXCLUDES));
        this.addExcludedProperties(request.getParameterValues(PROPERTY_EXCLUDES));
        this.addSortedProperties(request.getParameterValues(SORTED_PROPERTIES));
    }

    public static Set<String> getPaths(SlingHttpServletRequest request) throws IOException {
        Set<String> paths = new HashSet<String>();

        // Add Paths

        if (request.getParameterValues(PATHS) != null) {
            String[] pathArr = request.getParameterValues(PATHS);
            for (String path : pathArr) {
                if (path.length() > 0) {
                    paths.add(path);
                }
            }
        }

        paths.addAll(getPathsFromQuery(request.getResourceResolver(),
                request.getParameter(QUERY_TYPE),
                request.getParameter(QUERY)));

        RequestParameter data = request.getRequestParameter(DATA);
        if (data != null && data.getInputStream() != null) {
            paths.addAll(getPathsFromInputstream(data.getInputStream(), request.getCharacterEncoding()));
        }

        return paths;
    }

    private static Set<String> getPathsFromQuery(ResourceResolver resourceResolver, String language, String query) {
        if (StringUtils.isBlank(query)) {
            return Collections.EMPTY_SET;
        }

        Set<String> paths = new HashSet<String>();
        language = StringUtils.defaultIfEmpty(language, "xpath");
        Iterator<Resource> resources = resourceResolver.findResources(query, language);

        while (resources.hasNext()) {
            paths.add(resources.next().getPath());
        }

        return paths;
    }

    private static Set<String> getPathsFromInputstream(InputStream is, String encoding) throws IOException {
        if (is == null) {
            return Collections.EMPTY_SET;
        }

        Set<String> paths = new HashSet<String>();
        encoding = (encoding != null) ?  encoding : Charset.defaultCharset().name();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, encoding))) {
            String path;
            while ((path = br.readLine()) != null) {
                paths.add(path);
            }
        }

        return paths;
    }
}