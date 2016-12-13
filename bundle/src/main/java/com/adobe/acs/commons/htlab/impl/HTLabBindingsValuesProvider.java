/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.htlab.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;

import com.adobe.acs.commons.htlab.use.AdaptToUseFn;
import com.adobe.acs.commons.htlab.use.MapUse;
import com.adobe.acs.commons.htlab.use.ToStringUseFn;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.api.BindingsValuesProvider;

/**
 * Adds a Bindings key referencing a map of simple class names to fully-qualified class names for
 * classes under the .htlab.use package.
 */
@Component(
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE,
        label = "ACS AEM Commons - HTLab - Shortcut Bindings Value Provider",
        description = "Bind configuration to enable the binding for the HTLab use function shortcut."
)
@Service
@Property(name = "javax.script.name", value = "sightly", propertyPrivate = true)
public class HTLabBindingsValuesProvider implements BindingsValuesProvider {
    private static final String DEFAULT_BINDING_NAME = "HTLAB_USE";

    @Property(value = DEFAULT_BINDING_NAME, label = "Binding Name", description = "Name of binding for map of shortcuts")
    private static final String OSGI_BINDING_NAME = "htlab.shortcut.binding";

    private static final Class<?>[] USE_CLASS_NAMES = {
            AdaptToUseFn.class,
            MapUse.class,
            ToStringUseFn.class
    };

    private Map<String, Object> useClasses;
    private String bindingName;

    @Activate
    protected void activate(Map<String, Object> props) {
        Map<String, Object> _useClasses = new HashMap<String, Object>();
        for (Class<?> useClass : USE_CLASS_NAMES) {
            _useClasses.put(useClass.getSimpleName(), useClass.getName());
        }
        this.useClasses = Collections.unmodifiableMap(_useClasses);

        this.bindingName = PropertiesUtil.toString(props.get(OSGI_BINDING_NAME), DEFAULT_BINDING_NAME);
    }

    @Override
    public void addBindings(Bindings bindings) {
        if (!bindings.containsKey(bindingName)) {
            bindings.put(bindingName, this.useClasses);
        }
    }
}
