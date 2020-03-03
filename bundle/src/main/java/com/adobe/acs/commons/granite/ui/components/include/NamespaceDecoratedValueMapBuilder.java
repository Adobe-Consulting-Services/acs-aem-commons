package com.adobe.acs.commons.granite.ui.components.include;

import com.adobe.acs.commons.util.TypeUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adobe.acs.commons.granite.ui.components.include.IncludeDecoratorFilterImpl.NAMESPACE;
import static com.adobe.acs.commons.granite.ui.components.include.IncludeDecoratorFilterImpl.PREFIX;
import static org.apache.commons.lang3.StringUtils.*;


public class NamespaceDecoratedValueMapBuilder {
    
    public static final String PN_NAME = "name";
    private final SlingHttpServletRequest request;

    private final Map<String,Object> copyMap;
    
    static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(\\$\\{\\{([a-zA-Z0-9]+?)(:(.+?))??\\}\\})+?");
    static final Pattern PLACEHOLDER_TYPEHINTED_PATTERN = Pattern.compile("(.*)\\$\\{\\{(\\(([a-zA-Z]+)\\)){1}([a-zA-Z0-9]+)(:(.+))?\\}\\}(.*)?");
    
    public NamespaceDecoratedValueMapBuilder(SlingHttpServletRequest request, Resource resource) {
        this.request = request;
        this.copyMap = new HashMap<>(resource.getValueMap());
    
        this.applyDynamicVariables();
        this.applyNameSpacing();
    }
    
    private void applyNameSpacing() {
        
        if(copyMap.containsKey(PN_NAME)){
            
            String originalValue = copyMap.get(PN_NAME).toString();
            final String adjusted;
          
            if(request.getAttribute(NAMESPACE) != null){
                String namespace = request.getAttribute(NAMESPACE).toString();
                final boolean containsDotSlash = contains(originalValue, "./");
                
                if(containsDotSlash){
                    String extracted = substringAfter( originalValue, "./");
                    adjusted = "./" + namespace + "/" + extracted;
                }else{
                    adjusted = namespace + "/" + originalValue;
                }
                
            }else{
                adjusted = originalValue;
            }
            
            this.copyMap.put(PN_NAME, adjusted);
            
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
        Matcher matcher = PLACEHOLDER_TYPEHINTED_PATTERN.matcher(value);
    
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
            
            if(chosenValue == null) chosenValue = "";
            
            matcher.appendReplacement(buffer, chosenValue);
            
        }
        
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }
    
    private Object castTypeHintedValue(String typeHint, String chosenValue) {
    
        Class<?> clazz = null;
    
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
            case "date":
                clazz = Date.class;
                break;
        }
        
        return TypeUtil.toObjectType(chosenValue, clazz);
    }
}
