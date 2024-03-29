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
package com.adobe.acs.commons.version.impl;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EvolutionContextImpl implements EvolutionContext {
    private static final Logger log = LoggerFactory.getLogger(EvolutionContext.class);

    private List<Evolution> versions = new ArrayList<Evolution>();
    private List<Evolution> evolutionItems = new ArrayList<Evolution>();

    public EvolutionContextImpl(Resource providedResource, EvolutionConfig config) {
        Resource resource = providedResource.isResourceType("cq:Page")
                ? providedResource.getChild("jcr:content")
                : providedResource;
        populateEvolutions(resource, config);
    }

    @Override
    public List<Evolution> getEvolutionItems() {
        return Collections.unmodifiableList(evolutionItems);
    }

    @Override
    public List<Evolution> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    private void populateEvolutions(Resource resource, EvolutionConfig config) {
        try {
            ResourceResolver resolver = resource.getResourceResolver();
            VersionManager versionManager = resolver.adaptTo(Session.class).getWorkspace().getVersionManager();
            VersionHistory history = versionManager.getVersionHistory(resource.getPath());
            Iterator<Version> iter = history.getAllVersions();
            while (iter.hasNext()) {
                Version next = iter.next();
                String versionPath = next.getFrozenNode().getPath();
                Resource versionResource = resolver.resolve(versionPath);
                versions.add(new EvolutionImpl(next, versionResource, config));
                log.debug("Version={} added to EvolutionItem", next.getName());
            }
        } catch (UnsupportedRepositoryOperationException e1) {
            log.warn("Could not find version for resource={}", resource.getPath());
        } catch (Exception e) {
            log.error("Could not find versions", e);
        }
        evolutionItems = new ArrayList<>(versions);
        evolutionItems.add(new CurrentEvolutionImpl(resource, config));
    }
}
