package com.adobe.acs.commons.mcp.impl.processes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

  private final List<String> publishedReferences = new ArrayList<>();

  private String reference;
  private boolean exact;

  public ReferenceFinder(ResourceResolver resolver, String reference, String searchRoot, boolean exact) {

    log.trace("Finding references to {}", reference);
    this.reference = reference;
    this.exact = exact;
    String query = "SELECT * FROM [nt:base] AS s WHERE ISDESCENDANTNODE([" + searchRoot + "]) AND CONTAINS(s.*, '"
        + Text.escapeIllegalXpathSearchChars(reference) + "')";
    Set<String> paths = new HashSet<>();

    Iterator<Resource> resources = resolver.findResources(query, Query.JCR_SQL2);
    log.trace("Checking for references with: {}", query);
    while (resources.hasNext()) {
      Resource r = resources.next();
      if (!paths.contains(r.getPath())) {
        checkReferences(r);
        paths.add(r.getPath());
      }
    }
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
        String value = properties.get(k, "");
        if (reference.equals(value) || (!exact && value.contains(reference))) {
          log.trace("Found reference in property {}@{}", resource.getPath(), k);
          addReference(resource, k);
        }
      } else if (properties.get(k) instanceof String[]) {
        for (String v : properties.get(k, String[].class)) {
          if (reference.equals(v) || (!exact && v != null && v.contains(reference))) {
            log.trace("Found reference in property {}@{}", resource.getPath(), k);
            addReference(resource, k);
            break;
          }
        }
      }

    });
  }

  public List<Pair<String, String>> getAllReferences() {
    return allReferences;
  }

  public List<String> getPublishedReferences() {
    return publishedReferences;
  }

}