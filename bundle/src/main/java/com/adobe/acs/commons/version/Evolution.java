package com.adobe.acs.commons.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Evolution {

	private static final Logger log = LoggerFactory.getLogger(Evolution.class);

	private final List<EvolutionEntry> versionEntries = new ArrayList<EvolutionEntry>();
	private final Resource versionResource;
	private final Version version;
	private final EvolutionConfig config;

	public Evolution(Version version, Resource resource, EvolutionConfig config) {
		this.version = version;
		this.config = config;
		this.versionResource = resource;
		try {
			populate(versionResource, 0);
		} catch (Exception e) {
			log.warn("Could not populate Evolution", e);
		}
	}

	public List<EvolutionEntry> getVersionEntries() {
		return versionEntries;
	}

	public Date getVersionDate() {
		try {
			return version.getCreated().getTime();
		} catch (RepositoryException e) {
			log.warn("Could not get created date from version", e);
		}
		return null;
	}

	public String getVersionName() {
		try {
			return version.getName();
		} catch (RepositoryException e) {
			log.warn("Could not determine version name");
		}
		return "null";
	}

	public boolean isCurrent() {
		try {
			Version[] successors = version.getSuccessors();
			if (successors == null || successors.length == 0) {
				return true;
			}
		} catch (RepositoryException e) {
		}
		return false;
	}

	public Resource getResource() {
		return versionResource;
	}

	public ValueMap getProperties() {
		return ResourceUtil.getValueMap(versionResource);
	}

	private void populate(Resource r, int depth) throws PathNotFoundException, RepositoryException {
		ValueMap map = ResourceUtil.getValueMap(r);
		List<String> keys = new ArrayList<String>(map.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			Property property = r.adaptTo(Node.class).getProperty(key);
			String relPath = config.getRelativePropertyName(property.getPath());
			if (config.handleProperty(relPath)) {
				versionEntries.add(new EvolutionEntry(property, version, config));
			}
		}
		Iterator<Resource> iter = r.getChildren().iterator();
		while (iter.hasNext()) {
			depth++;
			Resource child = iter.next();
			String relPath = config.getRelativeResourceName(child.getPath());
			if (config.handleResource(relPath)) {
				versionEntries.add(new EvolutionEntry(child, version, config));
				populate(child, depth);
			}
			depth--;
		}
	}
}
