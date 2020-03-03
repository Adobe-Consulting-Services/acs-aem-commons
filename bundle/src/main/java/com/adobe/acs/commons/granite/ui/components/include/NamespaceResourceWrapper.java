package com.adobe.acs.commons.granite.ui.components.include;

import com.adobe.granite.ui.components.ExpressionResolver;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;

import javax.annotation.Nonnull;
import java.util.Iterator;


public class NamespaceResourceWrapper extends ResourceWrapper {
  
    private final ExpressionResolver expressionResolver;
    
    private final SlingHttpServletRequest request;
    private final ValueMap valueMap;
    
    public NamespaceResourceWrapper(@Nonnull Resource resource, @Nonnull ExpressionResolver expressionResolver,
                                    @Nonnull SlingHttpServletRequest request) {
        super(resource);
        this.expressionResolver = expressionResolver;
        this.request = request;
        
        valueMap = new NamespaceDecoratedValueMapBuilder(request, resource).build();
    }
    
    @Override
    public Resource getChild(String relPath) {
        Resource child = super.getChild(relPath);
        
        if(child == null){
            return null;
        }
        
        return new NamespaceResourceWrapper(child, expressionResolver, request);
    }
    
    @Override
    public Iterator<Resource> listChildren() {
        return new TransformIterator(
                new FilterIterator(super.listChildren(),
                        o -> isVisible(new NamespaceResourceWrapper((Resource) o, expressionResolver, request))),
                        o -> new NamespaceResourceWrapper((Resource) o, expressionResolver, request)
        );
    }
    
    private boolean isVisible(Resource o) {
        return !o.getValueMap().get("hide", Boolean.FALSE);
    }
    
    
    @Override
    public boolean hasChildren() {
        if (!super.hasChildren()) {
            return false;
        }
        
        return listChildren().hasNext();
    }
    
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        
        if(type == ValueMap.class){
              return (AdapterType) getValueMap();
        }
        
        return super.adaptTo(type);
    }
    
 
    @Override
    public ValueMap getValueMap() {
        return valueMap;
    }
    

}