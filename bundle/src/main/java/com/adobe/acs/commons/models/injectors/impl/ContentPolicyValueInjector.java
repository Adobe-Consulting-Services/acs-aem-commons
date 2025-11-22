/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.ContentPolicyValue;
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.*;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.convertValueMapValue;

@Component(property = {
                Constants.SERVICE_RANKING + ":Integer=5500"
        },
        service = Injector.class)
@Designate(ocd=ContentPolicyValueInjector.Configuration.class)
public class ContentPolicyValueInjector  implements Injector {

    private static final Logger LOG = LoggerFactory.getLogger(ContentPolicyValueInjector.class);

    Configuration config;

    @Activate
    public void activate(Configuration c) {
        this.config = c;
        LOG.info("ContentPolicyValueInjector {}", config.enabled() ? "enabled": "disabled");
    }


    @NotNull
    @Override
    public String getName() {
        return ContentPolicyValue.SOURCE;
    }

    @Nullable
    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element,
            DisposalCallbackRegistry callbackRegistry) {
        if (!config.enabled()) {
            return null;
        }

        ContentPolicy policy = getContentPolicy(adaptable);
        if(policy != null){
            return convertValueMapValue(policy.getProperties(), name, declaredType);
        }
        return null;
    }


    @ObjectClassDefinition(name="ACS AEM Commons ContentPolicyValueInjector")
    public @interface Configuration {

        @AttributeDefinition(name="enabled")
        public boolean enabled() default true;

    }

}
