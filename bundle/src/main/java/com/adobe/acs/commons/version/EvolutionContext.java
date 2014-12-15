package com.adobe.acs.commons.version;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvolutionContext {

	private static final Logger log = LoggerFactory.getLogger(EvolutionContext.class);

	private Resource resource = null;
	private VersionHistory history = null;
	private ResourceResolver resolver = null;
	private VersionManager versionManager = null;
	private List<Evolution> versions = new ArrayList<Evolution>();

	public EvolutionContext(Resource resource) {
		this.resource = resource;
		populateEvolutions();
	}

	public List<Evolution> getEvolutionItems() {
		return versions;
	}
	
	private void populateEvolutions(){
		try {
			this.resolver = resource.getResourceResolver();
			this.versionManager = resolver.adaptTo(Session.class).getWorkspace().getVersionManager();
			this.history = versionManager.getVersionHistory(resource.getPath());
			Iterator<Version> iter = history.getAllVersions();
			while (iter.hasNext()) {
				Version next = iter.next();
				String versionPath = next.getFrozenNode().getPath();
				Resource versionResource = resolver.resolve(versionPath);
				versions.add(new Evolution(next, versionResource));
				log.debug("Version={} added to EvolutionItem", next.getName());
			}
		} catch (UnsupportedRepositoryOperationException e1){
			log.warn("Could not find version for resource={}", resource.getPath());
		} catch (Exception e) {
			log.error("Could not find versions", e);
		}
	}

}
