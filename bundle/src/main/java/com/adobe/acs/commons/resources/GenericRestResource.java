package com.adobe.acs.commons.resources;

import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;


/**
 * A Resource that was obtained from your super duper remote REST API
 */
public class GenericRestResource extends SyntheticResource {

    protected ValueMap myValueMap;

    public GenericRestResource(ResourceResolver resourceResolver, String path, String resourceType, ValueMap valueMap) {
        super(resourceResolver, path, resourceType);

        this.myValueMap = valueMap;
    }

    public GenericRestResource(ResourceResolver resourceResolver, ResourceMetadata rm, String resourceType, ValueMap valueMap) {
        super(resourceResolver, rm, resourceType);

        this.myValueMap = valueMap;
    }

    @Override
    public ValueMap getValueMap() {
        // TODO resourceType is not present in that value map .. issue?

        return myValueMap;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type.isAssignableFrom(ValueMap.class)) {
            return (AdapterType) getValueMap();
        }

        return super.adaptTo(type);
    }

}
