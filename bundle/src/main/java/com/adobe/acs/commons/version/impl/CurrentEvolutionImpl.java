package com.adobe.acs.commons.version.impl;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionEntry;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Dominik Foerderreuther <df@adobe.com> on 19/12/16.
 */
public class CurrentEvolutionImpl implements Evolution {

    private static final Logger log = LoggerFactory.getLogger(CurrentEvolutionImpl.class);

    private final Resource resource;
    private final List<EvolutionEntry> versionEntries = new ArrayList<EvolutionEntry>();
    private EvolutionConfig config;

    public CurrentEvolutionImpl(Resource resource, EvolutionConfig config) {
        this.resource = resource;
        this.config = config;
        try {
            populate(this.resource, 0);
        } catch (RepositoryException e) {
            log.warn("Could not populate Evolution", e);
        }
    }

    @Override
    public boolean isCurrent() {
        return true;
    }

    @Override
    public String getVersionName() {
        return "Latest";
    }

    @Override
    public Date getVersionDate() {
        return new Date();
    }

    @Override
    public List<EvolutionEntry> getVersionEntries() {
        return this.versionEntries;
    }

    private void populate(Resource r, int depth) throws PathNotFoundException, RepositoryException {
        ValueMap map = r.getValueMap();
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Property property = r.adaptTo(Node.class).getProperty(key);
            String relPath = config.getRelativePropertyName(property.getPath());
            if (config.handleProperty(relPath)) {
                versionEntries.add(new CurrentEvolutionEntryImpl(property, config));
            }
        }
        Iterator<Resource> iter = r.getChildren().iterator();
        while (iter.hasNext()) {
            depth++;
            Resource child = iter.next();
            String relPath = config.getRelativeResourceName(child.getPath());
            if (config.handleResource(relPath)) {
                versionEntries.add(new CurrentEvolutionEntryImpl(child, config));
                populate(child, depth);
            }
            depth--;
        }
    }
}
