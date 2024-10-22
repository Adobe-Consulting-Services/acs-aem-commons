/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.granite.ui.components.impl.include;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;

import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.FilteringResourceWrapper;


public class NamespaceResourceWrapper extends FilteringResourceWrapper {

    private final ExpressionResolver expressionResolver;

    private final SlingHttpServletRequest request;
    private final String[] namespacedProperties;

    private final ValueMap valueMap;

    private boolean copyToplevelProperties;

    public NamespaceResourceWrapper(@NotNull Resource resource, @NotNull ExpressionResolver expressionResolver,
                                    @NotNull SlingHttpServletRequest request,
                                    String[] namespacedProperties,
                                    boolean copyToplevelProperties) {
        super(resource, expressionResolver, request);
        this.expressionResolver = expressionResolver;
        this.request = request;
        this.namespacedProperties = Optional.ofNullable(namespacedProperties)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(new String[0]);

        this.copyToplevelProperties = copyToplevelProperties;

        valueMap = new NamespaceDecoratedValueMapBuilder(request, resource, namespacedProperties,copyToplevelProperties).build();
    }

    @Override
    public Resource getChild(String relPath) {
        Resource child = super.getChild(relPath);

        if(child == null){
            return null;
        }

        NamespaceResourceWrapper wrapped =new NamespaceResourceWrapper(child, expressionResolver, request, namespacedProperties, copyToplevelProperties);

        if(!isVisible(wrapped)){
            return null;
        }else{
            return wrapped;
        }
    }

    @Override
    public Iterator<Resource> listChildren() {
        return new TransformIterator(
                new FilterIterator(super.listChildren(),
                        o -> isVisible(new NamespaceResourceWrapper((Resource) o, expressionResolver, request, namespacedProperties, copyToplevelProperties))),
                        o -> new NamespaceResourceWrapper((Resource) o, expressionResolver, request, namespacedProperties, copyToplevelProperties)
        );
    }

    private boolean isVisible(Resource o) {
        return !o.getValueMap().get("hide", Boolean.FALSE);
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
