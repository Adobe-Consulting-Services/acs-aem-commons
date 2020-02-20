/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.granite.ui.components;

import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.granite.ui.components.ExpressionCustomizer;

/**
 * SPI for providing a custom EL variable within a Granite UI component.
 * EL variables of all OSGi services implementing this interface are automatically available below
 * containers of type {@code /apps/acs-commons/touchui-widgets/enriched-el-container}.
 * 
 * @see {@link ExpressionCustomizer}
 * @see <a href="https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/docs/server/el.html">Granite Expression Language</a>
 */
@ConsumerType
public interface ELVariableProvider {
    
    /**
     * 
     * @return the name and value of the custom EL variable (usable in in <a href="https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/docs/server/el.html">Granite Expression Language</a>).
     * The key of the map is the variable name (should be unique), the value is the variable value (might be complex object or primitive wrapper).
     * The name must not contain {@code .}
     * Must never be {@code null}, but can be the empty map if no variable is provided.
     */
    @Nonnull Map<String, Object> getVariables(SlingHttpServletRequest request);
}
