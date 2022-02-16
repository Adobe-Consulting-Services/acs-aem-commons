/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.version.model;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.version.EvolutionAnalyser;
import com.adobe.acs.commons.version.EvolutionContext;

@Model(adaptables = SlingHttpServletRequest.class)
public class EvolutionModel {

    private static final Logger log = LoggerFactory.getLogger(EvolutionModel.class);

    @Inject
    private ResourceResolver resolver;

    @Inject
    private EvolutionAnalyser analyser;

    private final String path;

    public EvolutionModel(SlingHttpServletRequest request) {
        this.path = request.getParameter("path");
    }

    public String getResourcePath() {
        return path;
    }

    public EvolutionContext getEvolution() {
        if (StringUtils.isNotEmpty(path)) {
            Resource resource = resolver.resolve(path);
            if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
                return analyser.getEvolutionContext(resource);
            }
            log.warn("Could not resolve resource at path={}", path);
        }
        log.warn("No path provided");
        return null;
    }

}
