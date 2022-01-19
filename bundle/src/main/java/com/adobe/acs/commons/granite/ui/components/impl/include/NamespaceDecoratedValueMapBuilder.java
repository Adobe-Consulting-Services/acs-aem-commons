/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.granite.ui.components.impl.include;

import com.adobe.acs.commons.util.TypeUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adobe.acs.commons.granite.ui.components.impl.include.IncludeDecoratorFilterImpl.REQ_ATTR_NAMESPACE;
import static com.adobe.acs.commons.granite.ui.components.impl.include.IncludeDecoratorFilterImpl.PREFIX;
import static org.apache.commons.lang3.StringUtils.*;


public class NamespaceDecoratedValueMapBuilder {

    private final SlingHttpServletRequest request;

    private final Map<String,Object> copyMap;
    private final String[] namespacedProperties;

    static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(\\$\\{\\{([a-zA-Z0-9]+?)(:(.+?))??\\}\\})+?");
    static final Pattern PLACEHOLDER_TYPE_HINTED_PATTERN = Pattern.compile("(.*)\\$\\{\\{(\\(([a-zA-Z]+)\\)){1}([a-zA-Z0-9]+)(:(.+))?\\}\\}(.*)?");
    
    public NamespaceDecoratedValueMapBuilder(SlingHttpServletRequest request, Resource resource, String[] namespacedProperties) {
        this.request = request;
        this.copyMap = new HashMap<>(resource.getValueMap());
        this.namespacedProperties = Optional.ofNullable(namespacedProperties)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(new String[0]);

        this.applyDynamicVariables();
        this.applyNameSpacing();
    }

    private void applyNameSpacing() {

        if (request.getAttribute(REQ_ATTR_NAMESPACE) != null) {
            for (String namespacedProp : namespacedProperties) {
                if (copyMap.containsKey(namespacedProp)) {

                    String originalValue = copyMap.get(namespacedProp).toString();
                    final String adjusted;


                    String namespace = request.getAttribute(REQ_ATTR_NAMESPACE).toString();
                    final boolean containsDotSlash = contains(originalValue, "./");

                    if (containsDotSlash) {
                        String extracted = substringAfter(originalValue, "./");
                        adjusted = "./" + namespace + "/" + extracted;
                    } else {
                        adjusted = namespace + "/" + originalValue;
                    }

                    this.copyMap.put(namespacedProp, adjusted);

                }
            }
        }

    }
    
    public ValueMap build(){
        return new ValueMapDecorator(copyMap);
    }
    
    private void applyDynamicVariables() {
        for(Iterator<Map.Entry<String,Object>> iterator = this.copyMap.entrySet().iterator(); iterator.hasNext();){
    
            Map.Entry<String,Object> entry = iterator.next();
            
            if(entry.getValue() instanceof String) {
                final Object filtered = filter(entry.getValue().toString(), this.request);
                copyMap.put(entry.getKey(), filtered);
            }
            
        }
    }
    
    
    private Object filter(String value, SlingHttpServletRequest request) {
        Object filtered = applyTypeHintedPlaceHolders(value, request);
        
        if(filtered != null){
            return filtered;
        }
        
        return applyPlaceHolders(value, request);
    }
    
    private Object applyTypeHintedPlaceHolders(String value, SlingHttpServletRequest request) {
        Matcher matcher = PLACEHOLDER_TYPE_HINTED_PATTERN.matcher(value);
    
        if (matcher.find()) {
        
            String prefix = matcher.group(1);
            String typeHint = matcher.group(3);
            String paramKey = matcher.group(4);
            String defaultValue = matcher.group(6);
            String suffix = matcher.group(7);
    
            String requestParamValue = (request.getAttribute(PREFIX + paramKey) != null) ? request.getAttribute(PREFIX + paramKey).toString() : null;
            String chosenValue = defaultString(requestParamValue, defaultValue);
            String finalValue = defaultIfEmpty(prefix, EMPTY) + chosenValue + defaultIfEmpty(suffix, EMPTY);
        
            return isNotEmpty(typeHint) ? castTypeHintedValue(typeHint, finalValue) : finalValue;
        }
    
        return null;
    }

    private String applyPlaceHolders(String value, SlingHttpServletRequest request) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            
            String paramKey = matcher.group(2);
            String defaultValue = matcher.group(4);
            
            String requestParamValue = (request.getAttribute(PREFIX + paramKey) != null) ? request.getAttribute(PREFIX + paramKey).toString() : null;
            String chosenValue = defaultString(requestParamValue, defaultValue);
            
            if(chosenValue == null){
                chosenValue = StringUtils.EMPTY;
            }
            
            matcher.appendReplacement(buffer, chosenValue);
            
        }
        
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }
    
    private Object castTypeHintedValue(String typeHint, String chosenValue) {
    
        final Class<?> clazz;
    
        switch(typeHint.toLowerCase()){
            case "boolean":
                clazz = Boolean.class;
                break;
            case "long":
                clazz = Long.class;
                break;
            case "double":
                clazz = Double.class;
                break;
            default:
                clazz = String.class;
                break;
        }
        
        return TypeUtil.toObjectType(chosenValue, clazz);
    }
}
