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
package com.adobe.acs.commons.granite.ui.components.impl;

import java.util.Collection;
import java.util.Map.Entry;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.acs.commons.granite.ui.components.CustomELVariableInjector;
import com.adobe.acs.commons.granite.ui.components.ELVariableProvider;
import com.adobe.granite.ui.components.ExpressionCustomizer;

@Component
public class CustomELVariableInjectorImpl implements CustomELVariableInjector {

    @Reference
    Collection<ELVariableProvider> elVariableProviders;

    @Override
    public void inject(SlingHttpServletRequest request) {
        
        // enrich
        ExpressionCustomizer customizer = ExpressionCustomizer.from(request);
        
        for (ELVariableProvider provider : elVariableProviders) {
            for (Entry<String, Object> variable : provider.getVariables(request).entrySet()) {
                customizer.setVariable(variable.getKey(), variable.getValue());
            }
        }
    }

}
