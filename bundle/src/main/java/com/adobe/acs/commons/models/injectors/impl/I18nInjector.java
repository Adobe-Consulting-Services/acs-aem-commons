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

import com.adobe.acs.commons.i18n.I18nProvider;
import com.adobe.acs.commons.models.injectors.annotation.I18N;
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import com.day.cq.i18n.I18n;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import static com.adobe.acs.commons.models.injectors.annotation.I18N.SOURCE;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;

@Component(
        property = {
                Constants.SERVICE_RANKING + "=5500"
        },
        service = Injector.class
)
public class I18nInjector implements Injector {

    @Reference
    private I18nProvider i18nProvider;

    @Override
    public String getName() {
        return SOURCE;
    }

    @Override
    public Object getValue(Object adaptable, String name, Type type, AnnotatedElement annotatedElement, DisposalCallbackRegistry disposal) {

        if(!annotatedElement.isAnnotationPresent(I18N.class)){
            //skipping javax.Inject for performance reasons. Only supports direct injection.
            return null;
        }

        if (canAdaptToString(adaptable, type)) {

            String key = getI18nKey(name, annotatedElement);

            if (adaptable instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) adaptable;
                return i18nProvider.translate(key, request);
            } else {
                Resource resource = getResource(adaptable);
                return i18nProvider.translate(key, resource);
            }
        }else if(canAdaptToObject(adaptable, type)){
            if (adaptable instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) adaptable;
                return i18nProvider.i18n(request);
            } else {
                Resource resource = getResource(adaptable);
                return i18nProvider.i18n(resource);
            }
        }

        return null;
    }

    private String getI18nKey(String name, AnnotatedElement annotatedElement) {

        I18N annotation = annotatedElement.getAnnotation(I18N.class);
        String annotationKey = annotation.value();

        if (StringUtils.isNotEmpty(annotationKey)) {
            return annotationKey;
        }

        return name;
    }

    private boolean canAdaptToString(Object adaptable, Type type) {
        return getResource(adaptable) != null && ReflectionUtil.isAssignableFrom(type, String.class);
    }

    private boolean canAdaptToObject(Object adaptable, Type type) {
        return getResource(adaptable) != null && ReflectionUtil.isAssignableFrom(type, I18n.class);
    }

}

