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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionContext;
import com.day.cq.commons.jcr.JcrConstants;

public final class EvolutionContextImpl implements EvolutionContext {

    private static final Logger log = LoggerFactory.getLogger(EvolutionContext.class);

    private final List<EvolutionImplBase> versions = new ArrayList<EvolutionImplBase>();
    private final List<EvolutionImplBase> evolutionItems = new ArrayList<EvolutionImplBase>();

    public EvolutionContextImpl(final Resource resource, final EvolutionConfig config) {
        final ResourceResolver resolver = resource.getResourceResolver();
        final Optional<Workspace> workspace = Optional.ofNullable(resolver.adaptTo(Session.class))
            .map(Session::getWorkspace);
        if (!workspace.isPresent()) {
            log.warn(getWarnMessage(resource));
        }

        final Resource versionedResource = resource.isResourceType("cq:Page") ? resource.getChild(JcrConstants.JCR_CONTENT) : resource;
        try {
            final VersionManager versionManager = workspace.get().getVersionManager();
            final VersionHistory history = versionManager.getVersionHistory(versionedResource.getPath());
            @SuppressWarnings("unchecked")
			final Iterator<Version> iter = history.getAllVersions();
            while (iter.hasNext()) {
                final Version next = iter.next();
                final String versionPath = next.getFrozenNode().getPath();
                final Resource versionResource = resolver.resolve(versionPath);
                versions.add(new EvolutionImpl(next, versionResource, config));
                log.debug("Version={} added to EvolutionItem", next.getName());
            }
        } catch (final RepositoryException e) {
            log.warn(getWarnMessage(resource), e);
        }

        evolutionItems.addAll(versions);
        evolutionItems.add(new CurrentEvolutionImpl(versionedResource, config));

        connectPrevNext();
    }

	private String getWarnMessage(final Resource resource) {
		return String.format("Could not find versions for resource=%s", resource.getPath());
	}

	private void connectPrevNext() {
		for (int i = evolutionItems.size() - 2; i > 1; i--) {
			final EvolutionImplBase current = evolutionItems.get(i);
			current.setPrev(evolutionItems.get(i - 1));
			current.setNext(evolutionItems.get(i + 1));
		}
	}

    @Override
    public List<Evolution> getEvolutionItems() {
        return evolutionItems.stream().collect(Collectors.toList());
    }

    @Override
    public List<Evolution> getVersions() {
        return versions.stream().collect(Collectors.toList());
    }

}
