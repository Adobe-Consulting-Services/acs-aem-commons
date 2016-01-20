package com.adobe.acs.commons.synth.children;

import com.adobe.acs.commons.synth.children.impl.JSONModifiableValueMapDecorator;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;

import java.util.Map;

/**
 * Resource object that represents data that can be serialized to a resource's property.
 */
public class SyntheticChildAsPropertyResource extends SyntheticResource {
    public static final String RESOURCE_TYPE = "acs-commons/synthetic/synthetic-child-as-property-resource";

    private final JSONModifiableValueMapDecorator data;

    /**
     * Creates a new SyntheticChildAsPropertyResource.
     *
     * @param parent the synthetic nodes parent (a real JCR Resource)
     * @param nodeName the name of the synthetic child resource
     */
    public SyntheticChildAsPropertyResource(Resource parent, String nodeName) {
        super(parent.getResourceResolver(), parent.getPath() + "/" + nodeName, RESOURCE_TYPE);
        this.data = new JSONModifiableValueMapDecorator();
    }

    /**
     * Creates a new SyntheticChildAsPropertyResource.
     *
     * @param parent the synthetic nodes parent (a real JCR Resource)
     * @param nodeName the name of the synthetic child resource
     * @param data initial value map data
     */
    public SyntheticChildAsPropertyResource(Resource parent, String nodeName, Map<String, Object> data) {
        super(parent.getResourceResolver(), parent.getPath() + "/" + nodeName, RESOURCE_TYPE);
        if (data != null) {
            this.data = new JSONModifiableValueMapDecorator(data);
        } else {
            this.data = new JSONModifiableValueMapDecorator();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ValueMap getValueMap() {
        return this.data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) this.data;
        } else if (type == ModifiableValueMap.class) {
            return (AdapterType) this.data;
        }

        return super.adaptTo(type);
    }
}