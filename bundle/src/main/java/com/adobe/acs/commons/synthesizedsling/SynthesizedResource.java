package com.adobe.acs.commons.synthesizedsling;

import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Better SyntheticResource
 */
public class SynthesizedResource extends SyntheticResource {

    private ValueMap syntheticValueMap = new ValueMapDecorator(new HashMap<String, Object>());

    public SynthesizedResource(ResourceResolver resourceResolver, String path, String resourceType) {
        super(resourceResolver, path, resourceType);
    }

    public SynthesizedResource(ResourceResolver resourceResolver, ResourceMetadata rm, String resourceType) {
        super(resourceResolver, rm, resourceType);
    }

    public void putValueMapEntry(String key, Object value) {
        syntheticValueMap.put(key, value);
    }

    public void setValueMap(Map<String, Object> valueMap) {
        syntheticValueMap.putAll(valueMap);
    }

    @Override
    public ValueMap getValueMap() {
        ValueMap superMap = super.getValueMap();

        Set<String> superKeySet = superMap.keySet();
        for (String superKey : superKeySet) {
            if (!syntheticValueMap.containsKey(superKey)) {
                syntheticValueMap.put(superKey, superMap.get(superKey));
            }
        }

        return syntheticValueMap;
    }

}
