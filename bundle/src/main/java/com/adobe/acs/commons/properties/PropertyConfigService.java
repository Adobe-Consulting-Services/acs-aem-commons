/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.properties;

public interface PropertyConfigService {

    /**
     * Checks whether the passed property name should be excluded or not. This check is based on the OSGi
     * configuration for the service.
     *
     * @param propertyName current property name
     * @return whether to exclude or not
     */
    boolean isNotExcluded(final String propertyName);

    /**
     * Checks if the property value is of an allowed type. Currently only supports String and Long values.
     *
     * @param object current property value
     * @return whether it is allowed or not
     */
    boolean isAllowedType(Object object);
}
