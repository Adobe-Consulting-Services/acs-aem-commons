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

import com.adobe.acs.commons.granite.ui.components.NamespacedTransformedResourceProvider;
import com.adobe.granite.ui.components.ExpressionResolver;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@Component(service = NamespacedTransformedResourceProvider.class)
@Designate(ocd = NamespacedTransformedResourceProviderImpl.Config.class)
public class NamespacedTransformedResourceProviderImpl implements NamespacedTransformedResourceProvider {

    @ObjectClassDefinition(
            name = "ACS AEM Commons - NamespacedTransformedResourceProvider",
            description = "Transforms a resource underlying children with a namespace. Used for granite dialog snippets to be included in a granular way.")
    public @interface Config {
        @AttributeDefinition(
                name = "Properties",
                description = "Properties that should be namespaced"
        )
        String[] properties() default {"name", "fileNameParameter", "fileReferenceParameter"};

        @AttributeDefinition(
                name = "Copy top level properties",
                description = "Copy the top level properties of the snippets to the include node"
        )
        boolean copyToplevelProperties() default true;
    }

    @Reference
    private ExpressionResolver expressionResolver;

    private String[] namespacedProperties;
    private boolean copyToplevelProperties;

    @Activate
    @Modified
    public void init(Config config) {
        this.namespacedProperties = config.properties();
        this.copyToplevelProperties = config.copyToplevelProperties();
    }

    @Override
    public Resource transformResourceWithNameSpacing(SlingHttpServletRequest request, Resource targetResource) {
        return new NamespaceResourceWrapper(
                targetResource,
                expressionResolver,
                request,
                namespacedProperties,
                copyToplevelProperties
        );
    }

}
