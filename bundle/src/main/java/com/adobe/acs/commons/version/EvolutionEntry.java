package com.adobe.acs.commons.version;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvolutionEntry {

	private static final Logger log = LoggerFactory.getLogger(EvolutionEntry.class);

	private static int MAX_CHARS = 200;
	private static String V_ADDED = "added";
	private static String V_CHANGED = "changed";
	private static String V_REMOVED = "removed";
	
	private EvolutionEntryType type;
	private String name;
	private Object value;
	private int depth;
	private String path;
	private Version version;
	private String relativePath;
	private Property property;
	private EvolutionConfig config;

	public EvolutionEntry(Resource resource, Version version, EvolutionConfig config) {
		this.config = config;
		this.type = EvolutionEntryType.RESOURCE;
		this.name = resource.getName();
		this.depth = config.getDepthForPath(resource.getPath());
		this.path = resource.getParent().getName();
		this.version = version;
		this.value = null;
		this.relativePath = config.getRelativeResourceName(resource.getPath());
	}

	public EvolutionEntry(Property property, Version version, EvolutionConfig config) {
		try {
			this.config = config;
			this.property = property;
			this.type = EvolutionEntryType.PROPERTY;
			this.name = property.getName();
			this.depth = config.getDepthForPath(property.getPath());
			this.version = version;
			this.path = property.getParent().getName();
			this.value = config.printProperty(property);
			this.relativePath = config.getRelativePropertyName(property.getPath());
		} catch (Exception e) {
			log.error("Could not inititalize VersionEntry", e);
		}
	}

	public boolean isResource() {
		return EvolutionEntryType.RESOURCE == type;
	}

	public String getName() {
		return name;
	}

	public String getUniqueName() {
		return (name + path).replace(":", "_").replace("/", "_").replace("@", "_");
	}

	public EvolutionEntryType getType() {
		return type;
	}

	public String getValueString() {
		return config.printObject(value);
	}
	
	public String getValueStringShort() {
		String value = getValueString();
		if(value.length() > MAX_CHARS){
			return value.substring(0, MAX_CHARS) + "...";	
		}
		return value;
	}

	public int getDepth() {
		return depth - 1;
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

	public String getStatus() {
		if (isAdded()) {
			return V_ADDED;
		} else if (isWillBeRemoved()) {
			return V_REMOVED;
		} else if (isChanged()) {
			return V_CHANGED;
		} else {
			return "";
		}
	}

	public boolean isAdded() {
		try {
			if (isResource()) {
				Node node = version.getLinearPredecessor().getFrozenNode().getNode(relativePath);
				return node == null;
			} else {
				Property prop = version.getLinearPredecessor().getFrozenNode().getProperty(relativePath);
				return prop == null;
			}
		} catch (Exception e) {
		}
		return true;
	}

	public boolean isWillBeRemoved() {
		try {
			if (isCurrent()) {
				return false;
			}
			if (isResource()) {
				Node node = version.getLinearPredecessor().getFrozenNode().getNode(relativePath);
				return node == null;
			} else {
				Property prop = version.getLinearSuccessor().getFrozenNode().getProperty(relativePath);
				return prop == null;
			}
		} catch (Exception e) {
		}
		return true;
	}

	public boolean isChanged() {
		try {
			if(isResource()){
				return false;
			}
			Property prop = version.getLinearPredecessor().getFrozenNode().getProperty(relativePath);
			String currentValue = config.printProperty(prop);
			String oldValue = config.printProperty(property);
			return !currentValue.equals(oldValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
