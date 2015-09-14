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

package com.adobe.acs.commons.analysis.jcrchecksum.impl.options;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ProviderType
public class RequestChecksumGeneratorOptions extends AbstractChecksumGeneratorOptions {
    private static final Logger log = LoggerFactory.getLogger(RequestChecksumGeneratorOptions.class);

    public RequestChecksumGeneratorOptions(SlingHttpServletRequest request) throws IOException {

        this.addIncludedNodeTypes(request.getParameterValues(NODES_TYPES));
        this.addExcludedNodeTypes(request.getParameterValues(NODE_TYPE_EXCLUDES));
        this.addExcludedProperties(request.getParameterValues(PROPERTY_EXCLUDES));
        this.addSortedProperties(request.getParameterValues(SORTED_PROPERTIES));

        // Add Paths

        this.addPaths(request.getParameterValues(PATHS));
        this.addPaths(this.getPathsFromQuery(request.getResourceResolver(),
                request.getParameter(QUERY_TYPE),
                request.getParameter(QUERY)).toArray(new String[]{}));

        RequestParameter data = request.getRequestParameter(DATA);
        if(data != null && data.getInputStream() != null) {
            this.addPaths(this.getPathsFromInputstream(data.getInputStream()).toArray(new String[]{}));
        }
    }
}
