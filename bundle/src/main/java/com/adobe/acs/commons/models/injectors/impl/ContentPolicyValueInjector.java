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
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.*;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.convertValueMapValue;

@Component(
        property = {
                Constants.SERVICE_RANKING + ":Integer=5500"
        },
        service = Injector.class
)
public class ContentPolicyValueInjector  implements Injector {

    private static final Logger LOG = LoggerFactory.getLogger(ContentPolicyValueInjector.class);

    @NotNull
    @Override
    public String getName() {
        return ContentPolicyValue.SOURCE;
    }

    @Nullable
    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element,
                           DisposalCallbackRegistry callbackRegistry) {
        ContentPolicy policy = getContentPolicy(adaptable);

        if(policy != null){
            return convertValueMapValue(policy.getProperties(), name, declaredType);
        }

        return null;
    }


}
