package com.adobe.acs.commons.version;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvolutionEntry {

	private static final Logger log = LoggerFactory.getLogger(EvolutionEntry.class);

	private EvolutionEntryType type;
	private String name;
	private Object value;
	private int depth;
	private String path;
	private Version version;
	private String relativePath;
	private Property property;

	public EvolutionEntry(Resource resource, Version version) {
		this.type = EvolutionEntryType.RESOURCE;
		this.name = resource.getName();
		this.depth = StringUtils.countMatches(StringUtils.substringAfterLast(resource.getPath(), "jcr:frozenNode"), "/");
		this.path = resource.getParent().getName();
		this.version = version;
		this.value = null;
		this.relativePath = StringUtils.substringAfterLast(resource.getPath(), "jcr:frozenNode/");
	}

	public EvolutionEntry(Property property, Version version) {
		try {
			this.property = property;
			this.type = EvolutionEntryType.PROPERTY;
			this.name = property.getName();
			this.depth = StringUtils.countMatches(StringUtils.substringAfterLast(property.getPath(), "jcr:frozenNode"), "/");
			this.version = version;
			this.path = property.getParent().getName();
			this.value = EvolutionHelper.printProperty(property);
			this.relativePath = StringUtils.substringAfterLast(property.getPath(), "jcr:frozenNode").replaceFirst("/", "");
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
		return EvolutionHelper.printObject(value);
	}
	
	public String getValueStringShort() {
		String value = getValueString();
		if(value.length() > 200){
			return value.substring(0, 200) + "...";	
		}
		return value;
	}

	public int getDepth() {
		return depth - 1;
	}

	public String getIndentation() {
		return StringUtils.repeat("--", depth);
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
			return "added";
		} else if (isWillBeRemoved()) {
			return "removed";
		} else if (isChanged()) {
			return "changed";
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
			String currentValue = EvolutionHelper.printProperty(prop);
			String oldValue = EvolutionHelper.printProperty(property);
			return !currentValue.equals(oldValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
