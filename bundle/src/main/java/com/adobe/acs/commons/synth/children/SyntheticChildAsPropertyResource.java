package com.adobe.acs.commons.synth.children;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.HashMap;
import java.util.Map;

/**
 * Resource object that represents data that can be serialized to a resource's property.
 */
public class SyntheticChildAsPropertyResource extends SyntheticResource {
    public static final String RESOURCE_TYPE = "acs-commons/synthetic/synthetic-child-as-property-resource";

    private final HashMap<String, Object> data;

    public SyntheticChildAsPropertyResource(Resource parent, String nodeName) {
        super(parent.getResourceResolver(), parent.getPath() + "/" + nodeName, RESOURCE_TYPE);
        this.data = new HashMap<String, Object>();
    }

    public SyntheticChildAsPropertyResource(Resource parent, String nodeName, Map<String, Object> data) {
        super(parent.getResourceResolver(), parent.getPath() + "/" + nodeName, RESOURCE_TYPE);
        if (data != null) {
            this.data = new HashMap<String, Object>(data);
        } else {
            this.data = new HashMap<String, Object>();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ValueMap getValueMap() {
        return new ValueMapDecorator(this.data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) new ValueMapDecorator(this.data);
        } else if (type == ModifiableValueMap.class) {
            return (AdapterType) new ModifiableValueMapDecorator(this.data);
        }

        return super.adaptTo(type);
    }
}