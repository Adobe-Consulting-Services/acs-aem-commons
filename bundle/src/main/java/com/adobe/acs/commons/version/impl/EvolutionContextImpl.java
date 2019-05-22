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
package com.adobe.acs.commons.version.impl;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionContext;
import com.day.cq.commons.jcr.JcrConstants;

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
import java.util.Iterator;
import java.util.List;

public final class EvolutionContextImpl implements EvolutionContext {

    private static final Logger log = LoggerFactory.getLogger(EvolutionContext.class);

    private final List<Evolution> versions = new ArrayList<Evolution>();
    private final List<Evolution> evolutionItems = new ArrayList<Evolution>();

    public EvolutionContextImpl(Resource resource, final EvolutionConfig config) {
        final Resource r = resource.isResourceType("cq:Page") ? resource.getChild(JcrConstants.JCR_CONTENT) : resource;
        final ResourceResolver resolver = r.getResourceResolver();
        try {
            final VersionManager versionManager = resolver.adaptTo(Session.class).getWorkspace().getVersionManager();
            final VersionHistory history = versionManager.getVersionHistory(r.getPath());
            final Iterator<Version> iter = history.getAllVersions();
            while (iter.hasNext()) {
                final Version next = iter.next();
                final String versionPath = next.getFrozenNode().getPath();
                final Resource versionResource = resolver.resolve(versionPath);
                versions.add(new EvolutionImpl(next, versionResource, config));
                log.debug("Version={} added to EvolutionItem", next.getName());
            }
        } catch (final UnsupportedRepositoryOperationException e) {
            log.warn("Could not find versions for resource={}", r.getPath());
        } catch (final Exception e) {
            log.error("Could not find versions", e);
        }

        evolutionItems.addAll(versions);
        evolutionItems.add(new CurrentEvolutionImpl(r, config));
    
    }

    @Override
    public List<Evolution> getEvolutionItems() {
        return evolutionItems;
    }

    @Override
    public List<Evolution> getVersions() {
        return versions;
    }

}
