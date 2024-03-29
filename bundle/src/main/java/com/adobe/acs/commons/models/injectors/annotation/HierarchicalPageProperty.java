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


import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Injects a hierarchical page property.
 * Traverses upwards in the page hierarchy until the property is found.
 * Note: not supported by the javax.Inject annotation because of performance reasons. Only direct annotation is supported.
 */
@Target({METHOD, FIELD, PARAMETER})
@Retention(RUNTIME)
@InjectAnnotation
@Source(HierarchicalPageProperty.SOURCE)
public @interface HierarchicalPageProperty {


    /**
     * Start traversing upwards in the hierarchy from a specific level, skipping lower levels.
     * @since 6.0.16
     * @see https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/com/day/cq/wcm/api/Page.html#getAbsoluteParent-int-
     *   | level | returned                        |
     *  |     0 | /content                        |
     *  |     1 | /content/geometrixx             |
     *  |     2 | /content/geometrixx/en          |
     *  |     3 | /content/geometrixx/en/products |
     *  |     4 | null
     * If we'd use 1 in this example, we would skip over level 2 and 3.
     * -1 means we disable this value.
     */
    int traverseFromAbsoluteParent() default -1;

    /**
     * Source value used for this annotation.
     * @see Source
     */
    String SOURCE = "hierarchical-page-property";

    /**
     * Whether to use the current page (true) or the resource page (false).
     * @return
     */
    boolean useCurrentPage() default false;

    /**
     * Specifies the name of the value from the value map to take.
     * If empty, then the name is derived from the method or field.
     */
    String value() default StringUtils.EMPTY;

    /**
     * if set to REQUIRED injection is mandatory, if set to OPTIONAL injection is optional, in case of DEFAULT
     * the standard annotations ({@link org.apache.sling.models.annotations.Optional}, {@link org.apache.sling.models.annotations.Required}) are used.
     * If even those are not available the default injection strategy defined on the {@link org.apache.sling.models.annotations.Model} applies.
     * Default value = DEFAULT.
     */
    InjectionStrategy injectionStrategy() default InjectionStrategy.DEFAULT;

}
