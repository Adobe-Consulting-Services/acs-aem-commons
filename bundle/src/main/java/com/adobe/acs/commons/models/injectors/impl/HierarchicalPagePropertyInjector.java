/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.HierarchicalPageProperty;
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResourcePage;

@Component(
        property = {
                Constants.SERVICE_RANKING + ":Integer=5502"
        },
        service = Injector.class
)
public class HierarchicalPagePropertyInjector implements Injector {

    @Override
    public String getName() {
        return HierarchicalPageProperty.SOURCE;
    }

    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element,
                           DisposalCallbackRegistry callbackRegistry) {

        if (!element.isAnnotationPresent(HierarchicalPageProperty.class)) {
            //skipping javax.Inject for performance reasons. Only supports direct injection.
            return null;
        }

        Resource currentResource = getResource(adaptable);
        if (currentResource != null) {
            Resource adaptableRes = lookUpFromPage(currentResource);
            if (adaptableRes != null) {
                InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(adaptableRes);
                return ReflectionUtil.convertValueMapValue(inheritanceValueMap, name, declaredType);
            }

        }
        return null;

    }

    private Resource lookUpFromPage(Resource currentResource) {
        Page containingPage = getResourcePage(currentResource);
        return containingPage != null ? containingPage.getContentResource() : null;
    }


}
