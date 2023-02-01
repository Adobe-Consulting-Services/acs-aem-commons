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
package com.adobe.acs.commons.models.injectors.annotation;

import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject a child (or list of child) resources using a request as the adaptable.
 *
 * This is used to overcome limitations in the `@ChildResource` injector in that it only injects
 * a `Resource`, losing the context of the `SlingHttpServletRequest`.  For any sling models that
 * require a `SlingHttpServletRequest`, the loss of the `SlingHttpServletRequest` makes the
 * `@ChildResource` injector an insufficient solution.
 *
 * See https://issues.apache.org/jira/browse/SLING-8279 for more context on the issue that this
 * via provider solves.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@InjectAnnotation
@Source("child-resources-from-request")
public @interface ChildResourceFromRequest {
    String name() default "";

    InjectionStrategy injectionStrategy() default InjectionStrategy.DEFAULT;

    String via() default "";
}
