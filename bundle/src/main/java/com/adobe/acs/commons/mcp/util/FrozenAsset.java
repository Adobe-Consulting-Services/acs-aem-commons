package com.adobe.acs.commons.mcp.util;

import static org.apache.jackrabbit.JcrConstants.JCR_DATA;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.granite.asset.api.AssetIOException;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import static com.day.cq.dam.api.DamConstants.SUBASSETS_FOLDER;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.RenditionPicker;
import com.day.cq.dam.api.Revision;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrozenAsset implements Asset {

    private static final Logger LOG = LoggerFactory.getLogger(FrozenAsset.class);
    private static final Joiner pathJoiner = Joiner.on('/').skipNulls();

    private static final String CONTENT_PATH = pathJoiner.join(JcrConstants.JCR_CONTENT, DamConstants.RENDITIONS_FOLDER, DamConstants.ORIGINAL_FILE, JcrConstants.JCR_CONTENT);
    private static final String RENDITIONS_PATH = pathJoiner.join(JcrConstants.JCR_CONTENT, DamConstants.RENDITIONS_FOLDER);
    private static final String METADATA_PATH = pathJoiner.join(JcrConstants.JCR_CONTENT, DamConstants.METADATA_FOLDER);

    private final Asset head;
    private final Resource frozenResource;

    public FrozenAsset(Asset head, Revision revision) throws RepositoryException {
        final Node frozenNode = revision.getVersion().getFrozenNode();
        this.head = head;
        frozenResource = head.adaptTo(Resource.class).getResourceResolver().getResource(frozenNode.getPath());
    }

    public FrozenAsset(ResourceResolver rr, String path) throws RepositoryException {
        this.head = null;
        frozenResource = rr.getResource(path);
    }

    @Override
    public String getPath() {
        return frozenResource.getPath();
    }

    @Override
    public String getName() {
        return frozenResource.getName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type.equals(Resource.class)) {
            return (AdapterType) frozenResource;
        }
        return null;
    }

    public String getMetadataValue(final String name) {
        Resource meta = frozenResource.getChild(METADATA_PATH);
        if (meta == null) {
            return null;
        }

        return meta.getValueMap().get(name, String.class);
    }

    @Override
    public Object getMetadata(String name) {
        return getMetadata().get(name);
    }

    @Override
    public long getLastModified() {
        Resource r = frozenResource.getChild(CONTENT_PATH);
        if (r == null) {
            return System.currentTimeMillis();
        }
        return r.getValueMap().get(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance()).getTimeInMillis();
    }

    @Override
    public Rendition getRendition(String name) {
        Resource r = frozenResource.getChild(pathJoiner.join(RENDITIONS_PATH, name));
        if (r == null) {
            return null;
        }
        return new FrozenRendition(r);
    }

    @Override
    public Rendition getOriginal() {
        return getRendition(DamConstants.ORIGINAL_FILE);
    }

    @Override
    public Rendition getCurrentOriginal() {
        return getOriginal();
    }

    @Override
    public boolean isSubAsset() {
        if (head != null) {
            return head.isSubAsset();
        } else {
            return SUBASSETS_FOLDER.equals(ResourceUtil.getName(ResourceUtil.getParent(frozenResource)));
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Resource meta = frozenResource.getChild(METADATA_PATH);
        if (meta == null) {
            return null;
        }
        return meta.getValueMap();
    }

    @Override
    public Resource setRendition(String name, InputStream is, String mimeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCurrentOriginal(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Revision createRevision(String label, String comment)
            throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Rendition> getRenditions() {
        Resource renditions = frozenResource.getChild(RENDITIONS_PATH);
        if (renditions == null) {
            return Lists.newArrayList();
        }
        List<Rendition> rv = Lists.newArrayList();
        for (Resource r : renditions.getChildren()) {
            rv.add(getRendition(r.getName()));
        }
        return rv;
    }

    @Override
    public Iterator<Rendition> listRenditions() {
        return getRenditions().iterator();
    }

    @Override
    public Rendition getRendition(RenditionPicker picker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getModifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Asset restore(String revisionId) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Revision> getRevisions(Calendar cal) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMimeType() {
        String mimeType = getMetadataValue(DamConstants.DC_FORMAT);

        if (Strings.isNullOrEmpty(mimeType)) {
            final Rendition original = getOriginal();
            if (null != original) {
                mimeType = original.getMimeType();
            }
        }
        return mimeType;
    }

    @Override
    public Rendition addRendition(String name, InputStream is, String mimeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rendition addRendition(String name, InputStream is,
            Map<String, Object> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Asset addSubAsset(String name, String mimeType, InputStream stream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Asset> getSubAssets() {
        if (head == null) {
            throw new UnsupportedOperationException();
        }
        Resource subassets = frozenResource.getChild(DamConstants.SUBASSETS_FOLDER);
        if (subassets != null) {
            Stream<Resource> subs = StreamSupport.stream(
                    subassets.getChildren().spliterator(), false
            );

            return subs.map(r -> {
                try {
                    return new FrozenAsset(
                            frozenResource.getResourceResolver(),
                            r.getPath());
                } catch (RepositoryException ex) {
                    LOG.error("Error retrieving subasset from "+r.getPath(), ex);
                }
                return null;
            }).collect(Collectors.toList());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public void removeRendition(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBatchMode(boolean mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBatchMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rendition getImagePreviewRendition() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getMetadataValueFromJcr(String name) {
        return getMetadataValue(name);
    }

    @Override
    public String getID() {
        return frozenResource.getValueMap().get(Property.JCR_FROZEN_UUID, String.class);
    }

    //@Override -- required for 6.3 but not required in 6.2
    public void initAssetState() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class FrozenRendition implements Rendition {

        private final Resource container;

        private final Resource renditionData;

        public FrozenRendition(Resource container) {
            this.container = container;
            this.renditionData = container.getChild(JcrConstants.JCR_CONTENT);
        }

        @Override
        public Resource getParent() {
            return container.getParent();
        }

        @Override
        public Iterator<Resource> listChildren() {
            return container.listChildren();
        }

        @Override
        public Iterable<Resource> getChildren() {
            return container.getChildren();
        }

        @Override
        public Resource getChild(String relPath) {
            return container.getChild(relPath);
        }

        @Override
        public String getResourceType() {
            return container.getResourceType();
        }

        @Override
        public String getResourceSuperType() {
            return container.getResourceSuperType();
        }

        @Override
        public boolean hasChildren() {
            return container.hasChildren();
        }

        @Override
        public boolean isResourceType(String resourceType) {
            return container.isResourceType(resourceType);
        }

        @Override
        public ResourceMetadata getResourceMetadata() {
            return container.getResourceMetadata();
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return container.getResourceResolver();
        }

        @Override
        public ValueMap getValueMap() {
            return container.getValueMap();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == Resource.class) {
                return (AdapterType) container;
            }
            return container.adaptTo(type);
        }

        @Override
        public String getMimeType() {
            return getProperties().get(JcrConstants.JCR_MIMETYPE, String.class);
        }

        @Override
        public String getName() {
            return container.getName();
        }

        @Override
        public String getPath() {
            return container.getPath();
        }

        @Override
        public ValueMap getProperties() {
            return renditionData.getValueMap();
        }

        @Override
        public long getSize() {
            int size = 0;
            final Property p = renditionData.getValueMap().get(JCR_DATA, Property.class);
            try {
                return (null != p) ? p.getBinary().getSize() : 0;
            } catch (RepositoryException e) {
                LOG.error("Failed to get the Rendition binary size in bytes [{}]: ", getPath(), e);
            }
            return size;
        }

        @Override
        public InputStream getStream() {
            try {
                return renditionData.getValueMap().get(JCR_DATA, InputStream.class);
            } catch (Throwable t) {
                throw new AssetIOException(t);
            }
        }

        @Override
        public Asset getAsset() {
            return FrozenAsset.this;
        }
    }
}
