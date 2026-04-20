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
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.i18n.I18nProvider;
import com.adobe.acs.commons.models.injectors.annotation.I18N;
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import com.day.cq.i18n.I18n;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import static com.adobe.acs.commons.models.injectors.annotation.I18N.SOURCE;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;

@Component(
        property = {
                Constants.SERVICE_RANKING + ":Integer=5500"
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

        if (annotatedElement.isAnnotationPresent(I18N.class) && canAdaptToString(adaptable, type)) {
            //skipping javax.Inject for performance reasons. Only supports direct injection.
            return i18nStringAdaptation(adaptable, name, annotatedElement);
        }else if(canAdaptToObject(adaptable, type)){
            return i18nObjectAdaptation(adaptable, annotatedElement);
        }

        return null;
    }

    private I18n i18nObjectAdaptation(Object adaptable, AnnotatedElement annotatedElement) {
        if (adaptable instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;

            boolean forceLocaleRetrievalFromUnderlyingResource = isLocaleRetrievalFromUnderlyingResourceForced(annotatedElement);

            if(forceLocaleRetrievalFromUnderlyingResource){
                boolean localeIgnoreContent = getLocaleIgnoreContent(annotatedElement);
                return i18nProvider.i18n(request.getResource(), localeIgnoreContent);
            }else{
                return i18nProvider.i18n(request);
            }

        } else {
            boolean localeIgnoreContent = getLocaleIgnoreContent(annotatedElement);
            Resource resource = getResource(adaptable);
            return i18nProvider.i18n(resource,localeIgnoreContent);
        }
    }

    private String i18nStringAdaptation(Object adaptable, String name, AnnotatedElement annotatedElement) {
        boolean localeIgnoreContent = getLocaleIgnoreContent(annotatedElement);

        String key = getI18nKey(name, annotatedElement);

        if (adaptable instanceof SlingHttpServletRequest) {
            boolean forceLocaleRetrievalFromUnderlyingResource = isLocaleRetrievalFromUnderlyingResourceForced(annotatedElement);
            SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;

            if(forceLocaleRetrievalFromUnderlyingResource){
                return i18nProvider.translate(key, request.getResource(), localeIgnoreContent);
            }else{
                return i18nProvider.translate(key, request);
            }

        } else {
            Resource resource = getResource(adaptable);
            return i18nProvider.translate(key, resource,localeIgnoreContent);
        }
    }

    private boolean isLocaleRetrievalFromUnderlyingResourceForced(AnnotatedElement annotatedElement) {

        if(annotatedElement.isAnnotationPresent(I18N.class)){
            I18N annotation = annotatedElement.getAnnotation(I18N.class);
            return annotation.forceRetrievalFromUnderlyingResource();
        }

        return false;
    }

    private boolean getLocaleIgnoreContent(AnnotatedElement annotatedElement) {

        if(annotatedElement.isAnnotationPresent(I18N.class)){
            I18N annotation = annotatedElement.getAnnotation(I18N.class);
            return annotation.localeIgnoreContent();
        }

        return false;
    }

    private String getI18nKey(String name, AnnotatedElement annotatedElement) {

        if(annotatedElement.isAnnotationPresent(I18N.class)) {
            I18N annotation = annotatedElement.getAnnotation(I18N.class);
            String annotationKey = annotation.value();

            if (StringUtils.isNotEmpty(annotationKey)) {
                return annotationKey;
            }
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

