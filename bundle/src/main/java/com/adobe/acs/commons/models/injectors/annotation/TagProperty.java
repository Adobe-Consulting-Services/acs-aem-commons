/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.models.injectors.annotation;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 * Sling Models Injector which injects the Adobe AEM objects defined in
 * <a href="http://bit.ly/1gmlmfE">&lt;cq:defineObjects/&gt;</a>.
 * <p>
 * the following objects can be injected:
 * <ul>
 * <li> resource the current resource
 * <li> resourceResolver the current resource resolver
 *
 * <li> componentContext component context of this request
 *
 * <li> pageManager page manager
 * <li> currentPage containing page addressed by the request
 * <li> resourcePage containing page of the addressed resource
 *
 * <li> designer the designer
 * <li> currentDesign design of the addressed resource
 * <li> resourceDesign design of the addressed resource
 *
 * <li> currentStyle style addressed by the request
 * <li> session the current session
 * <li> xssApi cross site scripting provider for the current request
 * </ul>
 *
 * Note: This can only be used together with Sling Models API bundle in version 1.2.0
 */
@Target({ METHOD, FIELD, PARAMETER })
@Retention(RUNTIME)
@InjectAnnotation
@Source(TagProperty.SOURCE)
public @interface TagProperty {

    /**
     * Specifies the name of the value from the value map to take.
     * If empty, then the name is derived from the method or field.
     */
    String value() default StringUtils.EMPTY;

    /**
     * Source value used for this annotation.
     * @see Source
     */
    String SOURCE = "tag-property";

    /**
     * Specifies if it should use the hierarchy to search for the tag property.
     * If false, it will only look at the current resource.
     */
    boolean inherit() default false;
    /**
     * if set to REQUIRED injection is mandatory, if set to OPTIONAL injection is optional, in case of DEFAULT
     * the standard annotations ({@link org.apache.sling.models.annotations.Optional}, {@link org.apache.sling.models.annotations.Required}) are used.
     * If even those are not available the default injection strategy defined on the {@link org.apache.sling.models.annotations.Model} applies.
     * Default value = DEFAULT.
     */
    InjectionStrategy injectionStrategy() default InjectionStrategy.DEFAULT;

}
