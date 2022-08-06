/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.jcr.query.Query;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.mcp.impl.processes.renovator.Util;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.NameConstants;

/**
 * Class for finding references to content.
 */
public class ReferenceFinder {

  private static final Logger log = LoggerFactory.getLogger(ReferenceFinder.class);

  private static Resource getClosestPublishableType(Resource resource) {
    if (NameConstants.NT_PAGE.equals(resource.getResourceType())
        || DamConstants.NT_DAM_ASSET.equals(resource.getResourceType())) {
      return resource;
    } else if (resource.getParent() != null) {
      return getClosestPublishableType(resource.getParent());
    } else {
      return null;
    }
  }

  private final List<Pair<String, String>> allReferences = new ArrayList<>();

  private final boolean exact;

  private final List<String> publishedReferences = new ArrayList<>();
  private final String reference;

  public ReferenceFinder(ResourceResolver resolver, String reference, String searchRoot, boolean exact) {
    this.exact = exact;
    this.reference = reference;
    Set<String> paths = new HashSet<>();
    findReferences(resolver, reference, searchRoot).forEach(r -> {
      if (!paths.contains(r.getPath())) {
        checkReferences(r);
        paths.add(r.getPath());
      }
    });
  }

  private void addReference(Resource resource, String key) {
    Resource parent = getClosestPublishableType(resource);
    if (parent != null && Util.isActivated(resource.getResourceResolver(), parent.getPath())) {
      this.publishedReferences.add(parent.getPath());
      this.allReferences.add(new ImmutablePair<String, String>(resource.getPath(), key));
    } else {
      this.allReferences.add(new ImmutablePair<String, String>(resource.getPath(), key));
    }
  }

  private void checkReferences(Resource resource) {
    log.trace("Checking for references in resource {}", resource);
    ValueMap properties = resource.getValueMap();
    properties.keySet().forEach(k -> {
      if (properties.get(k) instanceof String) {
        checkStringReference(resource, properties, k);
      } else if (properties.get(k) instanceof String[]) {
        checkStringArrayReference(resource, properties, k);
      }

    });
  }

  private void checkStringArrayReference(Resource resource, ValueMap properties, String k) {
    for (String v : properties.get(k, String[].class)) {
      if (reference.equals(v) || (!exact && v != null && v.contains(reference))) {
        log.trace("Found reference in property {}@{}", resource.getPath(), k);
        addReference(resource, k);
        break;
      }
    }
  }

  private void checkStringReference(Resource resource, ValueMap properties, String k) {
    String value = properties.get(k, "");
    if (reference.equals(value) || (!exact && value.contains(reference))) {
      log.trace("Found reference in property {}@{}", resource.getPath(), k);
      addReference(resource, k);
    }
  }

  protected Stream<Resource> findReferences(ResourceResolver resolver, String reference, String searchRoot) {
    log.trace("Finding references to {}", reference);
    String query = "SELECT * FROM [nt:base] AS s WHERE ISDESCENDANTNODE([" + searchRoot + "]) AND CONTAINS(s.*, '"
        + Text.escapeIllegalXpathSearchChars(reference) + "')";

    log.trace("Checking for references with: {}", query);
    Iterable<Resource> resources = () -> resolver.findResources(query, Query.JCR_SQL2);

    return StreamSupport.stream(resources.spliterator(), false);
  }

  public List<Pair<String, String>> getAllReferences() {
    return Collections.unmodifiableList(allReferences);
  }

  public List<String> getPublishedReferences() {
    return Collections.unmodifiableList(publishedReferences);
  }

}