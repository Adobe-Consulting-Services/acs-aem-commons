package com.adobe.acs.commons.synth.children;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;

/**
 * Resource object that represents data that can be serialized to a resource's property.
 */
public class SyntheticChildAsPropertyResource extends SyntheticResource {
    public static final String RESOURCE_TYPE = "acs-commons/synthetic/synthetic-child-as-property-resource";

    private final ValueMap valueMap;

    public SyntheticChildAsPropertyResource(Resource parent, String nodeName, ValueMap valueMap) {
        super(parent.getResourceResolver(), parent.getPath() + "/" + nodeName, RESOURCE_TYPE);
        this.valueMap = valueMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ValueMap getValueMap() {
        return this.valueMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) this.valueMap;
        }

        return super.adaptTo(type);
    }
}